# Dennis Hi-Fi Player · v0.1.0

A standalone, offline, folder-based Android music player built for Samsung Galaxy S23 Ultra and other Android 8+ devices. This is **not** Dennis AI Mastering Lab, Stem Lab, or AutoMix Studio. Package name: `com.dennis.hifiplayer`.

## V0.1.0 implementation

- Professional dark-navy / champagne-gold / turquoise UI and custom adaptive launcher icon.
- User-selectable **multiple music folders**, persistent Android Storage Access Framework permissions, recursive subfolder scanning, filter by source folder, search by artist/track/album, and remove-folder without touching files.
- Local MP3, AAC, M4A, FLAC, WAV, OGG and OPUS (where supported by the phone's Android decoder) with Media3 ExoPlayer 1.11.1. Playback queue, next/previous, seek, shuffle, repeat, album art, background session and Android media controls.
- EQ OFF (default), AUTO, and MANUAL. Five manual gains from -6 to +6 dB. Auto EQ conservatively maps *embedded genre tags* onto gentle profiles. Unknown genre = flat. Profiles are not AI audio analysis; track-level acoustic classification is future work.
- Independent volume lift OFF by default, adjustable from +0.5 to +3 dB using Android's LoudnessEnhancer. Android compresses out-of-range samples, so no guarantee of fully transparent gain: use OFF for unchanged dynamics.
- Does not upload or transcode any songs, and does not change source audio files.

### Real limitations

* A source file may be FLAC/24-bit, but that **does not** mean playback is bit-perfect. Android mixer, sample-rate conversion, effects and Bluetooth codecs affect the output; bit-perfect USB output is not implemented.
* Hardware/system EQ implementations differ. The phone must expose Android's Equalizer effect on the player's session. If unavailable, the player keeps playing but EQ may not take effect. Similarly for LoudnessEnhancer.
* Large libraries can take time to scan because the app reads metadata. Scan is off the UI thread and capped at 12,000 tracks and 24 folder levels; persistent indexing is planned for later.
* Files in restricted directory roots (e.g. the Downloads *root*, the entire storage root, Android/data) cannot be granted via Android's folder picker.
* This source package is **not an APK** and has not been compiled in this environment. Validate the produced GitHub Actions build and test output quality on the actual S23 Ultra before considering production ready.

## Build APK on GitHub Actions

1. Create an **empty new GitHub repository**, e.g. `dennisp5-alt/Dennis-HiFi-Player` (do not merge into existing audio app repositories).
2. Upload the **contents** of this source folder to the new repository's `main` branch, preserving `.github/workflows/build-apk.yml`.
3. Visit **Actions → Build Android APK** and select **Run workflow**. It also builds automatically on a push to `main`.
4. After the workflow succeeds, select **Artifacts → Dennis-HiFi-Player-debug-v0.1.0** and download its ZIP. Unzip `app-debug.apk`, then install it on your Android phone (allow installing from the source you use).

Local build (Android Studio JDK 17, Android SDK API 36, Gradle 8.11.1): `gradle assembleDebug`.

## Audio quality engineering

By default `mode=off`, `boost=0` and the app adds no tonal EQ or gain effects. Auto profiles are gentle (+1.5 dB or less). For the most faithful playback on a particular headset/device, keep both modes off. Volume lift uses system loudness processing and is intentionally capped. **Do not use the +3 dB control as a substitute for safe listening volume.**

## Next planned validation

- Compile and run on S23 Ultra SM-S918B Android 16.
- Regression-test access to nested `Music/` folders with MP3, FLAC 16/24-bit, WAV, M4A and OPUS.
- Verify pause/resume, background lock-screen media controls and headset disconnects.
- Compare EQ OFF output and listening against reference music player; measure for clipping at each boost setting and evaluate device/vendor EQ availability.
- Exercise corrupted files, track change during rapid folder scans, and mixed sample rates.


## v0.1.1 music genres and EQ presets

- New **PRESET** EQ mode for Country, Rock, EDM/Eurodance, Pop, Rap/Hip-hop, R&B/Soul, 50s/Oldies, Christmas/Holiday, Jazz/Blues, Acoustic/Folk, and Classical/Natural.
- **AUTO** mode uses recognised embedded genre tags or user-assigned per-song or per-folder genres. The music is **not** analysed by AI; missing genres cannot be inferred reliably.
- Library genre filter chips show which preset category each song maps to. In Now Playing, tap the genre text to assign a track. In Library, choose a folder then tap **Assign genre to selected folder**. These settings never modify source audio files.
- Booster now ranges from off to +6 dB in 0.5 dB increments. Android LoudnessEnhancer may compress or distort higher peaks; this is not a guaranteed clip-free or true-peak-limited boost.
- The previous EQ/manual settings and folder library remain preserved through normal app upgrades.
