# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview - v3.1

Fluidtris is an Android Tetris-like game with a "fluid" physics twist: pieces can be freely dragged and rotated by touch before they lock into place. Features multi-piece simultaneous gameplay with physics-based collision, a progression system with level thresholds, and interactive controls. Built with Kotlin, targeting Android API 29+.

## Build & Run

```bash
# Build debug APK
./gradlew assembleDebug

# Install on all connected devices/emulators
./gradlew installDebug

# Run unit tests (no device needed)
./gradlew testDebugUnitTest

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest
```

## Commit Message Style

Keep messages concise: `type: brief subject` (1–2 lines max, no elaborate body). See global memory for details and examples.

## Architecture

The game is split across six files:

- **`MainActivity.kt`** — Boilerplate activity that sets up edge-to-edge display and hosts `FluidTetrisView`.
- **`FluidTetrisView.kt`** — Thin `View`: rendering (`onDraw`) and touch input only. No game logic.
- **`GameEngine.kt`** — All game state and logic. Zero Android imports.
- **`GameConstants.kt`** — Every magic number: grid size, margins, speeds, delays, piece shapes/colors.
- **`GameMath.kt`** — Pure math helpers: `rotatedBlockCenters`, `rotatePiece`, `lerpAngleDeg`, wall clamping, hit-testing.
- **`SoundManager.kt`** — `MediaPlayer` wrappers for lock and line-clear sounds.
- **`HighScoreManager.kt`** — SharedPreferences persistence for high score.

### GameEngine internals

**Game loop**: `FluidTetrisView` drives a `Handler` at ~60 FPS (`postDelayed(16ms)`), calling `engine.update(width, height)` then `invalidate()`.

**Physics model**: Falling pieces use a constant `GRAVITY` speed. `springForceX` carries horizontal momentum after drag release, decaying each frame by `SPRING_DAMPING`.

**Multi-piece physics**: The game maintains an `ActivePiece` list where multiple pieces can fall simultaneously. Each piece has independent velocity, rotation, and touch-drag state. Pieces collide with the grid walls, the bottom, and each other. When one piece snaps-animates after collision, other pieces continue falling and can bounce off snap-animating neighbors.

**Piece locking / snap animation**: When a piece touches the bottom wall or another placed piece and `canLockCleanly()` passes, a 3-second timer starts. Simultaneously, `beginSnapAnimation()` computes the exact grid-aligned target `(snapTargetX, snapTargetY, snapTargetRotation)` once. Each frame, `applySnapPull(t)` lerps the piece toward that target with a linear ramp — X at `SNAP_PULL_SPEED`, rotation at `SNAP_ROTATION_SPEED` (stronger). The timer is guarded: transit movement (including upward Y) does not reset it; only the user dragging to open space cancels the snap and returns the piece to fluid. When the timer expires, `turnPieceRigid()` snaps rotation to the nearest 90°, writes cells into `grid[][]`, and spawns the next piece. Other active pieces continue falling uninterrupted.

**Grid coordinate system**: The play area is `GRID_COLUMNS=7` × `GRID_ROWS=20`. Margins: `GRID_LEFT=150f`, `GRID_RIGHT_MARGIN=150f`, `GRID_TOP=100f`, `GRID_BOTTOM_MARGIN=180f`. Derived: `cellWidth = (width - 300f) / 7`, `cellHeight = (height - 280f) / 20`. Piece block size: `PIECE_SIZE=100f` px.

**Touch input**: Dragging a center block moves the piece; dragging a non-center block rotates it. Rotation uses a torque formula (cross product of block→center vector with drag delta). All pixel-space collision and wall checks use `rotatedBlockCenters()`, which mirrors the `canvas.rotate()` transform exactly.

**Next Piece Button**: A player-friendly feature that advances the falling piece to its successor without waiting for natural lock. Available as two conveniently positioned large squared buttons: one above the "New Game" button (left side) for left-handed players, and one above the "Exit" button (right side) for right-handed players. Disabled during pause and game over states. The next piece preview (shown above the buttons) rotates to show the next piece's orientation, letting players plan ahead.

**Level system**: Players progress through levels as their score exceeds `NEXT_LEVEL_SCORE` thresholds. Each level increases piece fall speed, making gameplay progressively harder. Advancement is tracked in `currentLevel` and persists across game sessions alongside the high score.

**Sound**: `move_sound.ogg` (line clear), `rigid_sound.wav` (piece lock), game-over celebration audio. Loaded from `res/raw/`. A mute toggle is available on the pause overlay for convenience.

**Pause & Game Over**: Players can pause the game via three conveniently positioned pause buttons. The pause overlay displays remaining UI controls (Resume, New Game, Exit) and a sound toggle. Game Over state triggers a celebration sound and prevents further gameplay until "New Game" is selected.

**Rendering**: Imperative `Canvas` drawing in `onDraw()`. Pieces draw as jelly rounded-rects that shrink toward crisp squares as `getCollisionSolidity()` rises from 0→1 during the lock countdown. Background image fills the screen. Next-piece preview renders a colored square.

**Testability**: `GameEngine.currentTimeMs` is an injectable `() -> Long` (defaults to `System::currentTimeMillis`). Unit tests inject a fake clock advanced 16 ms per tick so timer-dependent behavior is fast and deterministic.

## Known Issues / Design Notes

- High score is stored only in SharedPreferences as a plain integer — no player name or date tracking.
- Next-piece preview renders only a colored square, not the actual piece shape.
