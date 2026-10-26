package com.steinbergfour.max.processing;

import com.steinbergfour.max.data.Processing;

import java.util.HashMap;

public abstract class Processor {
    public static enum ProcessorType {
        FADER, PEQ, STATIC
    }

    public abstract void processInPlace(byte[] input);

    public static final HashMap<ProcessorType, Class<? extends Processor>> PROCESSOR_MAP = new HashMap<>();

    static {
        PROCESSOR_MAP.put(ProcessorType.FADER, Fader.class);
        PROCESSOR_MAP.put(ProcessorType.PEQ, ParametricEQ.class);
    }

    public Processor(Processing data) {

    }

    public static Processor generate(Processing processing) {
        var type = processing.type().toUpperCase();
        ProcessorType pr = ProcessorType.valueOf(type);

        try {
            return PROCESSOR_MAP.get(pr).getConstructor(Processing.class).newInstance(processing);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
