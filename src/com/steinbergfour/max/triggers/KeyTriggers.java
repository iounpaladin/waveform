package com.steinbergfour.max.triggers;

import com.steinbergfour.max.data.Production;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.Synthesizer;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Objects;

public class KeyTriggers implements KeyListener {
    private Production p;
    private static final int channel = 9;
    private static final int volume = 80;
    private Synthesizer synth;
    private boolean midi;
    // todo: implement hold hotkeys (and/or double actions)

    public static String keyEventToHotkey(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            return "ENTER";
        } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            return "ESC";
        }

        return String.valueOf(e.getKeyChar());
    }

    public KeyTriggers(Production p) {
        this.p = p;
//        try {
//            this.synth = MidiSystem.getSynthesizer();
//            this.synth.open();
//            this.midi = true;
//        } catch (Exception e) {
//
//        }
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    private static int keyToMidi(KeyEvent e) {
        return switch (e.getKeyChar()) {
            case 'q' -> 35;
            case 'w' -> 38;
            case 'e' -> 42;
            case 'r' -> 49;
            default -> -1;
        };
    }

    @Override
    public void keyPressed(KeyEvent e) {
        for (var cueID : p.getCueList().keySet()) {
            var cue = p.getCueList().get(cueID);
            if (Objects.equals(cue.hotkey(), keyEventToHotkey(e))) {
                p.trigger(cue);
            }
        }

        if (this.midi && keyToMidi(e) != -1) {
            synth.getChannels()[channel].noteOn(keyToMidi(e), volume);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (this.midi && keyToMidi(e) != -1) {
            synth.getChannels()[channel].noteOff(keyToMidi(e), volume);
        }
    }
}
