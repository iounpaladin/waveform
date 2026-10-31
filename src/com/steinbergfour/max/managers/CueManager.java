package com.steinbergfour.max.managers;

import com.steinbergfour.max.actions.ActionSequencer;
import com.steinbergfour.max.actions.SimultaneousActionFirer;
import com.steinbergfour.max.data.*;
import com.steinbergfour.max.processing.Processor;
import org.jetbrains.annotations.NotNull;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;

/**
 * Central class with resource management, action management, etc
 */
public class CueManager {
    private final SoundManager mgr;
    public static CueManager singleton;
    private final CueList cueList;
    private final ResourceList resourceList;
    private final ProcessingList processingList;
    private final CompositionList compositionList;
    private final HashMap<String, CachedData> cache;
    private final HashMap<String, Processor> processors;
    public static final AudioFormat targetFormat;
    private final List<Consumer<Cue>> cueActivatedListeners;
    private final List<Consumer<Cue>> cueFinishedListeners;

    static {
        targetFormat = new AudioFormat(
                44100f,
                16,
                2,
                true,
                false
        );
    }

    // should never be called by anyone but SoundManager
    public void _announceFinished(String owningCue) {
        // todo: disambiguate between action cues and play cues
        Cue owningCueObj = null;

        for (var cue : this.cueList.values()) {
            if (cue.name().equals(owningCue)) {
                owningCueObj = cue;
                break;
            }
        }

        if (owningCueObj == null) return;

        this.triggerFinishedListeners(owningCueObj);
    }

    public boolean cacheInitialised() {
        return this.cache != null && !this.cache.isEmpty();
    }

    public static record CachedData(byte[] data, AudioFormat fmt, long length) {

    }

    public CueManager(CueList cueList, ResourceList resourceList, ProcessingList processingList, CompositionList compositionList) {
        this(cueList, resourceList, processingList, compositionList, true);
    }

    public CueManager(CueList cueList, ResourceList resourceList, ProcessingList processingList, CompositionList compositionList, boolean preload) {
        this.mgr = new SoundManager();
        this.cueList = cueList;
        this.resourceList = resourceList;
        this.processingList = processingList;
        this.compositionList = compositionList;
        if (singleton != null) {
            throw new RuntimeException("singleton");
        }
        singleton = this;
        this.cache = new HashMap<>();
        this.processors = new HashMap<>();
        this.cueActivatedListeners = new LinkedList<>();
        this.cueFinishedListeners = new LinkedList<>();
        if (preload) this.preloadResources();
    }

    public static enum ListenerType {
        ACTIVATED, FINISHED, BOTH
    }

    public void registerListener(Consumer<Cue> listener, ListenerType type) {
        switch (type) {
            case ACTIVATED:
                this.cueActivatedListeners.add(listener);
                break;
            case FINISHED:
                this.cueFinishedListeners.add(listener);
                break;
            case BOTH:
                this.cueActivatedListeners.add(listener);
                this.cueFinishedListeners.add(listener);
                break;
        }
}
    public void preloadResources() {
        // resources !!!
        System.out.println("== Resource Initialisation ==");
        for (var resourceName : resourceList.keySet()) {
            var resource = resourceList.get(resourceName);
            try {
                AudioInputStream ais = AudioSystem.getAudioInputStream(
//                        targetFormat,
                        Paths.get(resource.href()).toAbsolutePath().toFile()
                );
                AudioInputStream aisConverted = AudioSystem.getAudioInputStream(targetFormat, ais);
                byte[] data = aisConverted.readAllBytes();
                cache.put(
                        resourceName,
                        new CachedData(data, aisConverted.getFormat(), aisConverted.getFrameLength())
                );
                System.out.printf("Cached %s=%s\n", resourceName, resource.href());
            } catch (Exception e) {
                // ignored
                e.printStackTrace();
            }
        }

        System.out.println("== Processor Initialisation ==");
        for (var processorName : processingList.keySet()) {
            this.processors.put(processorName, Processor.generate(processingList.get(processorName)));
            System.out.printf("Built processor %s=%s\n", processorName, processingList.get(processorName).type());
        }

        System.out.println("== Composition Initialisation ==");

        for (var compositionID : this.compositionList.keySet()) {
            var composition = this.compositionList.get(compositionID);

            var result = this.compose(composition);

            cache.put(
                    compositionID,
                    new CachedData(result, targetFormat, result.length)
            );
            System.out.printf("Composed %s\n", compositionID);
        }

        System.out.println("\n\n\n");

        // todo: process loops
    }

    private byte @NotNull [] compose(Composition composition) {
        var samples = composition.sampleList();

        byte[] current = new byte[0];

        for (var sample : samples) {
            current = join(
                    current,
                    this.applyFilters(
                            this.cache.get(sample.target()).data, sample.filters()
                    )
            );
        }

        return current;
    }

    private byte[] applyFilters(byte[] data, String filters) {
        byte[] current = Arrays.copyOf(data, data.length);
        String[] filterList = filters.split(",\\s*");

        for(String filter : filterList) {
            var processingData = this.processors.get(filter);
            processingData.processInPlace(current);
        }

        return current;
    }

    public static byte[] join(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    private void triggerActivateListeners(Cue c) {
        for (var listener : this.cueActivatedListeners) {
            listener.accept(c);
            System.out.println(listener);
        }
    }

    private void triggerFinishedListeners(Cue c) {
        for (var listener : this.cueFinishedListeners) {
            listener.accept(c);
            System.out.println(listener);
        }
    }

    public void trigger(@NotNull Cue c) {
        this.triggerActivateListeners(c);
        System.out.printf("Trigger %s\n", c.name());
        switch (c.type()) {
            case SEQUENCED:
                this.sequence(c.name(), c.actions());
                break;
            case SIMULTANEOUS:
                this.simultaneous(c.name(), c.actions());
                break;
        }
    }

    private void simultaneous(String owningCue, ActionList actions) {
        SimultaneousActionFirer firer = new SimultaneousActionFirer(actions);
//        firer.fireAndThen(
//                () -> {this._announceFinished(owningCue);}
//        );
        firer.fireAndThen(() -> {});
    }

    private void sequence(String owningCue, ActionList actionList) {
        ActionSequencer seq = new ActionSequencer(actionList);
//        seq.runAsync(() -> {this._announceFinished(owningCue);});
        seq.runAsync(() -> {});
    }

    public SoundManager getSoundManager() {
        return mgr;
    }

    public CachedData getCache(String key) {
        return this.cache.get(key);
    }
}
