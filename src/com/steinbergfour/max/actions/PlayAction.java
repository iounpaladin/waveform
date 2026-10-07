package com.steinbergfour.max.actions;

import com.steinbergfour.max.managers.CueManager;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;

public class PlayAction implements Actionable {
    private final String target;
    private final String preset;

    public PlayAction(String target, String preset) {
        this.target = target;
        this.preset = preset;
    }

    @Override
    public void activate(Responder responder) {
        System.out.println(this.target);
        AudioInputStream ais;
        try {
            ais = AudioSystem.getAudioInputStream(new File("C:\\Users\\maxss\\OneDrive\\Documents\\Audacity\\11-@loop.wav"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        CueManager.singleton.getSoundManager().play(ais, responder);
    }
}
