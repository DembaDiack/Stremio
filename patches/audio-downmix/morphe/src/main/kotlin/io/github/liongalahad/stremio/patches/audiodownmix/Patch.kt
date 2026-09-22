package io.github.liongalahad.stremio.patches.audiodownmix

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.findInstructionIndicesReversedOrThrow
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.methodCall
import app.morphe.patcher.newInstance
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import io.github.liongalahad.stremio.patches.shared.Constants.STREMIO_COMPATIBILITY

private const val BRIDGE = "Lcom/stremio/morphe/AudioDownmixBridge;"
private const val DOWNMIX_FACTORY = "Lcom/stremio/morphe/DownmixRenderersFactory;"
private const val CUSTOM_FACTORY = "Lcom/stremio/common/players/subtitles/CustomRenderersFactory;"

@Suppress("unused")
val audioDownmixPatch = bytecodePatch(
    name = "Force stereo downmix with center boost",
    description = "Downmixes multichannel audio to stereo and boosts the center channel for clearer dialogue on 2.0 speakers. Applies to ExoPlayer and libVLC.",
    default = true
) {
    compatibleWith(STREMIO_COMPATIBILITY)
    extendWith("extensions/stremio.mpe")

    execute {
        listOf(
            ExoPlayerCreateFingerprint,
            VlcOptionsFingerprint
        ).forEach { it.matchAll(1..1) }

        ExoPlayerCreateFingerprint.method.apply {
            val factoryFilter = newInstance(CUSTOM_FACTORY)

            findInstructionIndicesReversedOrThrow(factoryFilter).forEach { index ->
                val register = getInstruction<OneRegisterInstruction>(index).registerA
                replaceInstruction(index, "new-instance v$register, $DOWNMIX_FACTORY")
            }

            findInstructionIndicesReversedOrThrow(
                methodCall(CUSTOM_FACTORY, "<init>", "(Landroid/content/Context;)V")
            ).forEach { index ->
                replaceInstruction(
                    index,
                    "invoke-direct { v8, v11 }, $DOWNMIX_FACTORY-><init>(Landroid/content/Context;)V"
                )
            }

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

        VlcOptionsFingerprint.method.addInstructions(
            implementation!!.instructions.size - 1,
            """
                invoke-static { v1 }, $BRIDGE->appendVlcDownmixOptions(Ljava/util/List;)V
            """
        )
    }
}
