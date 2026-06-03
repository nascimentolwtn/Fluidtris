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
1. **[2026-06-02] Rotation collision detection uses un-rotated shape**
   Do instead: `keepPiecesInsideWalls` and wall-bound checks must call `rotatePiece()` to get the actual rotated cell offsets before clamping — the current code silently uses the wrong shape.

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
2. **[bug] Rotation wall-check uses un-rotated shape** — `keepPiecesInsideWalls` doesn't account for rotated piece shape; fix requires calling `rotatePiece()` before clamping.
3. **[bug] High score resets on app close** — `highScore` is in-memory only; persist via `SharedPreferences`.
4. **[cleanup] `collideWithAnotherPiece()` is dead code** — audit timer logic in `checkCollisions()` before deciding to wire it up or delete it.
5. **[feature] Spring physics wiring** — `springForceX/Y`, `springConstant`, `damping` are scaffolded but never applied; connect to movement/rotation if spring feel is desired.