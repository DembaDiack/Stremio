package io.github.liongalahad.stremio.patches.audiodownmix

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import io.github.liongalahad.stremio.patches.shared.Constants.STREMIO_COMPATIBILITY
import org.w3c.dom.Document
import org.w3c.dom.Element

private const val BRIDGE = "Lcom/stremio/morphe/AudioDownmixBridge;"
private const val DOWNMIX_FACTORY = "Lcom/stremio/morphe/DownmixRenderersFactory;"
private const val CUSTOM_FACTORY = "Lcom/stremio/common/players/subtitles/CustomRenderersFactory;"

private val downmixSettingsResourcePatch = resourcePatch {
    compatibleWith(STREMIO_COMPATIBILITY)

    execute {
        document("AndroidManifest.xml").use(::transformManifest)
        document("res/layout/activity_main.xml").use(::transformMainLayout)
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
            VlcOptionsFingerprint
        ).forEach { it.matchAll(1..1) }

        ExoPlayerCreateFingerprint.method.apply {
            implementation!!.instructions.withIndex().forEach { (index, instruction) ->
                val reference = (instruction as? ReferenceInstruction)?.reference
                    ?: return@forEach
                when (reference) {
                    is TypeReference -> {
                        if (reference.type == CUSTOM_FACTORY) {
                            val register = getInstruction<OneRegisterInstruction>(index).registerA
                            replaceInstruction(index, "new-instance v$register, $DOWNMIX_FACTORY")
                        }
                    }
                    is MethodReference -> {
                        if (reference.definingClass == CUSTOM_FACTORY && reference.name == "<init>") {
                            replaceInstruction(
                                index,
                                "invoke-direct { v8, v11 }, $DOWNMIX_FACTORY-><init>(Landroid/content/Context;)V"
                            )
                        }
                    }
                    else -> Unit
                }
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

private fun transformMainLayout(document: Document) {
    val root = document.documentElement ?: return
    root.appendChild(document.createElement("com.stremio.morphe.MorpheDownmixNavView").apply {
        setAttribute("android:layout_width", "160dp")
        setAttribute("android:layout_height", "48dp")
        setAttribute("android:layout_gravity", "start|top")
        setAttribute("android:layout_marginStart", "20dp")
        setAttribute("android:layout_marginTop", "140dp")
    })
}
