package com.stremio.common.players.subtitles;

import android.content.Context;

import androidx.media3.exoplayer.DefaultRenderersFactory;

public class CustomRenderersFactory extends DefaultRenderersFactory {
    public CustomRenderersFactory(Context context) {
        super(context);
    }
}
