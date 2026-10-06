package com.steinbergfour.max.triggers;

import com.steinbergfour.max.data.Production;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Objects;

public class KeyTriggers implements KeyListener {
    private Production p;
    // todo: implement hold hotkeys

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
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        for (var cueID : p.getCueList().keySet()) {
            var cue = p.getCueList().get(cueID);
            if (Objects.equals(cue.hotkey(), keyEventToHotkey(e))) {
                p.trigger(cue);
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }
}
