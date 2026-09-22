package com.stremio.morphe;

import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.common.audio.ChannelMixingAudioProcessor;
import androidx.media3.common.audio.ChannelMixingMatrix;

import java.util.List;

public final class AudioDownmixBridge {
    private static final float SURROUND_MIX = 0.707107f;
    private static final int DEFAULT_CENTER_BOOST_DB = 20;

    private AudioDownmixBridge() {
    }

    public static ChannelMixingAudioProcessor createDownmixProcessor() {
        return createDownmixProcessor(DEFAULT_CENTER_BOOST_DB);
    }

    public static ChannelMixingAudioProcessor createDownmixProcessor(int centerBoostDb) {
        ChannelMixingAudioProcessor processor = new ChannelMixingAudioProcessor();
        float centerMix = SURROUND_MIX * dbToLinear(centerBoostDb);

        processor.putChannelMixingMatrix(buildMatrix(6, centerMix, SURROUND_MIX));
        processor.putChannelMixingMatrix(buildMatrix(8, centerMix, SURROUND_MIX));

        return processor;
    }

    public static void appendVlcDownmixOptions(List<String> options) {
        if (options == null) {
            return;
        }

        if (!options.contains("--stereo-mode=1")) {
            options.add("--stereo-mode=1");
        }
    }

    private static ChannelMixingMatrix buildMatrix(
            int inputChannelCount,
            float centerMix,
            float surroundMix
    ) {
        float[] coefficients = new float[inputChannelCount * 2];

        for (int input = 0; input < inputChannelCount; input++) {
            int base = input * 2;
            coefficients[base] = 0f;
            coefficients[base + 1] = 0f;

            switch (input) {
                case 0:
                    coefficients[base] = 1f;
                    break;
                case 1:
                    coefficients[base + 1] = 1f;
                    break;
                case 2:
                    coefficients[base] = centerMix;
                    coefficients[base + 1] = centerMix;
                    break;
                case 4:
                    coefficients[base] = surroundMix;
                    break;
                case 5:
                    coefficients[base + 1] = surroundMix;
                    break;
                case 6:
                    if (inputChannelCount >= 8) {
                        coefficients[base] = surroundMix;
                    }
                    break;
                case 7:
                    if (inputChannelCount >= 8) {
                        coefficients[base + 1] = surroundMix;
                    }
                    break;
                default:
                    break;
            }
        }

        return new ChannelMixingMatrix(inputChannelCount, 2, coefficients);
    }

    private static float dbToLinear(int db) {
        return (float) Math.pow(10d, db / 20d);
    }
}
