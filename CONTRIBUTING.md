# 🤝 Contributing to Dhvani Music

First off, thank you for considering contributing to **Dhvani Music**! 🎉  
Whether you are fixing a bug, adding new visual themes, improving audio streaming, or refining the translation, your help is warmly welcomed and deeply appreciated.

---

## 📋 Table of Contents
1. [Code of Conduct](#-code-of-conduct)
2. [Getting Started & Local Setup](#-getting-started--local-setup)
3. [Project Architecture](#-project-architecture)
4. [Development Workflow & Guidelines](#-development-workflow--guidelines)
5. [Submitting a Pull Request](#-submitting-a-pull-request)
6. [Reporting Bugs & Requesting Features](#-reporting-bugs--requesting-features)

---

## 📜 Code of Conduct
We are committed to providing a welcoming, inclusive, and harassment-free experience for everyone. Be respectful, kind, and supportive of your fellow developers and community members.

---

## 🛠️ Getting Started & Local Setup

### Prerequisites
- **Android Studio** Ladybug (2024.2.1+) or newer
- **JDK 17** (or JDK 21)
- **Android SDK** API 34+
- **Git**

### Clone & Build
```bash
# 1. Clone the repository
git clone https://github.com/Kanaiya-rgb/Dhvani-Music.git
cd Dhvani-Music

# 2. Build the project locally
./gradlew compileDevDebugKotlin

# 3. Assemble Debug APK
./gradlew assembleDevDebug
```

---

## 🏗️ Project Architecture

Dhvani Music follows a clean, single-activity modern Jetpack Compose architecture:

| Package Layer | Path | What's Inside |
|---|---|---|
| **UI Screens** | `app/src/main/java/com/music/dhvani/ui/screens/` | Home, Search, Library, Categories, Settings |
| **Player & Visuals** | `app/src/main/java/com/music/dhvani/ui/player/` | Fullscreen Player, Motion Canvas, Mesh Gradient |
| **Core Components** | `app/src/main/java/com/music/dhvani/ui/components/` | Custom seekbars/sliders, dialogs, bottom sheets, mini-player |
| **Playback Engine** | `app/src/main/java/com/music/dhvani/playback/` | Media3 `PlaybackService`, ExoPlayer, cache & gapless playback |
| **Data & APIs** | `app/src/main/java/com/music/dhvani/data/` | InnerTube streaming, lyrics providers, canvas resolvers |

> 💡 Detailed map available in [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md).

---

## 🎨 Development Workflow & Guidelines

1. **Jetpack Compose First**: All new UI components must be built in idiomatic Jetpack Compose with Material 3 theming.
2. **Smooth & Aesthetic**: Preserve fluid animations, subtle haptics, and clean dark-mode contrast (including Pure Black AMOLED).
3. **Audio Safety**: Changes to `PlaybackService`, `AudioCache`, or `StreamResolver` must never block the main UI thread.
4. **Compile Check Before Push**:
   Always verify your code compiles cleanly before committing:
   ```bash
   ./gradlew compileDevDebugKotlin
   ```

---

## 🚀 Submitting a Pull Request

1. **Fork the repository** on GitHub.
2. **Create a topic branch** from `main`:
   ```bash
   git checkout -b feature/my-cool-feature
   ```
3. **Commit your changes** with clear, descriptive commit messages:
   ```bash
   git commit -m "feat(ui): add new neon slider style"
   ```
4. **Push to your fork**:
   ```bash
   git push origin feature/my-cool-feature
   ```
5. **Open a Pull Request** to the `main` branch of `Kanaiya-rgb/Dhvani-Music`:
   - Provide a concise description of your changes.
   - Include screenshots or screen recordings for any UI changes.
   - Reference any related issues (e.g. `Fixes #12`).

---

## 🐛 Reporting Bugs & Requesting Features

- **Found a bug?** Open an issue on [GitHub Issues](https://github.com/Kanaiya-rgb/Dhvani-Music/issues). Please include your Android version, device model, and reproduction steps.
- **Have a feature idea?** Start a discussion or open a feature request issue!

---

### ❤️ Thank You!
Your contributions help make Dhvani Music one of the most aesthetic, high-fidelity, and privacy-focused open-source music players in the world! 🎵✨
