package io.github.liongalahad.stremio.patches.audiodownmix

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import io.github.liongalahad.stremio.patches.shared.Constants.STREMIO_COMPATIBILITY
import org.w3c.dom.Document
import org.w3c.dom.Element

private const val BRIDGE = "Lcom/stremio/morphe/AudioDownmixBridge;"

private val downmixSettingsResourcePatch = resourcePatch {
    compatibleWith(STREMIO_COMPATIBILITY)

    execute {
        document("AndroidManifest.xml").use(::transformManifest)
    }
}

@Suppress("unused")
val audioDownmixPatch = bytecodePatch(
    name = "Force stereo downmix with center boost",
    description = "Downmixes multichannel audio to stereo and boosts the center channel for clearer dialogue on 2.0 speakers. Applies to ExoPlayer and libVLC.",
    default = true
) {
    compatibleWith(STREMIO_COMPATIBILITY)
    dependsOn(downmixSettingsResourcePatch)
    extendWith("extensions/stremio.mpe")

    execute {
        listOf(
            ExoPlayerCreateFingerprint,
            VlcOptionsFingerprint,
            DefaultRenderersFactoryBuildAudioSinkFingerprint
        ).forEach { it.matchAll(1..1) }

        // Replace DefaultRenderersFactory.buildAudioSink (the inherited method that
        // constructs the audio sink) with a delegation to the bridge. Subclassing
        // CustomRenderersFactory is impossible because it is a final Kotlin class.
        DefaultRenderersFactoryBuildAudioSinkFingerprint.method.apply {
            val instructionCount = implementation!!.instructions.size
            implementation!!.removeInstructions(0, instructionCount)
            addInstructions(
                0,
                """
                    invoke-static { p1, p2, p3 }, $BRIDGE->buildAudioSink(Landroid/content/Context;ZZ)Landroidx/media3/exoplayer/audio/AudioSink;
                    move-result-object v0
                    return-object v0
                """
            )
        }

        // Disable tunnelled playback so the downmix processor always applies.
        ExoPlayerCreateFingerprint.method.apply {
            val tunnelingCallIndex = implementation!!.instructions.indexOfFirst { instruction ->
                val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
                    ?: return@indexOfFirst false
                reference.definingClass ==
                    "Landroidx/media3/exoplayer/trackselection/DefaultTrackSelector\$Parameters\$Builder;" &&
                    reference.name == "setTunnelingEnabled"
            }
            if (tunnelingCallIndex >= 0) {
                addInstructions(
                    tunnelingCallIndex,
                    """
                        const/4 v7, 0x0
                    """
                )
            }
        }

        VlcOptionsFingerprint.method.apply {
            addInstructions(
                implementation!!.instructions.size - 1,
                """
                    invoke-static { v1 }, $BRIDGE->appendVlcDownmixOptions(Ljava/util/List;)V
                """
            )
        }
    }
}

private fun transformManifest(document: Document) {
    val application = document.getElementsByTagName("application").item(0) as? Element
        ?: return
    application.appendChild(document.createElement("activity").apply {
        setAttribute("android:name", "com.stremio.morphe.DownmixSettingsActivity")
        setAttribute("android:exported", "false")
        setAttribute("android:screenOrientation", "landscape")
        setAttribute("android:theme", "@android:style/Theme.Material.NoActionBar")
    })
}
