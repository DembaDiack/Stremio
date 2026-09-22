extension {
    name = "extensions/stremio.mpe"
}

android {
    namespace = "com.stremio.morphe.extension"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    sourceSets.named("main") {
        java.setSrcDirs(
            listOf(
                "../../patches/multi-account/src",
                "../../patches/addon-reordering/src",
                "../../patches/audio-downmix/src",
                "../../patches/audio-downmix/compile-stubs"
            )
        )
    }
}

dependencies {
    compileOnly("androidx.media3:media3-exoplayer:1.5.1")
    compileOnly("androidx.media3:media3-common:1.5.1")
}
