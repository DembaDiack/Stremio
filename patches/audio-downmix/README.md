# Force stereo downmix with center boost

Downmixes 5.1/7.1 audio to stereo and boosts the center channel before mixing, matching Kodi's **Center mix level** behavior on 2.0 speakers.

## Players

- **ExoPlayer:** replaces `DefaultRenderersFactory.buildAudioSink` with a call to `AudioDownmixBridge.buildAudioSink`, which attaches a `ChannelMixingAudioProcessor` configured for every channel count (1-8). Mono is upmixed to stereo, stereo is passed through untouched, and multichannel sources are downmixed with a boosted center channel. Tunnelled playback is disabled during player setup so the processor always applies.
- **libVLC:** adds `--stereo-mode=1` so multichannel sources are downmixed to stereo. libVLC does not expose Kodi-style per-channel boost, so dialogue improvement is less precise than ExoPlayer.

## Options

Runtime preferences are stored in `SharedPreferences` (`morphe_downmix`):

- `enabled` (default `true`) - toggles the downmix.
- `center_boost_db` (default `6`, range `0-30`) - center channel boost in dB.

The settings screen is exposed by `DownmixSettingsActivity`.
