# Fluidtris

A fluid, touch-driven Tetris-like game for Android where pieces can be freely dragged and rotated before locking into place.

## 🎮 Overview

Fluidtris reimagines the classic block-dropping puzzle with intuitive physics-based interaction. Built with Kotlin and targeting Android API 29+, the game features:

- **Fluid Controls**: Drag pieces anywhere on screen and rotate them with swipe gestures
- **Classic Gameplay**: Clear lines by filling rows with 7×20 grid play area
- **Smooth Animation**: 60 FPS game loop with responsive touch feedback
- **Sound Effects**: Audio feedback for line clears and piece locking

## 🏗️ Architecture

The entire game logic is contained in just two files:

- **`MainActivity.kt`** - Activity boilerplate handling edge-to-edge display setup
- **`FluidTetrisView.kt`** - Custom View with complete game implementation including:
  - Game loop (60 FPS via Handler)
  - Physics and collision detection
  - Touch input handling
  - Rendering with Canvas
  - Sound management

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
- **Smart Locking**: 3-second timer when piece contacts bottom or other pieces

### Grid System
- **Dimensions**: 7 columns × 20 rows
- **Cell Sizing**: Dynamic calculation based on screen size
- **Block Size**: Fixed at 100px for consistent visual appearance

### Audio
- `move_sound.ogg` - Line clear effect
- `rigid_sound.wav` - Piece lock effect

## ⚠️ Known Issues & Limitations

| Issue | Description | Status |
|-------|-------------|--------|
| High Score Persistence | High score stored only in memory, resets on app close | Open |
| Unused Spring Physics | Spring system scaffolded but not active in game loop | Open |
| Collision Detection | `collideWithAnotherPiece()` defined but never called | Open |
| Rotation Bounds | Wall collision doesn't account for rotated piece shape | Open |
| Next Piece Preview | Currently renders colored square instead of actual piece | Open |

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
