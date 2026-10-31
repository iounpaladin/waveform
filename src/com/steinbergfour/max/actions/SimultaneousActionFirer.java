package com.steinbergfour.max.actions;

import com.steinbergfour.max.data.ActionList;

import java.util.Collection;
import java.util.Collections;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;

public class SimultaneousActionFirer {
    private final ActionList actionList;
    private final Vector<Boolean> completed;
    private Runnable r;

    public SimultaneousActionFirer(ActionList actionList) {
        this.actionList = actionList;
        completed = new Vector<>(Collections.nCopies(actionList.size(), false));
    }

    public void fireAndThen(Runnable r) {
        this.r = r;
        int i = 0;
        for (var action : this.actionList) {
            int finalI = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> action.activate(null)).thenRun(() -> {
//                if (action.getClass().equals(PlayAction.class))
//                    return;
                this.completed.set(finalI, true);
                check();
            });
            i++;
        }
    }

    private void check() {
        for (boolean b : this.completed) {
            if (!b) return;
        }

        r.run();
    }
}
