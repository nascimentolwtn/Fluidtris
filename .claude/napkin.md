# Napkin Runbook

## v2.4 - Curation Rules
- Re-prioritize on every read.
- Keep recurring, high-value notes only.
- Max 10 items per category.
- Each item includes date + "Do instead".

## Execution & Validation (Highest Priority)
1. **[2026-06-02] Always build with gradlew on Windows**
   Do instead: use `.\gradlew assembleDebug` (backslash, no `./`) in PowerShell; `./gradlew` works in Bash tool.

2. **[2026-06-02] Unit tests need no device; use the right task**
   Do instead: run `./gradlew testDebugUnitTest` for unit tests (no device needed). `./gradlew test` does not resolve with `--tests` flag. Use `connectedAndroidTest` only when emulator is confirmed running.

3. **[2026-06-03] All magic numbers are in `GameConstants` — use them, don't re-hardcode**
   Do instead: reference `GameConstants.PIECE_SIZE`, `GRID_LEFT`, `GRID_RIGHT_MARGIN`, `LOCK_DELAY_MS`, `SNAP_PULL_SPEED`, etc. Never hardcode `100f`, `150f`, `3000L` etc. in logic code.

4. **[2026-06-15] Install always targets all connected devices**
   Do instead: use `./gradlew installDebug` (no `-s` flag). Gradle installs on every authorized device automatically. Confirm output says "Installed on N devices." Never screenshot to verify; write unit tests and let the user test visually.

## Android / Kotlin Gotchas
1. **[2026-06-03] Game is intentionally portrait-only — do not add landscape support**
   Do instead: do not suggest or implement device rotation or landscape orientation. The game stays portrait by design.

2. **[2026-06-03] Use `rotatedBlockCenters()` for all pixel-space checks — collision AND wall clamping**
   Do instead: call `rotatedBlockCenters(shape, pieceX, pieceY, pieceRotation)` (mirrors `canvas.rotate()` transform). Do NOT use `rotatePiece()` + `col * 100f` — wrong for non-axis-aligned rotations. Wall clamping uses `clampPieceXByCenters()`; shape-based `clampPieceX()` is dead production code (kept for its tests only).

3. **[2026-06-03] Architecture: GameEngine + thin FluidTetrisView**
   Do instead: all game state and logic lives in `GameEngine.kt`; `FluidTetrisView.kt` is rendering/input only. Constants in `GameConstants.kt`, pure math in `GameMath.kt`, sound in `SoundManager.kt`. GameEngine has zero Android imports.

4. **[2026-06-04] High score load must be seeded before game loop starts**
   Do instead: always call `engine.highScore = highScoreManager.loadHighScore()` in `FluidTetrisView.init` before `handler.post(updateRunnable)`. Saving is via `onHighScoreBeat`; forgetting the load means score resets on every launch.

5. **[2026-06-15] Timer-dependent GameEngine logic must use `currentTimeMs()`**
   Do instead: `GameEngine.currentTimeMs` is an injectable `() -> Long` (default `System::currentTimeMillis`). Unit tests inject a fake clock and advance it 16 ms per tick. Never add new `System.currentTimeMillis()` calls directly — use `currentTimeMs()`.

## Domain Behavior Guardrails
1. **[2026-06-03] Grid is 7×20; margins: left=150f, right=150f, top=100f, bottom=180f**
   Do instead: derive via `GameConstants.GRID_LEFT/RIGHT_MARGIN/TOP/BOTTOM_MARGIN`; `cellWidth = (w - 300f) / 7`, `cellHeight = (h - 280f) / 20`.

2. **[2026-06-15] Snap animation: target is committed once at contact, timer is guarded**
   Do instead: `beginSnapAnimation()` records `(snapTargetX, snapTargetY, snapTargetRotation)` exactly once when the lock timer starts. `applySnapPull(t)` lerps toward the stored target each frame (linear ramp, X + Y + rotation). The timer is NOT reset by the animation itself moving the piece (even upward). Only user drag to open space cancels the snap (`isDragging && no contact` → `isSnapAnimating = false`).

3. **[2026-06-02] Rotation gesture: top-half drag = clockwise, bottom-half = counter-clockwise**
   Do instead: when adjusting touch input, check which half of the piece bounding box the touch originates from before applying rotation direction.

## Backlog
1. **[2026-06-15] Feature: Level display and level-up sound**
   Do instead: add "Level X" label near the score display (starts at 1). Level increases every 1000 points (level = score / 1000 + 1). When level increases, play a distinct sound (separate from lock/line-clear sounds). Add new sound file `level_up_sound.ogg` to `res/raw/` and wire to `SoundManager.kt` with `playLevelUpSound()` method called in `GameEngine.checkLines()` after updating score.

2. **[2026-06-15] Feature: slide while animating when no collision**
   Do instead: during snap animation (lock countdown), allow piece to slide horizontally if there is no block/wall collision ahead. Currently snap animation is rigid; allow user input to push the piece left/right during the countdown if the target position remains open.

3. **[2026-06-15] Feature: add ads**
   Do instead: integrate ad framework (Google Mobile Ads SDK). Show ads at three points: (a) mid-game banner/interstitial, (b) during pause menu, (c) during game-over screen. Define placement strategy and frequency.

## Done
- **[2026-06-17] Feature: "Next" button to skip ahead** — button positioned below next-piece preview square, green background, labeled "Next". Tapping it randomizes the next-piece preview only; leaves current piece in its current state (position, rotation, collision state) to continue its natural course. Current piece falls freely; player can manage multiple pieces simultaneously. Button disabled when game over. `onNextPieceButton()` method in `GameEngine` generates new random next piece and rotation. Button touch detection in `FluidTetrisView.onTouchEvent()`. Constants `NEXT_BUTTON_WIDTH=160f`, `NEXT_BUTTON_HEIGHT=80f` in `GameConstants`. Unit test suite `NextPieceButtonTest` (9 tests) verifies: current piece preservation, next piece randomization, game-over no-op, score/grid/collision state preservation, preview bounds. All 170+ tests pass.
- **[2026-06-17] Feature: unified level multiplier affects gravity** — `getLevelMultiplier()` returns `(1 + (level - 1) * LEVEL_DIFFICULTY_FACTOR).coerceAtMost(MAX_LEVEL_MULTIPLIER)` where level = score / 1000 + 1. Applies to gravity scaling: pieces fall progressively faster at higher levels. Constants `LEVEL_DIFFICULTY_FACTOR=0.3f` and `MAX_LEVEL_MULTIPLIER=3f` in `GameConstants`. Public `getLevel()` getter. Unit test `GameEngineLevelTest` covers level progression, multiplier scaling, and gravity increase with 4 tests. All 161 tests pass.
- **[2026-06-15] Feature: gravity increases with score (each 1000 points)** — formula: `GRAVITY * (1 + score / 1000 * 0.5)` capped at 3x base gravity. Each 1000 points increases falling speed by 50%, making the game progressively harder. Implemented in `GameEngine.update()` line 60. All tests pass.
- **[2026-06-15] Feature: Exit button on game-over + player name on high score** — Exit button (red) next to New Game on game-over screen. AlertDialog asks for player name only when game ends with new high score. Name pre-selected for easy replacement. Score text turns yellow on new high score. All 130 tests pass. Committed `e11840d`.
- **[2026-06-15] Fix: spurious upward push in moveUpUntilClear** — `doesPieceCollideWithGridAtY` used axis-aligned ±50 corners (too wide vs cellHeight=82), causing unnecessary pushes on piece-on-piece lock path. Replaced with center-based detection. Regression test: L@45° on pre-filled row 17 locks at row 16. 130 tests pass. Committed `1a705df`.
- **[2026-06-15] Feature: snap-to-grid animation during lock countdown** — when the lock timer starts, `beginSnapAnimation()` computes the exact grid-aligned `(X, Y, rotation)` target once. `applySnapPull(t)` lerps the piece there each frame (linear ramp; separate `SNAP_PULL_SPEED=0.12` for X/Y and `SNAP_ROTATION_SPEED=0.28` for rotation). The timer is guarded: transit movement (including upward Y) does not reset it. Dragging to open space cancels the snap and returns the piece to fluid. `lerpAngleDeg()` added to `GameMath.kt` for shortest-arc rotation lerp. `currentTimeMs` injected into `GameEngine` for deterministic unit tests. 125 tests pass.
- **[2026-06-04] Feature: prancy marshmallow bounce physics** — on contact without a clean lock position, the piece bounces with `springForceX` impulse + `BOUNCE_ROTATION_DEG` tilt. After 4 bounces, force-locks as safety net. `bounceCount`, `slideDirection`, `isSlidingOnContact` track state. Tagged `gameplay-v1-bounce-physics`.
- **[2026-06-04] Fix: piece overwrites another piece when locked with rotation** — two bugs: (1) `doesPieceCollideWithGridAtY` ignored actual `by` from `rotatedBlockCenters`, using `testY+50f` for all blocks; (2) `turnPieceRigid` snaps rotation before writing to grid but never re-checked for overlap in the snapped shape. Fix: corrected `doesPieceCollideWithGridAtY` to use real `by`, added post-snap overlap-clearing loop in `turnPieceRigid` using the same coord formula as the grid write. 1 regression test added. 86 tests pass.
- **[2026-06-04] Feature: Spring physics wiring** — `springForceX` added to `GameEngine`; set from horizontal drag delta × `SPRING_CARRY=0.5`, decays each frame × `SPRING_DAMPING=0.80`. Piece slides horizontally after drag release; zeroed on touch-down, lock-timer start, and piece spawn. Build clean.
- **[2026-06-04] Feature: tiered line-clear scoring** — 1 line = 100pts, 2 lines = 300pts, 3 lines = 500pts, 4 lines = 800pts. Changed `checkLines()` in `GameEngine.kt` from `score += linesCleared * 100` to `when` statement with tiered bonuses. Updated 2 unit tests and added 2 new tests (`threeFullRows_score500`, `fourFullRows_score800`). All 85 tests pass.
- **[2026-06-04] Polish: fluid pieces 4px smaller with centered draw offset** — `fluidBlockSize = PIECE_SIZE - (1f - solidity) * 4f`; draw offset keeps visual center at rotation pivot; corner radius grows proportionally, giving rounder look when fluid. No logic/collision changes.
- **[2026-06-04] Fix: HighScore not persisting across restarts** — `engine.highScore` was never seeded from SharedPreferences on startup. Added `engine.highScore = highScoreManager.loadHighScore()` in `FluidTetrisView.init`. Save path via `onHighScoreBeat` was already correct.
- **[2026-06-03] Test campaign Phase 2 — GameEngine tests** — `GameEngineLineTest` (6), `GameEngineCollisionTest` (8), `GameEngineLockTest` (6), `GameEngineStateTest` (9). Bug found and fixed: `checkLines()` `for` loop skipped consecutive full rows; replaced with `while` that re-checks same index after each clear. All 85 unit tests pass.
- **[2026-06-03] Refactor: God Object split** — `FluidTetrisView.kt` (750+ lines) split into `GameConstants.kt`, `GameMath.kt`, `SoundManager.kt`, `GameEngine.kt`, thin `FluidTetrisView.kt`. Dead code (`collideWithAnotherPiece`, spring fields) deleted. `assembleDebug` clean.
- **[2026-06-03] Fix: rotated pieces can't reach left wall** — switched `keepPiecesInsideWalls()` to `clampPieceXByCenters()` with `rotatedBlockCenters()`. 4 regression tests added to `WallClampTest.kt`.
