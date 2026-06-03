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

3. **[2026-06-02] Piece block size is hardcoded at 100f px — not derived from cellWidth/cellHeight**
   Do instead: when editing rendering or collision code, treat block size as `100f` constant, not grid-cell size.

## Android / Kotlin Gotchas
1. **[2026-06-03] Game is intentionally portrait-only — do not add landscape support**
   Do instead: do not suggest or implement device rotation or landscape orientation. The game stays portrait by design.

2. **[2026-06-03] Use `rotatedBlockCenters()` for all pixel-space checks — collision AND wall clamping**
   Do instead: call `rotatedBlockCenters(shape, pieceX, pieceY, pieceRotation)` (mirrors `canvas.rotate()` transform). Do NOT use `rotatePiece()` + `col * 100f` — wrong for non-axis-aligned rotations. Wall clamping uses `clampPieceXByCenters()`; shape-based `clampPieceX()` is dead production code (kept for its tests only).

2. **[2026-06-02] `collideWithAnotherPiece()` is dead code**
   Do instead: piece-to-piece collision locking flows through the timer path in `checkCollisions()`. Do not add calls to `collideWithAnotherPiece()` without auditing the timer logic first.

3. **[2026-06-02] Spring physics fields are scaffolded but disconnected**
   Do instead: `springForceX/Y`, `springConstant`, `damping` are set but never applied to movement. Touch any of these fields only if intentionally wiring up spring physics.

4. **[2026-06-02] High score resets on app close — no persistence**
   Do instead: to persist high score, write to `SharedPreferences`; the `highScore` field is in-memory only.

## Domain Behavior Guardrails
1. **[2026-06-02] Grid is 7×20; pixel layout uses fixed margins (100f left/right, 250f top/bottom)**
   Do instead: derive pixel positions via `cellWidth = (width - 100f) / 7` and `cellHeight = (height - 250f) / 20` — don't hardcode arbitrary offsets.

2. **[2026-06-02] Piece lock timer is 3 seconds of continuous contact**
   Do instead: `bottomCollisionTime` / `pieceCollisionTime` are `System.currentTimeMillis()` timestamps; lock triggers when delta ≥ 3000 ms. Don't confuse them with frame counters.

3. **[2026-06-02] Rotation gesture: top-half drag = clockwise, bottom-half = counter-clockwise**
   Do instead: when adjusting touch input, check which half of the piece bounding box the touch originates from before applying rotation direction.

## Backlog
1. **[refactor] God Object split — Thin View + Game Engine** — plan at `.claude/plans/plan-a-refactoring-on-nifty-avalanche.md`; splits `FluidTetrisView.kt` (750+ lines) into `GameConstants`, `GameMath`, `SoundManager`, `GameEngine`, thin `FluidTetrisView`. **Required to unblock test campaign Phase 2.**
2. **[test] Test campaign Phase 2 — GameEngine tests** — plan at `.claude/plans/plan-test-campaign.md`; blocked on refactor above. Once `GameEngine` is extracted, add `GameEngineLineTest`, `GameEngineCollisionTest`, `GameEngineLockTest`, `GameEngineStateTest`.
3. **[feature] Make sound button icon-only** — replace "Sound: ON/OFF" text with speaker icon (🔊/🔇) to save space and match UI polish.
4. **[bug] High score resets on app close** — `highScore` is in-memory only; persist via `SharedPreferences`.
5. **[cleanup] `collideWithAnotherPiece()` is dead code** — audit timer logic in `checkCollisions()` before deciding to wire it up or delete it.
6. **[feature] Spring physics wiring** — `springForceX/Y`, `springConstant`, `damping` are scaffolded but never applied; connect to movement/rotation if spring feel is desired.

## Done
- **[2026-06-03] Fix: rotated pieces can't reach left wall** — `keepPiecesInsideWalls()` was using logical `rotatePiece()` shape for clamping, but rendering uses `canvas.rotate()` around the original bounding box center. Fixed by switching to `clampPieceXByCenters()` with `rotatedBlockCenters()`. 4 regression tests added to `WallClampTest.kt`. Total 28 unit tests pass.
- **[2026-06-03] Test campaign Phase 1** — `RotatePieceTest.kt` added (11 tests for `rotatePiece` on I/T/O pieces, full shape equality); duplicate `rotatePiece` definition was already removed. All 24 unit tests pass.