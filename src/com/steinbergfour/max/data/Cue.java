package com.steinbergfour.max.data;

public record Cue(String hotkey, String name, String reminder, ActionList actions, Reduction type) {
    public static enum Reduction {
        SIMULTANEOUS, SEQUENCED
    }
}
