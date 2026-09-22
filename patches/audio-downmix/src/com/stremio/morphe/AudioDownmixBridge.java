package com.stremio.morphe;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.common.audio.ChannelMixingAudioProcessor;
import androidx.media3.common.audio.ChannelMixingMatrix;

import java.util.List;

public final class AudioDownmixBridge {
    private static final String PREFS = "morphe_downmix";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_CENTER_BOOST_DB = "center_boost_db";

    private static final float SURROUND_MIX = 0.707107f;
    private static final int DEFAULT_CENTER_BOOST_DB = 20;

    private static Context appContext;

    private AudioDownmixBridge() {
    }

    /** Records the application context so runtime preferences can be read. */
    public static void init(Context context) {
        if (context != null) {
            appContext = context.getApplicationContext();
        }
    }

    public static boolean isEnabled() {
        return appContext == null || prefs().getBoolean(KEY_ENABLED, true);
    }

    public static void setEnabled(boolean enabled) {
        if (appContext == null) return;
        prefs().edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public static int getCenterBoostDb() {
        return appContext == null
                ? DEFAULT_CENTER_BOOST_DB
                : prefs().getInt(KEY_CENTER_BOOST_DB, DEFAULT_CENTER_BOOST_DB);
    }

    public static void setCenterBoostDb(int centerBoostDb) {
        if (appContext == null) return;
        prefs().edit().putInt(KEY_CENTER_BOOST_DB, centerBoostDb).apply();
    }

    private static SharedPreferences prefs() {
        return appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static ChannelMixingAudioProcessor createDownmixProcessor() {
        return createDownmixProcessor(getCenterBoostDb());
    }

    public static ChannelMixingAudioProcessor createDownmixProcessor(int centerBoostDb) {
        ChannelMixingAudioProcessor processor = new ChannelMixingAudioProcessor();
        float centerMix = SURROUND_MIX * dbToLinear(centerBoostDb);

        processor.putChannelMixingMatrix(buildMatrix(6, centerMix, SURROUND_MIX));
        processor.putChannelMixingMatrix(buildMatrix(8, centerMix, SURROUND_MIX));

        return processor;
    }

    public static void appendVlcDownmixOptions(List<String> options) {
        if (options == null || !isEnabled()) {
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
