package io.github.liongalahad.stremio.patches.audiodownmix

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.iface.Method

private fun Method.parameters() = parameterTypes.map(CharSequence::toString)

internal object ExoPlayerCreateFingerprint : Fingerprint(
    returnType = "Landroidx/media3/exoplayer/ExoPlayer;",
    custom = { method, classDef ->
        classDef.type == "Lcom/stremio/common/players/ExoPlayer\$Companion;" &&
            method.name == "createPlayer" &&
            method.parameters() == listOf(
                "Landroid/content/Context;",
                "Lcom/stremio/core/types/resource/Stream;",
                "Lcom/stremio/core/types/profile/Profile\$Settings;",
                "Landroidx/media3/exoplayer/LoadControl;"
            )
    }
)

internal object VlcOptionsFingerprint : Fingerprint(
    returnType = "Ljava/util/List;",
    custom = { method, classDef ->
        classDef.type == "Lcom/stremio/common/players/VlcPlayer;" &&
            method.name == "getVlcOptions" &&
            method.parameters().isEmpty()
    }
)

internal object DefaultRenderersFactoryBuildAudioSinkFingerprint : Fingerprint(
    returnType = "Landroidx/media3/exoplayer/audio/AudioSink;",
    custom = { method, classDef ->
        classDef.type == "Landroidx/media3/exoplayer/DefaultRenderersFactory;" &&
            method.name == "buildAudioSink" &&
            method.parameters() == listOf(
                "Landroid/content/Context;",
                "Z",
                "Z"
            )
    }
)
