# Force stereo downmix with center boost

Downmixes 5.1/7.1 audio to stereo and boosts the center channel by 20 dB before mixing, matching Kodi's **Center mix level** behavior on 2.0 speakers.

## Players

- **ExoPlayer:** injects a custom `ChannelMixingAudioProcessor` through `DownmixRenderersFactory` and disables tunnelled playback during player setup.
- **libVLC:** adds `--stereo-mode=1` so multichannel sources are downmixed to stereo. libVLC does not expose Kodi-style per-channel boost, so dialogue improvement is less precise than ExoPlayer.

## Options

This first version hardcodes **+20 dB** center boost to match the requested Kodi setting.
