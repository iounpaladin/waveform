package com.steinbergfour.max.processing;

import com.steinbergfour.max.data.Processing;
import com.steinbergfour.max.managers.CueManager;

public class Fader extends Processor {
    private final Key startKey;
    private final Key endKey;
    private final float startValue;
    private final float endValue;
    private final Keyframe keyframe;
    private final FadeType fadeType;

    public static enum Keyframe {
        VOLUME;
    }

    private static enum FadeType {
        CUBIC, LINEAR;
    }

    public static record Key(int position, boolean fromEnd) {
        public int toIndex(int length) {
            return this.fromEnd ? length - position - 1 : position;
        }
    }

    public Fader(Processing data) {
        super(data);

        this.startKey = parseKeyframe(data.parameters().get("startKey"));
        this.endKey = parseKeyframe(data.parameters().get("endKey"));
        this.startValue = Float.parseFloat(data.parameters().get("startValue"));
        this.endValue = Float.parseFloat(data.parameters().get("endValue"));
        this.keyframe = Keyframe.valueOf(data.parameters().get("keyframe").toUpperCase());
        this.fadeType = FadeType.valueOf(data.parameters().get("fadeType").toUpperCase());
    }

    private static Key parseKeyframe(String inp) {
        int number;
        boolean fromEnd;
        if (inp.startsWith("-")) {
            number = Integer.parseInt(inp.substring(1));
            fromEnd = true;
        } else {
            number = Integer.parseInt(inp);
            fromEnd = false;
        }

        float numberInSeconds = ((float) number) / 1000.0f;
        float frames = CueManager.targetFormat.getSampleRate() * numberInSeconds * CueManager.targetFormat.getFrameSize();
        return new Key(Math.round(frames), fromEnd);
    }

    @Override
    public void processInPlace(byte[] input) {
        int startingPosition = this.startKey.toIndex(input.length);
        int endingPosition = this.endKey.toIndex(input.length);
        for (int idx = startingPosition; idx < endingPosition; idx += 4) { // stereo, 2 bytes per frame
            // little endian
            short left = (short) ((input[idx] & 0xFF) | (input[idx + 1] & 0xFF << 8));
            short right = (short) ((input[idx + 2] & 0xFF) | (input[idx + 3] & 0xFF << 8));

            float cursor = (float) (idx - startingPosition) / (endingPosition - startingPosition);
            float currentValue = this.interpolate(this.startValue, this.endValue, cursor);

            // switch on this.keyframe
            short newLeft = (short) (left * currentValue);
            short newRight = (short) (right * currentValue);

            input[idx] = (byte) (newLeft & 0xFF);
            input[idx + 1] = (byte) ((newLeft >> 8) & 0xFF);
            input[idx + 2] = (byte) (newRight & 0xFF);
            input[idx + 3] = (byte) ((newRight >> 8) & 0xFF);
            // this is bullshit
            // need to pack into 16 bit, signed, little endian samples
        }
    }

    public float interpolate(float start, float end, float cursor) {
        // different interpolation function
        return cursor * end + (1 - cursor) * start;
    }
}
