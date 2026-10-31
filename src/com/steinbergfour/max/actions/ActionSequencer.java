package com.steinbergfour.max.actions;

import com.steinbergfour.max.data.ActionList;
import com.steinbergfour.max.managers.SoundManager;

import java.awt.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ActionSequencer {
    private final ActionList al;
    private int index;
    private Runnable action;

    // action list must not change or sequencer will explode
    public ActionSequencer(ActionList actionList) {
        this.al = actionList;
        this.index = 0;
    }

    // can only be run once, will explode otherwise
    public void runAsync(Runnable action) {
        assert this.index == 0;
        this.action = action;
        CompletableFuture<Void> future = CompletableFuture.runAsync(this::next);
    }

    private void next() {
        if (index >= al.size()) {
            action.run();
            return;
        }
        var action = al.get(index);
        index++;
        action.activate(this::next);
    }
}
