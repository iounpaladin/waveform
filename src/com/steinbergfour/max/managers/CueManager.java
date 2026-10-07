package com.steinbergfour.max.managers;

import com.steinbergfour.max.actions.ActionSequencer;
import com.steinbergfour.max.actions.Actionable;
import com.steinbergfour.max.actions.Responder;
import com.steinbergfour.max.data.*;
import org.jetbrains.annotations.NotNull;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.awt.*;
import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

/**
 * Central class with resource management, action management, etc
 */
public class CueManager {
    private final SoundManager mgr;
    public static CueManager singleton;
    private final CueList cl;
    private final ResourceList rl;
    private final PostprocessingList pl;
    private final HashMap<String, CachedData> cache;

    public static record CachedData(byte[] data, AudioFormat fmt, long length) {

    }

    public CueManager(CueList cl, ResourceList rl, PostprocessingList pl) {
        this(cl, rl, pl, true);
    }

    public CueManager(CueList cl, ResourceList rl, PostprocessingList ppl, boolean preload) {
        this.mgr = new SoundManager();
        this.cl = cl;
        this.rl = rl;
        this.pl = ppl;
        if (singleton != null) {
            throw new RuntimeException("singleton");
        }
        singleton = this;
        this.cache = new HashMap<>();
        if (preload) this.preloadResources();
    }

    public void preloadResources() {
        // resources !!!
        for (var resourceName : rl.keySet()) {
            var resource = rl.get(resourceName);
            try {
                AudioInputStream ais = AudioSystem.getAudioInputStream(
                        Paths.get(resource.href()).toAbsolutePath().toFile()
                );
                byte[] data = ais.readAllBytes();
                cache.put(
                        resourceName,
                        new CachedData(data, ais.getFormat(), ais.getFrameLength())
                );
                System.out.printf("%s=%s successfully initialised\n", resourceName, resource.href());
            } catch (Exception e) {
                // ignored
                e.printStackTrace();
            }
        }
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
