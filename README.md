# Fluidtris v3.1

A fluid, touch-driven Tetris-like game for Android where pieces can be freely dragged, rotated, and collide with each other before locking into place.

## 🎮 Overview

Fluidtris reimagines the classic block-dropping puzzle with intuitive physics-based interaction and multi-piece simultaneous gameplay. Built with Kotlin and targeting Android API 29+, the game features:

- **Fluid Controls**: Drag pieces anywhere on screen and rotate them with swipe gestures
- **Multi-Piece Physics**: Multiple pieces fall simultaneously and collide with each other and the grid
- **Level Progression**: Difficulty scales with score; higher levels increase piece fall speed
- **Interactive Controls**: Next button spawns additional falling pieces; dual positioning for left/right-handed players
- **Pause & Game Over**: Comprehensive pause overlay with sound toggle; celebration audio on game over
- **Smooth Animation**: 60 FPS game loop with snap-to-grid animations and responsive touch feedback
- **Sound Effects**: Audio feedback for line clears, piece locking, and level-up events

## 🏗️ Architecture

Game logic is cleanly separated across six specialized files:

- **`MainActivity.kt`** — Activity boilerplate handling edge-to-edge display setup
- **`FluidTetrisView.kt`** — Thin custom View: rendering and touch input only, no game logic
- **`GameEngine.kt`** — All game state and logic (zero Android imports for testability)
- **`GameConstants.kt`** — Every magic number: grid, margins, speeds, delays, piece shapes/colors
- **`GameMath.kt`** — Pure math helpers: rotations, collision detection, wall clamping
- **`SoundManager.kt`** — MediaPlayer wrappers for audio feedback
- **`HighScoreManager.kt`** — SharedPreferences persistence for high scores and levels

## 🛠️ Build & Run

### Prerequisites
- Android Studio with Kotlin support
- Android SDK 29+
- Connected device or emulator

### Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device/emulator
./gradlew installDebug

# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Run a single unit test class
./gradlew test --tests "com.libuy.fluidtris.ExampleUnitTest"
```

## 🎯 Key Features

### Fluid Input System
- **Dragging**: Move pieces freely in screen space
- **Rotation**: 
  - Swipe top half of piece → clockwise rotation
  - Swipe bottom half → counter-clockwise rotation
- **Smart Locking**: 3-second snap-to-grid animation when piece contacts bottom or other pieces

### Multi-Piece Gameplay
- **Simultaneous Falls**: Multiple pieces fall and collide with each other independently
- **Next Button**: Two large squared buttons let you spawn additional falling pieces immediately — one positioned above "New Game" (left side) for left-handed players, one above "Exit" (right side) for right-handed players. Disabled during pause and game over. Next-piece preview rotates to show orientation

### Level Progression
- **Progressive Difficulty**: Game levels increase with score (every 300 points); each level multiplies gravity by 1.3x up to 3x maximum
- **Level Display**: Current level shown in top-left corner
- **Level-Up Audio**: Celebratory sound on advancement

### Pause & Game Over
- **Pause Controls**: Three conveniently positioned pause buttons accessible mid-game
- **Pause Overlay**: Displays Resume, New Game, Exit options plus a sound toggle
- **Game Over Celebration**: Special audio feedback when the game ends

### Grid System
- **Dimensions**: 8 columns × 20 rows
- **Cell Sizing**: Dynamic calculation based on screen size
- **Block Size**: Fixed at 100px for consistent visual appearance

### Audio
- `move_sound.ogg` - Line clear effect
- `rigid_sound.wav` - Piece lock effect
- Game Over celebration audio
- Level-up sound

## ⚠️ Known Issues & Limitations

### Placeholder Audio Files
Game-over celebration sound and high-score bonus sound are currently sourced from temporary audio assets. Future work should replace `game_over_sound.mp3` and `high_score_cheer.mp3` with appropriately licensed CC0 audio files that match the game's tone.

## 🎨 Rendering

- Imperative drawing via `Canvas` in `onDraw()`
- Single reused `Paint` object for performance
- Full-screen background image as base layer
- All game elements drawn on top of background

## 📱 Requirements

- **Minimum SDK**: Android 29 (Android 10)
- **Language**: Kotlin
- **Build System**: Gradle

## 🤝 Contributing

This project is open to contributions. Please note the known issues section when working on physics or collision-related features.

## 📄 License

This project is licensed under the MIT License - see the [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT) file for details.
---

*Built with ❤️ using Kotlin and Android's Canvas API*
