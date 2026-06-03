# Fluidtris Refactoring: Thin View + Game Engine

## Context

All 723 lines of game logic, rendering, audio, and input handling live in a single `FluidTetrisView.kt` — a classic God Object. Refactoring into a **Thin View + Game Engine** pattern will:
- Make game logic unit-testable without an Android device
- Separate rendering from state
- Eliminate ~80 lines of confirmed dead code
- Provide a clean foundation for future features (high-score persistence, rotation wall-fix)

The pattern chosen — pure `GameEngine` + thin Android `View` — is intentionally lighter than full MVC; a third Controller object would add a file with no real benefit given the game's scale.

---

## Target File Layout

| File | Lines (est.) | Responsibility |
|---|---|---|
| `GameConstants.kt` | ~60 | All magic numbers and piece data |
| `GameMath.kt` | ~65 | Four pure helper functions (existing tests target these) |
| `SoundManager.kt` | ~45 | `MediaPlayer` lifecycle, zero game logic |
| `GameEngine.kt` | ~220 | All mutable game state + logic; **zero Android imports** |
| `FluidTetrisView.kt` | ~160 | Rendering, input translation, game loop timer |

All new files live in `app/src/main/java/com/libuy/fluidtris/`.

---

## File Specs

### `GameConstants.kt` — `object GameConstants`

Move from `FluidTetrisView`:
- `GRID_COLUMNS = 7`, `GRID_ROWS = 20`
- `PIECE_SIZE = 100f`
- `GRAVITY = 2.0f`, `UPWARD_DRAG_FACTOR = 0.4f`, `ROTATION_SENSITIVITY = 30f`
- `LOCK_DELAY_MS = 3000L`, `GAME_LOOP_INTERVAL_MS = 16L`
- `GRID_LEFT = 150f`, `GRID_TOP = 100f`, `GRID_RIGHT_MARGIN = 150f`, `GRID_BOTTOM_MARGIN = 180f`
- `pieces`, `pieceColors`, `pieceCenterCells` (the 7 tetromino definitions)

---

### `GameMath.kt` — top-level functions (no class wrapper)

**Cut verbatim** from the bottom of `FluidTetrisView.kt`. Zero logic changes, zero signature changes:
```kotlin
internal fun clampPieceX(...)
internal fun hitCellFromTouch(...)
internal fun effectiveVerticalDrag(...)
internal fun rotationDeltaFromDrag(...)
```

Both existing unit test files (`WallClampTest`, `TouchControlTest`) are in `package com.libuy.fluidtris` and call these functions by bare name. Moving them to a same-package `.kt` file requires **no test changes** — Kotlin's package scope covers it.

---

### `SoundManager.kt` — `class SoundManager(context: Context)`

```kotlin
class SoundManager(context: Context) {
    private var movePlayer: MediaPlayer?
    private var rigidPlayer: MediaPlayer?
    fun playMove()
    fun playRigid()
    fun release()
}
```

This is the only file that imports `android.media.MediaPlayer`. `GameEngine` calls a lambda callback instead of holding a sound reference.

---

### `GameEngine.kt` — `class GameEngine(private val onPieceLocked: () -> Unit)`

All current `FluidTetrisView` state fields move here **except** paint, bitmap, handler, and updateRunnable. Methods that receive screen dimensions accept `viewWidth: Int, viewHeight: Int` as parameters (since `width`/`height` are `View` properties unavailable here).

**Fields kept:**
- `grid`, `score`, `highScore`
- `pieceX`, `pieceY`, `pieceRotation`, `velocityY`
- `currentPiece`, `currentPieceColor`, `nextPiece`, `nextPieceColor`
- `isDragging`, `isDraggingCenter`, `touchedBlockRow`, `touchedBlockCol`, `lastTouchX`, `lastTouchY`
- `isPaused`, `isGameOver`, `wasManuallyPausedBeforeSystemPause`
- `bottomCollisionTime`, `pieceCollisionTime`, `isWaitingToTurnRigidAtBottom`, `isWaitingToTurnRigidAtPiece`

**Fields deleted (dead code):**
- `springForceX`, `springForceY`, `springConstant`, `damping` — never applied anywhere

**Methods moved here:**
- `fun update(viewWidth: Int, viewHeight: Int)`
- `fun resetGame(viewWidth: Int, viewHeight: Int)`
- `fun onTouchDown(x, y, viewWidth, viewHeight): Boolean`
- `fun onTouchMove(dx, dy, viewWidth, viewHeight)`
- `fun onTouchUp()`
- `fun rotatePiece(shape, rotation): List<List<Int>>`
- `fun keepPiecesInsideWalls(viewWidth: Int)`
- `fun checkCollisions(viewWidth: Int, viewHeight: Int)`
- `fun turnPieceRigid(viewWidth: Int, viewHeight: Int)` — calls `onPieceLocked()` for sound
- `fun checkLines()`
- `fun isPieceAtBottom(viewHeight: Int): Boolean`
- `fun isPieceCollidingWithAnotherPiece(cellWidth, cellHeight): Boolean`
- `fun collideAtBottom(cellWidth, cellHeight)` — **keep**: called from `onTouchMove` (line 319)

**Methods deleted (dead code):**
- `collideWithAnotherPiece(cellWidth, cellHeight)` — confirmed: never called, only its check twin is

---

### `FluidTetrisView.kt` (slimmed to ~160 lines)

Retains only:
- `paint`, `backgroundBitmap`
- `engine: GameEngine`, `soundManager: SoundManager`
- `handler`, `updateRunnable` — game loop timer
- `override fun onDraw(canvas)` — reads `engine` state, draws; no logic
- `override fun onTouchEvent(event)` — computes dx/dy, routes to `engine.onTouchDown/Move/Up`; button taps call `engine.resetGame(width, height)` and `engine.isPaused = !engine.isPaused`
- `override fun onWindowFocusChanged` — delegates to engine fields
- `override fun onSizeChanged` — calls `engine.resetGame(w, h)` here instead of `init` (fixes the pre-existing `width=0` bug)

---

## Refactoring Sequence

Each step keeps `./gradlew test` green and `assembleDebug` buildable.

1. **Extract `GameConstants.kt`** — move all constants and piece data; update `FluidTetrisView` to reference `GameConstants.*`. Run tests.

2. **Extract `GameMath.kt`** — cut-paste four bottom-of-file pure functions; delete them from `FluidTetrisView.kt`. Run `./gradlew test` — this is the critical test-contract checkpoint.

3. **Extract `SoundManager.kt`** — replace `mediaPlayer`/`rigidSoundPlayer` fields with a single `soundManager` instance. Build + smoke test.

4. **Create `GameEngine.kt` skeleton** — move all state fields; `FluidTetrisView` still holds all logic methods for now. Build check.

5. **Migrate logic to `GameEngine` in safe order:**
   - `rotatePiece` (no side effects)
   - `checkLines`, `isPieceAtBottom`, `isPieceCollidingWithAnotherPiece`
   - `keepPiecesInsideWalls`
   - `collideAtBottom`, `turnPieceRigid`
   - `checkCollisions`, `update`
   - `resetGame`
   - Touch input logic (`onTouchDown/Move/Up`)
   - Delete `collideWithAnotherPiece` and spring fields during this step

6. **Thin down `FluidTetrisView`** — replace `resetGame()` call in `init` with `onSizeChanged` hook; remove all residual logic.

Run `./gradlew test && ./gradlew assembleDebug` after each sub-step.

---

## Key Risks

| Risk | Mitigation |
|---|---|
| `width`/`height` used inside logic methods | Pass `viewWidth`/`viewHeight` as params to every engine method that needs dimensions |
| `resetGame` called in `init` when `width == 0` | Move to `onSizeChanged` — also fixes a pre-existing bug |
| `collideAtBottom` looks like dead code but is called line 319 | Confirmed live; keep it; rename to `lockPieceAtBottom` for clarity |
| Tests break if pure functions are wrapped in a class | Keep them as top-level `internal fun` declarations in `GameMath.kt` |
| Grid boundary constants duplicated in `onDraw` and logic | Both sides reference `GameConstants.GRID_LEFT` etc. after Step 1 |

---

## Verification

```bash
# After every step
.\gradlew test

# After final step
.\gradlew assembleDebug
.\gradlew installDebug   # manual smoke test: game starts, piece falls, drag works, lock works, score increments
```

Manual checks:
- Piece spawns and falls
- Touch-drag moves piece; edge-drag rotates it
- Piece locks after 3s contact (bottom and piece-stack)
- Line clear increments score and plays sound
- New Game button resets; Pause button pauses
- App focus loss auto-pauses