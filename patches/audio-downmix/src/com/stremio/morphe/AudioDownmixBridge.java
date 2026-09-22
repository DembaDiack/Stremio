package com.stremio.morphe;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.common.audio.ChannelMixingAudioProcessor;
import androidx.media3.common.audio.ChannelMixingMatrix;
import androidx.media3.exoplayer.audio.AudioSink;
import androidx.media3.exoplayer.audio.DefaultAudioSink;

import java.util.List;

public final class AudioDownmixBridge {
    private static final String PREFS = "morphe_downmix";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_CENTER_BOOST_DB = "center_boost_db";

    private static final float SURROUND_MIX = 0.707107f;
    private static final int DEFAULT_CENTER_BOOST_DB = 6;

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

        // Mono -> stereo (constant gain). Without a matrix the processor throws
        // UnhandledAudioFormatException for single-channel sources.
        processor.putChannelMixingMatrix(new ChannelMixingMatrix(1, 2, new float[] { 1f, 1f }));
        // Stereo -> identity. onConfigure returns NOT_SET for an identity matrix,
        // so the processor is bypassed and already-stereo content plays untouched.
        processor.putChannelMixingMatrix(identityMatrix(2));
        // Multichannel -> stereo downmix with a boosted center channel.
        for (int channels = 3; channels <= 8; channels++) {
            processor.putChannelMixingMatrix(buildMatrix(channels, centerMix, SURROUND_MIX));
        }

        return processor;
    }

    private static ChannelMixingMatrix identityMatrix(int channels) {
        float[] coefficients = new float[channels * channels];
        for (int i = 0; i < channels; i++) {
            coefficients[i * channels + i] = 1f;
        }
        return new ChannelMixingMatrix(channels, channels, coefficients);
    }

    /**
     * Builds the ExoPlayer audio sink, injecting the downmix processor when the
     * runtime preference is enabled. Only uses media3 APIs that exist in the
     * APK's bundled media3 version so it survives DEX verification.
     */
    public static AudioSink buildAudioSink(
            Context context,
            boolean enableFloatOutput,
            boolean enableAudioTrackPlaybackParams
    ) {
        init(context);

        DefaultAudioSink.Builder builder = new DefaultAudioSink.Builder(context)
                .setEnableFloatOutput(enableFloatOutput);
        if (isEnabled()) {
            builder.setAudioProcessors(new AudioProcessor[] { createDownmixProcessor() });
        }
        return builder.build();
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
        // Coefficients are laid out column-major in this media3 build:
        // coefficients[inputChannel * outputChannelCount + outputChannel].
        float[] coefficients = new float[inputChannelCount * 2];

        // Front left -> left, front right -> right (always).
        coefficients[0] = 1f;
        coefficients[1 * 2 + 1] = 1f;

        if (inputChannelCount >= 5) {
            // 5.0 / 5.1 / 6.1 / 7.1: index 2 is the front center channel.
            coefficients[2 * 2] = centerMix;
            coefficients[2 * 2 + 1] = centerMix;
            // Index 3 (LFE) is dropped; 4 and 5 are back left / back right.
            coefficients[4 * 2] = surroundMix;
            coefficients[5 * 2 + 1] = surroundMix;
        } else if (inputChannelCount == 4) {
            // Quad: index 2 = back left, index 3 = back right.
            coefficients[2 * 2] = surroundMix;
            coefficients[3 * 2 + 1] = surroundMix;
        }
        // 3-channel (2.1): index 2 is the LFE channel, dropped by leaving it 0.

        if (inputChannelCount == 7) {
            // 6.1: index 6 is the back center channel -> both outputs.
            coefficients[6 * 2] = surroundMix;
            coefficients[6 * 2 + 1] = surroundMix;
        } else if (inputChannelCount >= 8) {
            // 7.1: index 6 = side left, index 7 = side right.
            coefficients[6 * 2] = surroundMix;
            coefficients[7 * 2 + 1] = surroundMix;
        }

        return new ChannelMixingMatrix(inputChannelCount, 2, coefficients);
    }

    private static float dbToLinear(int db) {
        return (float) Math.pow(10d, db / 20d);
    }
}
