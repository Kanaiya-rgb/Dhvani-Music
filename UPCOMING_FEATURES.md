# 🚀 Upcoming Features Roadmap — Dhvani Music

Detailed implementation blueprints for the next top-requested features in Dhvani Music.

---

## 1. 🎛️ "Slowed + Reverb" & "Nightcore" Audio Mode

### Overview
A real-time audio manipulation mode allowing users to transform any song into a **Slowed + Reverb (Lofi Vibe)** or **Nightcore / Sped-Up (High Energy)** version on the fly with a single tap.

### Key Capabilities
- **🌙 Slowed + Reverb:**
  - Playback Speed: `0.85x` (configurable `0.75x` – `0.90x`)
  - Pitch Scaling: Scaled down with speed (classic tape/vinyl slowdown effect)
  - Audio FX: Environmental spacey room reverb (`EnvironmentalReverb` / stereo delay diffusion)
- **⚡ Nightcore (Sped Up):**
  - Playback Speed: `1.20x` – `1.30x`
  - Pitch Scaling: Scaled up with speed (anime/dance aesthetic)
  - Audio FX: Crisp treble and presence boost
- **🎧 Custom Mode:**
  - Independent **Speed** slider (`0.5x` – `2.0x`)
  - Independent **Pitch** slider (`-12 semitones` to `+12 semitones`)
  - **Reverb Depth** slider (`0%` to `100%`)

### Technical Architecture
- **Engine:** AndroidX Media3 / ExoPlayer `PlaybackParameters(speed, pitch)`.
- **Reverb Implementation:** Native Android `android.media.audiofx.EnvironmentalReverb` or `PresetReverb` attached to the current `audioSessionId` from `PlaybackService`.
- **UI Integration:**
  - A subtle glyph or capsule badge on the Now Playing screen (`Normal` · `Slowed 🌙` · `Nightcore ⚡`).
  - Tapping opens an aesthetic bottom sheet dialog with quick presets and fine-tuning sliders.

---

## 2. 📊 Dhvani "Wrapped" — Monthly & Weekly Listening Stats

### Overview
A personalized listening analytics experience (similar to Spotify Wrapped and Apple Music Replay) celebrating the user's musical journey with shareable visual story cards.

### Key Capabilities
- **📈 Core Metrics Tracked:**
  - **Total Listening Time:** Total minutes / hours listened this week, month, and all-time.
  - **Top 5 Songs:** Most repeated tracks with play count and total time.
  - **Top 5 Artists:** Favorite artists ranked by listen time.
  - **Listening Persona:** Fun archetype badge (e.g. *"Midnight Lofi Listener"*, *"Bass Head"*, *"Acoustic Dreamer"*).
  - **Most Active Hours:** Peak listening clock (Morning commuter, Afternoon focus, Late Night owl).
- **🎨 Shareable Story Cards (Instagram & WhatsApp):**
  - Full-screen animated Compose cards with dynamic mesh gradients derived from the user's top album covers.
  - One-tap **"Share to Instagram / WhatsApp"** button: Renders the Compose card directly into a high-res bitmap (`Bitmap.CompressFormat.PNG`) and opens the Android system share sheet.

### Technical Architecture
- **Data Source:** Dhvani's existing `PlaybackHistory` and `ListeningRecorder` local Room / SQLite database.
- **Aggregation:** A lightweight background DAO query aggregating `timestamp`, `durationListened`, and `videoId` grouped by week and month.
- **UI Integration:**
  - A prominent banner on the **Library** or **Home** tab: *"Your September in Music is ready! 🎧"*.
  - Story-style swipeable card pager (`HorizontalPager`) with smooth transitions and celebratory animations.

---

### 📅 Implementation Priority
1. **Slowed + Reverb & Nightcore Mode** (Audio Engine & NowPlaying UI)
2. **Dhvani Wrapped & Monthly Stats** (Database aggregation & Shareable story cards)
