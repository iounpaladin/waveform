package com.steinbergfour.max.sound;

import com.steinbergfour.max.data.Cue;

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

    public void play() throws LineUnavailableException, UnsupportedAudioFileException, IOException {
        Clip clip = AudioSystem.getClip();
        // getAudioInputStream() also accepts a File or InputStream
        AudioInputStream ais = AudioSystem.getAudioInputStream(new File("C:\\Users\\maxss\\OneDrive\\Documents\\Audacity\\11-@loop.wav"));
        clip.open(ais);
        clip.loop(Clip.LOOP_CONTINUOUSLY);

        this.clips.add(clip);
    }

    public void kill() {
        for(var clip : this.clips) {
            clip.stop();
        }
    }
}
