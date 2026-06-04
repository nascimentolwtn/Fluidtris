# Napkin Runbook

## Curation Rules
- Re-prioritize on every read.
- Keep recurring, high-value notes only.
- Max 10 items per category.
- Each item includes date + "Do instead".

## Execution & Validation (Highest Priority)
1. **[2026-06-02] Always build with gradlew on Windows**
   Do instead: use `.\gradlew assembleDebug` (backslash, no `./`) in PowerShell; `./gradlew` works in Bash tool.

2. **[2026-06-02] Test requires a connected device/emulator for instrumented tests**
   Do instead: run `.\gradlew test` for unit tests (no device needed); use `connectedAndroidTest` only when emulator is confirmed running.

3. **[2026-06-03] All magic numbers are in `GameConstants` — use them, don't re-hardcode**
   Do instead: reference `GameConstants.PIECE_SIZE`, `GRID_LEFT`, `GRID_RIGHT_MARGIN`, `LOCK_DELAY_MS`, etc. Never hardcode `100f`, `150f`, `3000L` etc. in logic code.

## Android / Kotlin Gotchas
1. **[2026-06-03] Game is intentionally portrait-only — do not add landscape support**
   Do instead: do not suggest or implement device rotation or landscape orientation. The game stays portrait by design.

2. **[2026-06-03] Use `rotatedBlockCenters()` for all pixel-space checks — collision AND wall clamping**
   Do instead: call `rotatedBlockCenters(shape, pieceX, pieceY, pieceRotation)` (mirrors `canvas.rotate()` transform). Do NOT use `rotatePiece()` + `col * 100f` — wrong for non-axis-aligned rotations. Wall clamping uses `clampPieceXByCenters()`; shape-based `clampPieceX()` is dead production code (kept for its tests only).

3. **[2026-06-03] Architecture: GameEngine + thin FluidTetrisView**
   Do instead: all game state and logic lives in `GameEngine.kt`; `FluidTetrisView.kt` is rendering/input only. Constants in `GameConstants.kt`, pure math in `GameMath.kt`, sound in `SoundManager.kt`. GameEngine has zero Android imports.

4. **[2026-06-04] High score load must be seeded before game loop starts**
   Do instead: always call `engine.highScore = highScoreManager.loadHighScore()` in `FluidTetrisView.init` before `handler.post(updateRunnable)`. Saving is via `onHighScoreBeat`; forgetting the load means score resets on every launch.

## Domain Behavior Guardrails
1. **[2026-06-03] Grid is 7×20; margins: left=150f, right=150f, top=100f, bottom=180f**
   Do instead: derive via `GameConstants.GRID_LEFT/RIGHT_MARGIN/TOP/BOTTOM_MARGIN`; `cellWidth = (w - 300f) / 7`, `cellHeight = (h - 280f) / 20`.

2. **[2026-06-02] Piece lock timer is 3 seconds of continuous contact**
   Do instead: `bottomCollisionTime` / `pieceCollisionTime` are `System.currentTimeMillis()` timestamps; lock triggers when delta ≥ 3000 ms. Don't confuse them with frame counters.

3. **[2026-06-02] Rotation gesture: top-half drag = clockwise, bottom-half = counter-clockwise**
   Do instead: when adjusting touch input, check which half of the piece bounding box the touch originates from before applying rotation direction.

## Backlog
1. **[feature] Ask player name when hitting new highscore** — show Android `AlertDialog` with `EditText` when `onHighScoreBeat` fires in FluidTetrisView; save name alongside score in SharedPreferences via new `HighScoreManager.saveHighScoreName()` method.
2. **[feature] Spring physics wiring** — deleted in refactor as dead code; re-add to `GameEngine` fields if spring feel is desired.

## Done
- **[2026-06-04] Fix: piece overwrites another piece when locked with rotation** — `lockPieceAtBottom()` was writing unrotated shape to grid, then `turnPieceRigid()` wrote rotated shape, causing overwrites. Removed redundant grid-writing from `lockPieceAtBottom()` to let `turnPieceRigid()` be sole lock authority. All 85 unit tests pass.
- **[2026-06-04] Polish: fluid pieces 4px smaller with centered draw offset** — `fluidBlockSize = PIECE_SIZE - (1f - solidity) * 4f`; draw offset keeps visual center at rotation pivot; corner radius grows proportionally, giving rounder look when fluid. No logic/collision changes.
- **[2026-06-04] Fix: HighScore not persisting across restarts** — `engine.highScore` was never seeded from SharedPreferences on startup. Added `engine.highScore = highScoreManager.loadHighScore()` in `FluidTetrisView.init`. Save path via `onHighScoreBeat` was already correct.
- **[2026-06-03] Test campaign Phase 2 — GameEngine tests** — `GameEngineLineTest` (6), `GameEngineCollisionTest` (8), `GameEngineLockTest` (6), `GameEngineStateTest` (9). Bug found and fixed: `checkLines()` `for` loop skipped consecutive full rows; replaced with `while` that re-checks same index after each clear. All 85 unit tests pass.
- **[2026-06-03] Refactor: God Object split** — `FluidTetrisView.kt` (750+ lines) split into `GameConstants.kt`, `GameMath.kt`, `SoundManager.kt`, `GameEngine.kt`, thin `FluidTetrisView.kt`. Dead code (`collideWithAnotherPiece`, spring fields) deleted. `assembleDebug` clean.
- **[2026-06-03] Fix: rotated pieces can't reach left wall** — switched `keepPiecesInsideWalls()` to `clampPieceXByCenters()` with `rotatedBlockCenters()`. 4 regression tests added to `WallClampTest.kt`.
- **[2026-06-03] Test campaign Phase 1** — `RotatePieceTest.kt` added (11 tests for `rotatePiece` on I/T/O pieces).