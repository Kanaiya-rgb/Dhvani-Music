# Dhvani Music — Release Notes & Changelog (v2.4.0)

**Date:** September 23, 2026  
**Version:** `2.4.0` (VersionCode: `36`)  
**Repository:** [Kanaiya-rgb/Dhvani-Music](https://github.com/Kanaiya-rgb/Dhvani-Music)  
**Git Tag:** `v2.4.0`

---

## 🚀 Overview of Changes

Aaj ke session mein Dhvani Music ke liye important bug fixes, speed enhancements, home-screen widgets, UI/UX improvements, aur audio processing features implement kiye gaye hain.

---

## 1. 🐛 Bug Fixes

### A. Imported Playlist Redirection Bug Fixed
- **Issue:** Jab user kisi Spotify ya custom local playlist ko open karta tha, toh app use galat tarike se `Downloads` / `LocalMusicScreen` screen par bhej deta tha (jisse lagta tha ki playlist download ho rahi hai).
- **Fix:** [`MainActivity.kt`](file:///c:/Users/asus/Downloads/Music/app/src/main/java/com/music/dhvani/MainActivity.kt) ke andar `isDeviceFolder()` function ko update kiya gaya taaki `local:custom:` aur `local:liked` playlists ko device folders se alag treat kiya jaye aur proper playlist `DetailScreen` open ho.

### B. 4-Cover Mosaic Collage for Imported Playlists
- **Issue:** Imported playlists par single ya blank image dikh rahi thi, 4-track cover collage nahi aa raha tha.
- **Fix:**
  - [`Models.kt`](file:///c:/Users/asus/Downloads/Music/app/src/main/java/com/music/dhvani/data/model/Models.kt): `ShelfItem` data class mein `mosaicUrls: List<String> = emptyList()` field add kiya.
  - [`LibraryScreen.kt`](file:///c:/Users/asus/Downloads/Music/app/src/main/java/com/music/dhvani/ui/screens/LibraryScreen.kt): `ytShelfItems`, `spotifyShelfItems`, aur `spotifyProfileShelfItems` se playlist ke first 4 distinct songs ke thumbnails collect kiye.
  - [`HomeScreen.kt`](file:///c:/Users/asus/Downloads/Music/app/src/main/java/com/music/dhvani/ui/screens/HomeScreen.kt): Spotify-style 2×2 image grid layout create kiya jab playlist mein 4 images available hon.

---

## 2. ⚡ Performance Improvements

### Parallel Spotify Playlist Import (6x Faster)
- **File:** [`PlaylistManager.kt`](file:///c:/Users/asus/Downloads/Music/app/src/main/java/com/music/dhvani/data/playlist/PlaylistManager.kt)
- **Detail:** Pehle Spotify ke tracks one-by-one sequentially resolve hote the jisme user ko kaafi wait karna padta tha. Ab `kotlinx.coroutines.sync.Semaphore(6)` ka use karke tracks ko parallel batches mein fast import kiya jata hai.

---

## 3. 📱 Custom Home-Screen Widgets

App ke liye 2 sleek aur reactive home-screen widgets introduce kiye gaye:

1. **MediaWidgetPill (4×1 Pill Widget)**
   - Minimalist modern pill shape design.
   - Album artwork thumbnail, song title, artist name, aur Play/Pause, Next/Previous controls.
   - Layout: [`widget_media_pill.xml`](file:///c:/Users/asus/Downloads/Music/app/src/main/res/layout/widget_media_pill.xml)
   - Configuration: [`widget_media_pill.xml`](file:///c:/Users/asus/Downloads/Music/app/src/main/res/xml/widget_media_pill.xml)

2. **MediaWidgetTurntable (3×3 Turntable Vinyl Widget)**
   - Retro vinyl record aesthetic.
   - Rotating turntable look with circular artwork, song information, aur media controls.
   - Layout: [`widget_media_turntable.xml`](file:///c:/Users/asus/Downloads/Music/app/src/main/res/layout/widget_media_turntable.xml)
   - Configuration: [`widget_media_turntable.xml`](file:///c:/Users/asus/Downloads/Music/app/src/main/res/xml/widget_media_turntable.xml)

- Receiver & Logic: [`MediaWidget.kt`](file:///c:/Users/asus/Downloads/Music/app/src/main/java/com/music/dhvani/widget/MediaWidget.kt) & [`AndroidManifest.xml`](file:///c:/Users/asus/Downloads/Music/app/src/main/AndroidManifest.xml)

---

## 4. 🎛️ Audio Effects & DSP

- **Reverb Audio Processor:** [`ReverbAudioProcessor.kt`](file:///c:/Users/asus/Downloads/Music/app/src/main/java/com/music/dhvani/playback/audio/ReverbAudioProcessor.kt) Media3 ExoPlayer audio pipeline mein integrate kiya gaya.
- **Audio Effects Sheet:** [`AudioEffectsSheet.kt`](file:///c:/Users/asus/Downloads/Music/app/src/main/java/com/music/dhvani/ui/player/AudioEffectsSheet.kt) Now Playing screen se directly Reverb, Equalizer, aur Sound Booster control karne ke liye.

---

## 5. 🎨 UI/UX & Miscellaneous

- **ImportPlaylistSheet:**
  - One-tap clipboard paste & clear button.
  - Spotify public profile playlists batch import support.
  - Playlist cover preview in import sheet.
- **App Icons & Branding:**
  - Modern app launcher logos, vector assets, and notification logos updated across mipmap directories.
- **Repository Maintenance:**
  - [`.gitignore`](file:///c:/Users/asus/Downloads/Music/.gitignore) updated with debug scripts (`parsed_profile.txt`, `parse_profile.py`, `*.pyc`).
  - [`version.json`](file:///c:/Users/asus/Downloads/Music/version.json) & [`app/build.gradle.kts`](file:///c:/Users/asus/Downloads/Music/app/build.gradle.kts) bumped to version `2.4.0` (code `36`).

---

## 📦 Release Verification
- Build status: **BUILD SUCCESSFUL**
- Git tag: **`v2.4.0`**
- Git branch: **`main`**
