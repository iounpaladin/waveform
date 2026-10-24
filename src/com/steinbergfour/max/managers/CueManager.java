package com.steinbergfour.max.managers;

import com.steinbergfour.max.actions.ActionSequencer;
import com.steinbergfour.max.data.*;
import org.jetbrains.annotations.NotNull;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

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
    public static final AudioFormat targetFormat;

    static {
        targetFormat = new AudioFormat(
                44100f,
                16,
                2,
                true,
                false
        );
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
        if (preload) this.preloadResources();
    }

    public void preloadResources() {
        // resources !!!

        // todo: create compositions and apply filters
        // also need to rename/restructure some stuff in the data
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
                System.out.printf("%s=%s successfully initialised\n", resourceName, resource.href());
            } catch (Exception e) {
                // ignored
                e.printStackTrace();
            }
        }

        System.out.println("== Composition Initialisation ==");

        System.out.println(this.compositionList.keySet());
    }

    public void trigger(@NotNull Cue c) {
        System.out.printf("Trigger %s\n", c.name());
        switch (c.type()) {
            case SEQUENCED:
                this.sequence(c.actions());
                break;
            case SIMULTANEOUS:
                for (var action : c.actions()) {
                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> action.activate(null));
                }
                break;
        }
    }

    private void sequence(ActionList actionList) {
        ActionSequencer seq = new ActionSequencer(actionList);
        seq.runAsync();
    }

    public SoundManager getSoundManager() {
        return mgr;
    }

    public CachedData getCache(String key) {
        return this.cache.get(key);
    }
}
