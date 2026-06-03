# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Fluidtris is an Android Tetris-like game with a "fluid" physics twist: pieces can be freely dragged and rotated by touch before they lock into place. Built with Kotlin, targeting Android API 29+.

## Build & Run

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

## Architecture

The entire game is implemented in two files:

- **`MainActivity.kt`** — Boilerplate activity that sets up edge-to-edge display and hosts `FluidTetrisView`.
- **`FluidTetrisView.kt`** — Custom `View` containing all game logic, rendering, and input handling.

### FluidTetrisView internals

**Game loop**: A `Handler` running on the main looper fires `update()` at ~60 FPS via `postDelayed(16ms)`.

**Physics model**: Falling pieces use a constant `gravity` speed rather than true acceleration. A spring system (`springForceX/Y`, `springConstant`, `damping`) was scaffolded but is currently unused in the active code path.

**Piece locking**: When a piece touches the bottom wall or another placed piece, a 3-second timer starts (`bottomCollisionTime` / `pieceCollisionTime`). If the piece stays in contact for 3 seconds, `turnPieceRigid()` is called, which snaps `pieceRotation` to the nearest 90°, writes the piece cells into `grid[][]`, and spawns the next piece.

**Grid coordinate system**: The play area is `gridColumns=7` × `gridRows=20`. Grid cells and pixel positions are computed from `cellWidth = (width - 100f) / gridColumns` and `cellHeight = (height - 250f) / gridRows`. Piece block size is hardcoded at `100f` px.

**Touch input**: Dragging a piece moves it in screen space. Horizontal drags on the top half of a piece rotate it clockwise; drags on the bottom half rotate it counter-clockwise. Rotation is applied via canvas transform — the grid collision logic uses `rotatePiece()` to get the logical rotated shape (supports 0/90/180/270°).

**Sound**: Two `MediaPlayer` instances — `move_sound.ogg` (line clear) and `rigid_sound.wav` (piece lock). Loaded from `res/raw/`.

**Rendering**: All drawing is done imperatively in `onDraw()` using `Canvas` and a single reused `Paint` object. The next-piece preview currently renders only a colored square (not the actual piece shape).

## Known Issues / Design Notes

- `collideWithAnotherPiece()` is defined but never called; piece-collision locking goes through the timer path in `checkCollisions()` instead.
- High score is stored only in memory (`highScore` field) — it resets when the app is closed.
- `springForceX/Y` fields are set during touch but never applied to movement or rotation.
- Rotation collision detection does not account for the rotated piece shape when checking wall bounds (`keepPiecesInsideWalls` uses the un-rotated shape).
