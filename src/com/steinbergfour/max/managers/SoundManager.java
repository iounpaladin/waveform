package com.steinbergfour.max.managers;

import com.steinbergfour.max.actions.Responder;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SoundManager {
    private final Map<String, List<Clip>> clips;

    public SoundManager() {
        this.clips = new HashMap<>();
    }

    public boolean fire(String owningCue, AudioInputStream ais, Responder completed, boolean loop)  {
        final Clip clip;
        try {
            clip = AudioSystem.getClip();
        } catch (LineUnavailableException e) {
            return false;
            // ignored
        }
        // getAudioInputStream() also accepts a File or InputStream
        try {
            clip.open(ais);
        } catch (Exception e) {
            // ignored
        }

        clip.start();
        if(loop) clip.loop(Clip.LOOP_CONTINUOUSLY);

        clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    completed.respond();
                    clip.close();
                    this.clips.get(owningCue).remove(clip);
                }
        });

        if(!this.clips.containsKey(owningCue)) {
            this.clips.put(owningCue, new ArrayList<>());
        }
        this.clips.get(owningCue).add(clip);
        return true;
    }

    public void kill() {
        for (String owningCue : this.clips.keySet()) {
            this.kill(owningCue);
        }
    }

    public void kill(String cue) {
        for(var clip : this.clips.get(cue)) {
            clip.close();
        }
    }
}
