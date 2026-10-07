package com.steinbergfour.max.actions;

import com.steinbergfour.max.data.ActionList;
import com.steinbergfour.max.managers.SoundManager;

import java.awt.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ActionSequencer {
    private final ActionList al;
    private int index;

    // action list must not change or sequencer will explode
    public ActionSequencer(ActionList actionList) {
        this.al = actionList;
        this.index = 0;
    }

    // can only be run once, will explode otherwise
    public void runAsync() {
        assert this.index == 0;
        CompletableFuture<Void> future = CompletableFuture.runAsync(this::next);
    }

    private void next() {
        System.out.println(System.currentTimeMillis());
        if (index >= al.size()) return;
        var action = al.get(index);
        index++;
        action.activate(this::next);
    }
}
