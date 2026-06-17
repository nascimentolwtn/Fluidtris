# Feature: Config Screen — Unified Game Controls

**Status**: Backlog  
**Date Added**: 2026-06-17  
**Priority**: Medium

## Overview

Consolidate all in-game UI controls (sound toggle, music toggle, next-piece buttons, new-game, exit) into a dedicated config screen. Only the pause button remains on the main play area. The config button replaces the current sound-toggle position.

## Motivation

- **Reduced visual clutter**: Remove 5+ buttons from the play area, leaving only pause.
- **Centralized game controls**: Group all config options in one logical place.
- **Better UX for touch**: Larger touch targets in a dedicated overlay instead of scattered buttons.
- **Prepare for feature expansion**: Config screen is the natural place for future options (difficulty, language, accessibility).

## Design

### Layout Changes

**Current State**:
- Sound toggle button at fixed position (e.g., `x: 300–400, y: 280–380`).
- Music toggle button below it.
- Next-Piece buttons (left/right) above new-game/exit.
- New Game, Exit buttons on pause overlay.

**New State**:
- **Config button** (gear icon or "⚙") positioned where sound toggle was (`x: 300–400, y: 280–380`).
- **Pause button** remains unchanged (top-right or top-center).
- **Config overlay** (modal dialog when config is tapped):
  - Centered full-screen semi-transparent background.
  - Button grid or vertical menu:
    1. **Toggle Sound** (current state shown: 🔊 / 🔇)
    2. **Toggle Music** (if music feature exists; show 🎵 / 🎵🔇)
    3. **Next Piece Left** (with label "Next ←")
    4. **Next Piece Right** (with label "Next →")
    5. **New Game** (red button)
    6. **Exit** (red button)
  - **Close / Back button** (top-right X or back arrow).
- **Enabled during pause**: Config button remains active when pause overlay is open; if config is activated on this screen, just keep game paused.
- **Disabled during game-over**: Config controls (Next, Music toggle) are inactive; only New Game and Exit are available.

### Constants to Add (GameConstants.kt)

```kotlin
CONFIG_BUTTON_X = 300f           // Left-aligned with margin
CONFIG_BUTTON_Y = 280f           // Below next-piece preview
CONFIG_BUTTON_WIDTH = 100f
CONFIG_BUTTON_HEIGHT = 100f

CONFIG_OVERLAY_BACKGROUND_COLOR = Color.argb(200, 0, 0, 0)  // Semi-transparent black
CONFIG_BUTTON_GRID_COLS = 2
CONFIG_BUTTON_GRID_ROWS = 3
CONFIG_BUTTON_PADDING = 20f
```

### Code Changes

#### 1. **GameEngine.kt**

Add state:
```kotlin
var isConfigOpen: Boolean = false

fun onConfigButtonTap() {
    isConfigOpen = true
}

fun closeConfig() {
    isConfigOpen = false
}

// Route config menu button taps:
fun onConfigToggleSound() {
    onToggleSoundButton()  // Reuse existing logic
}

fun onConfigToggleMusic() {
    // Toggle music state (delegate to SoundManager)
    soundManager.toggleBgMusic(context)
}

fun onConfigNextPieceLeft() {
    // Spawn next piece (existing onNextPieceButton logic)
    onNextPieceButton()
}

fun onConfigNextPieceRight() {
    // Same as onNextPieceButton for now (or add directional spawning later)
    onNextPieceButton()
}

fun onConfigNewGame() {
    reset()  // Reuse existing reset logic
    closeConfig()
}

fun onConfigExit() {
    closeConfig()
    // Delegate to activity (post callback to MainActivity)
    onGameExit?.invoke()
}
```

#### 2. **FluidTetrisView.kt**

Remove:
- Sound-toggle button drawing and touch detection (from `onDraw` and `onTouchEvent`).
- Music-toggle button (if it exists).
- Next-Piece left/right button drawing (from main play area).
- Direct tap handling for these buttons.

Add:
- **Config button** drawn at `(CONFIG_BUTTON_X, CONFIG_BUTTON_Y)`.
- **Config overlay** drawn conditionally when `engine.isConfigOpen`.
- **Touch handling** for config button and all overlay buttons.

Example overlay structure (pseudocode):
```kotlin
if (engine.isConfigOpen) {
    // Semi-transparent background
    canvas.drawRect(..., backgroundPaint)
    
    // Config button grid (3 rows × 2 cols):
    // [Sound Toggle] [Music Toggle]
    // [Next Left   ] [Next Right  ]
    // [New Game    ] [Exit        ]
    
    drawConfigButton(canvas, 0, 0, "🔊", toggleSoundHandler)
    drawConfigButton(canvas, 1, 0, "🎵", toggleMusicHandler)
    drawConfigButton(canvas, 0, 1, "Next ←", nextLeftHandler)
    drawConfigButton(canvas, 1, 1, "Next →", nextRightHandler)
    drawConfigButton(canvas, 0, 2, "New Game", newGameHandler)  // Red
    drawConfigButton(canvas, 1, 2, "Exit", exitHandler)  // Red
    
    // Close button (X) at top-right of overlay
    drawCloseButton(canvas, closeHandler)
}
```

#### 3. **FluidTetrisView.onTouchEvent()**

Add touch detection for:
- Config button tap → `engine.onConfigButtonTap()`
- Each overlay button (route to `engine.onConfig*()` methods).
- Close button → `engine.closeConfig()`.

#### 4. **Pause Overlay (existing)**

Ensure pause overlay is not drawn or is disabled when config is open (avoid modal overlap).

#### 5. **SoundManager.kt**

Ensure `toggleBgMusic()` exists and is wired correctly (may already exist from music feature).

### Tests to Add

**GameEngine tests** (`GameEngineStateTest.kt` or new file):
1. `configButton_opensConfigOverlay()` — config starts closed; tap opens it.
2. `configClosed_whenPauseOpens()` — opening pause auto-closes config.
3. `soundToggleInConfig_togglesSoundState()` — config sound button calls `onToggleSoundButton()`.
4. `musicToggleInConfig_togglesMusicState()` — config music button calls `soundManager.toggleBgMusic()`.
5. `nextButtonInConfig_spawnsPiece()` — left/right next buttons work in config overlay.
6. `newGameInConfig_resetsEngine()` — New Game button clears board and score.
7. `exitInConfig_closesConfig()` — Exit button closes overlay (actual activity exit is UI-level).
8. `configDisabledDuringPause()` — config button tap is no-op when paused.
9. `configDisabledDuringGameOver_exceptNewGameExit()` — during game-over, only New Game and Exit are active.

**FluidTetrisView tests** (integration, if using webapp-testing):
1. Config button tap opens overlay.
2. Overlay buttons are correctly positioned.
3. Close button closes overlay.
4. Pause button remains visible and functional.

### Implementation Order

1. Add constants to `GameConstants.kt`.
2. Add `isConfigOpen` state and routing methods to `GameEngine.kt`.
3. Write unit tests (GameEngine).
4. Implement `onDraw()` changes in `FluidTetrisView.kt` (config button + overlay).
5. Implement `onTouchEvent()` changes for all overlay buttons.
6. Remove old button-drawing code (sound/music/next toggles).
7. Test on device/emulator.
8. Verify pause overlay and game-over overlay still work.

### Success Criteria

- ✓ Config button is the only in-game button (besides pause).
- ✓ Config overlay opens on config button tap.
- ✓ All 6 controls accessible and functional from overlay.
- ✓ Overlay closes on back/close button or overlay-outside tap.
- ✓ No visual overlap with pause or game-over overlays.
- ✓ All 9 unit tests pass.
- ✓ Manual testing confirms all buttons work on device.

### Blockers / Dependencies

- **SoundManager.toggleBgMusic()** must exist (from music feature backlog item #2).
- Music feature must be partially implemented or stubbed (if not, skip music toggle for now).

### Notes

- If music feature is not ready, ship config screen without music toggle and add it later.
- "Next Left" and "Next Right" can be the same action for now (spawn next piece); directional spawning can be added later if needed.
- Close button can be tapped anywhere outside the overlay (standard pattern).