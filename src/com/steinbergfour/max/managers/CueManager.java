package com.steinbergfour.max.managers;

import com.steinbergfour.max.actions.ActionSequencer;
import com.steinbergfour.max.actions.Actionable;
import com.steinbergfour.max.actions.Responder;
import com.steinbergfour.max.data.*;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.concurrent.CompletableFuture;

/**
 * Central class with resource management, action management, etc
 */
public class CueManager {
    private SoundManager mgr;
    public static CueManager singleton;

    public CueManager(CueList cl, ResourceList rl, PostprocessingList ppl) {
        this.mgr = new SoundManager();
        if (singleton != null) {
            throw new RuntimeException("singleton");
        }
        singleton = this;
    }

    public void preloadResources() {

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
}
