# Dhvani Music — Release Notes & Changelog (v2.5.1)

**Date:** September 28, 2026  
**Version:** `2.5.1` (VersionCode: `38`)  
**Repository:** [Kanaiya-rgb/Dhvani-Music](https://github.com/Kanaiya-rgb/Dhvani-Music)  
**Git Tag:** `v2.5.1`  
**APK Asset:** `DhvaniMusic-v2.5.1.apk` (Size: ~35 MB)

---

## 🚀 Overview of Changes

In this update, Dhvani Music receives key refinements to the Ambient Cinema landscape mode, a brand-new bottom-right floating quick shuffle player, dynamic island optimizations, and pixel-perfect loading skeletons.

---

## 1. 🎬 Ambient Cinema Landscape Mode Overhaul
- **File:** `AmbientCinemaLandscapeOverlay.kt`
- **Expanded Cover Art:**
  - Scaled cover artwork from ~156dp up to **236dp** with smooth animated transitions when controls show/hide.
- **Interactive Queue Drawer with Remove Song Option:**
  - Added a dedicated bottom-right Queue button (`queue_music` icon) opening a frosted glass side drawer.
  - Lists all upcoming songs with live animated audio equalizer on the active track.
  - Added a **Cross ('X') button** on each song allowing users to remove any unwanted tracks directly from the queue.
- **Tap-to-Reveal Controls:**
  - Full-screen Cinema Mode starts clean with only the song title, artist, cover, and synced lyrics.
  - Tapping anywhere reveals playback slider, play/pause, next/previous buttons with a 5-second auto-hide timer.
- **Fine-Grained Volume Gestures (1%–2% Steps):**
  - Replaced coarse 5% volume increments with a smooth 1%–2% gesture HUD on the right side of the screen.
- **Boosted Active Karaoke Lyrics:**
  - Boosted active lyric line font size to **29.sp** with **39.sp** line height for effortless readability across landscape orientations.

---

## 2. 🔀 Floating "Play Random" / Quick Shuffle Action Button
- **Files:** `MainActivity.kt`, `HomeScreen.kt`
- **Floating Action Button (FAB):**
  - Placed at the bottom-right of the Home screen, positioned right above the Mini Player.
  - Uses a vibrant amber-saffron gradient with subtle glowing shadow (`DhvaniIcons.Shuffle`).
  - Shuffles recommendations and starts playback instantly with one tap.
- **Settings Connected:**
  - Tied to `AppSettings.showPlayRandomButton` ("Show play random button") so users can toggle it on/off anytime from Appearance Settings.

---

## 3. 🏝️ In-App Dynamic Island Optimization
- **File:** `DynamicIslandPlayer.kt`
- **Problem:** Compact dynamic island was ~240dp wide, covering the top bar icons (Listen Together, History, Profile).
- **Fix:** Redesigned in-app compact island into an authentic Apple-style **ultra-compact notch capsule (~65dp)** with spinning mini artwork and live equalizer.
- **Result:** Keeps all TopAppBar icons (**Listen Together**, **History / Notifications**, and **User Avatar**) 100% visible and unobstructed.

---

## 4. 💀 Pixel-Perfect Home Feed Skeletons
- **File:** `Skeletons.kt`
- **Problem:** When loading the app, `feedSkeleton()` showed a giant empty 300dp+ box which caused an abrupt layout jump when data loaded.
- **Fix:** Added `SongShelfSkeleton(rows = 3)` to mirror the real 3-row "Listen Again" / Quick Picks layout, eliminating layout shift completely.

---

## 📦 Release Asset Verification
- **APK Path:** `app/build/outputs/apk/prod/release/DhvaniMusic-v2.5.1.apk`
- **Build Variant:** `prodRelease` (signed)
- **Size:** 36,645,268 bytes (~35 MB)
