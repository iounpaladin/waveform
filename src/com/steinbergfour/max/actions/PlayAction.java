package com.steinbergfour.max.actions;

import com.steinbergfour.max.managers.CueManager;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class PlayAction implements Actionable {
    private final String target;
    private final String preset;
//    private static byte[] data;
//    private static AudioFormat fmt;
//    private static long length;

    public PlayAction(String target, String preset) {
        this.target = target;
        this.preset = preset;
    }

//    private static void evilBS() {
//        try {
//            AudioInputStream ais = AudioSystem.getAudioInputStream(new File("C:\\Users\\maxss\\OneDrive\\Documents\\Audacity\\11-@loop.wav"));
//            data = ais.readAllBytes();
//            fmt = ais.getFormat();
//            length = ais.getFrameLength();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    private AudioInputStream getAudioInputStream() {
//        return new AudioInputStream(new ByteArrayInputStream(data), fmt, length);
//    }
//
//    static {
//        evilBS();
//    }

    @Override
    public void activate(Responder responder) {
        System.out.println(this.target);
//        AudioInputStream ais;
//        try(var str = Files.newDirectoryStream(Paths.get("C:\\Users\\maxss\\OneDrive\\Documents\\Audacity"), this.target + "-*.wav");) {
//            var path = str.iterator().next();
//            ais = AudioSystem.getAudioInputStream(path.toFile());
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
        try {
            CueManager.CachedData data = CueManager.singleton.getCache(this.target);
            AudioInputStream ais = new AudioInputStream(
                    new ByteArrayInputStream(data.data()), data.fmt(), data.length()
            );
            CueManager.singleton.getSoundManager().play(ais, responder);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
