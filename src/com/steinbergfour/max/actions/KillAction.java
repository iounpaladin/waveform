package com.steinbergfour.max.actions;

import com.steinbergfour.max.data.Production;
import com.steinbergfour.max.managers.CueManager;
import com.steinbergfour.max.managers.SoundManager;

public class KillAction implements Actionable {
    private final String target;

    public KillAction(String target) {
        this.target = target;
    }

    @Override
    public void activate(Responder responder) {
        System.out.println("Killing " + target);
        if (target.equals("@e")) {
            CueManager.singleton.getSoundManager().kill();
        } else {
            CueManager.singleton.getSoundManager().kill(target);
        }
        if(responder != null) responder.respond();
    }
}
