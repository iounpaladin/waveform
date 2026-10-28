package com.steinbergfour.max.actions;

import com.steinbergfour.max.managers.CueManager;

import javax.sound.sampled.AudioInputStream;
import java.io.*;

public class PlayAction implements Actionable {
    private final String cue;
    private final String target;
    private final boolean loop;

    public PlayAction(String target, String cue, boolean loop) {
        this.target = target;
        this.cue = cue;
        this.loop = loop;
    }

    @Override
    public void activate(Responder responder) {
        System.out.println(this.target);

        try {
            CueManager.CachedData data = CueManager.singleton.getCache(this.target);
            AudioInputStream ais = new AudioInputStream(
                    new ByteArrayInputStream(data.data()), data.fmt(), data.length()
            );
            CueManager.singleton.getSoundManager().fire(this.cue, ais, responder, this.loop);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
