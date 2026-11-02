package com.steinbergfour.max.managers;

import com.steinbergfour.max.actions.Responder;
import com.steinbergfour.max.data.Cue;

import javax.sound.sampled.*;
import java.util.*;

public class SoundManager {
    private final Map<String, List<Pair<Clip, Long>>> clips;

    public SoundManager() {
        this.clips = new HashMap<>();
    }

    public boolean fire(String owningCue, AudioInputStream ais, Responder completed, boolean loop) {
        return this.fire(owningCue, ais, completed, loop, 0);
    }

    public boolean fire(String owningCue, AudioInputStream ais, Responder completed, boolean loop, double percent) {
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

//        System.out.println(clip.getMicrosecondLength());
        clip.setMicrosecondPosition((long) (percent * clip.getMicrosecondLength()));
        clip.start();
//        clip.setMicrosecondPosition(position * 1000); // idk what the correct order is
        System.out.println(clip.getMicrosecondPosition());
        if (loop) clip.loop(Clip.LOOP_CONTINUOUSLY);

        clip.addLineListener(event -> {
            System.out.println(event);
            if (event.getType() == LineEvent.Type.STOP) {
                if (clip.isActive()) clip.close();
                CueManager.singleton._announceFinished(owningCue);
                this.clips.get(owningCue).remove(clip);
                completed.respond();
            }
        });

        if (!this.clips.containsKey(owningCue)) {
            this.clips.put(owningCue, new ArrayList<>());
        }
        this.clips.get(owningCue).add(new Pair<>(clip, 0L));

        return true;
    }

    public void kill() {
        for (String owningCue : this.clips.keySet()) {
            this.kill(owningCue);
        }
    }

    public void kill(String cue) {
        for (var clip : this.clips.get(cue)) {
            clip.first().stop();
            clip.first().close();
        }
        this.clips.put(cue, new ArrayList<>());
    }

    public long pause(String cue) {
        // only return a long if we are in a blocking/monolithic cue
//        System.out.println(this.clips.get(cue));
        if (this.clips.get(cue).size() == 1) {
            var c = this.clips.get(cue).getFirst();
            long pos = c.first().getMicrosecondPosition();
            c.first().stop();
            c.first().close();
            this.clips.get(cue).remove(c);
            return pos;
        } else {
            this.kill(cue);
            return 0L;
        }
    }

    public void resumeMicroseconds(String cue, long pos) {
        // assumes its already been triggered
        // i dont think this works
        for (var clip : this.clips.get(cue)) {
            clip.first().setMicrosecondPosition(pos);
        }
    }

    public void resume(String cue, double pos) {
        this.resumeMicroseconds(cue, (long) (pos * CueManager.singleton.blockingCueDuration()));
        // assumes its already been triggered
//        for (var clip : this.clips.get(cue)) {
//            clip.first().setMicrosecondPosition((long) (pos * CueManager.singleton.blockingCueDuration()));
//            System.out.println(pos);
//            System.out.println(clip.first().getMicrosecondPosition());
//        }
    }
}
