package com.steinbergfour.max.managers;

import com.steinbergfour.max.actions.Responder;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SoundManager {
    private List<Clip> clips;

    public SoundManager() {
        this.clips = new ArrayList<Clip>();
    }

    public void play(AudioInputStream ais, Responder completed)  {
        final Clip clip;
        try {
            clip = AudioSystem.getClip();
        } catch (LineUnavailableException e) {
            return;
            // ignored
        }
        // getAudioInputStream() also accepts a File or InputStream
        try {
            clip.open(ais);
        } catch (Exception e) {
            // ignored
        }

        clip.start();
//        clip.loop(Clip.LOOP_CONTINUOUSLY);
        clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    completed.respond();
                    clip.close();
                    this.clips.remove(clip);
                }
        });

        this.clips.add(clip);
    }

    public void kill() {
        for(var clip : this.clips) {
            clip.close();
        }
    }
}
