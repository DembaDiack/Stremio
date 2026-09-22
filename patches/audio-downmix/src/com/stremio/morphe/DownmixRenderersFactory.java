package com.stremio.morphe;

import android.content.Context;

import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.common.audio.ChannelMixingAudioProcessor;
import androidx.media3.exoplayer.audio.AudioSink;
import androidx.media3.exoplayer.audio.DefaultAudioSink;

import com.stremio.common.players.subtitles.CustomRenderersFactory;

public final class DownmixRenderersFactory extends CustomRenderersFactory {
    public DownmixRenderersFactory(Context context) {
        super(context);
    }

    @Override
    protected AudioSink buildAudioSink(
            Context context,
            boolean enableFloatOutput,
            boolean enableAudioTrackPlaybackParams
    ) {
        ChannelMixingAudioProcessor processor = AudioDownmixBridge.createDownmixProcessor();

        return new DefaultAudioSink.Builder(context)
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioOutputPlaybackParameters(enableAudioTrackPlaybackParams)
                .setAudioProcessors(new AudioProcessor[] { processor })
                .build();
    }
}
