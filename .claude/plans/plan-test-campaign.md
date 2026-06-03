# Test Campaign: Fluidtris

## Context

The game has 4 pure functions already tested. Comprehensive coverage requires extracting `GameEngine` first (see `plan-a-refactoring-on-nifty-avalanche.md`) because every other logic method is `private` on an Android `View` class — not reachable by JUnit unit tests. This campaign is split into two phases to reflect that dependency.

**Success criteria:**
- Phase 1: `.\gradlew test` passes with RotatePieceTest added.
- Phase 2: `.\gradlew test` passes with all GameEngine tests added; every public method on `GameEngine` has at least one test covering the happy path and one covering an edge case.

---

## Phase 1 — Before Refactoring (do now)

### Prerequisite: Fix duplicate `rotatePiece`

`rotatePiece` is a file-level `internal fun` defined twice in `FluidTetrisView.kt` (lines 702–715 and 720–733, identical bodies). Delete the second copy (lines 717–733 including its comment). The Kotlin compiler accepts this silently but it is a latent confusion point.

### New file: `RotatePieceTest.kt`

Location: `app/src/test/java/com/libuy/fluidtris/RotatePieceTest.kt`

Tests the `rotatePiece(shape, rotation)` file-level function. All assertions use `assertEquals` on `List<List<Int>>`.

**I-piece tests** (shape `[[1,1,1,1]]`, symmetric under 180°):
- `ipiece_0deg_unchanged` — `rotatePiece(I, 0f)` returns `[[1,1,1,1]]`
- `ipiece_90deg_isVertical` — returns `[[1],[1],[1],[1]]`
- `ipiece_180deg_unchanged` — returns `[[1,1,1,1]]` (symmetric)
- `ipiece_270deg_isVertical` — returns `[[1],[1],[1],[1]]` (symmetric with 90°)
- `ipiece_360deg_isIdentity` — `rotatePiece(I, 360f)` returns `[[1,1,1,1]]`

**T-piece tests** (shape `[[1,1,1],[0,1,0]]`):
- `tpiece_0deg_unchanged` — returns `[[1,1,1],[0,1,0]]`
- `tpiece_90deg` — returns `[[0,1],[1,1],[0,1]]`
- `tpiece_180deg` — returns `[[0,1,0],[1,1,1]]`
- `tpiece_270deg` — returns `[[1,0],[1,1],[1,0]]`
- `tpiece_composedRotations` — `rotatePiece(rotatePiece(T, 90f), 90f)` equals `rotatePiece(T, 180f)`

**O-piece tests** (shape `[[1,1],[1,1]]`):
- `opiece_allRotationsIdentical` — result at 0°/90°/180°/270° is always `[[1,1],[1,1]]`

---

## Phase 2 — After GameEngine Extracted (Step 4–5 of refactoring plan)

Each test class constructs a `GameEngine(onPieceLocked = {})` directly with no Android context needed. Dimensions are passed as parameters; use constants `viewWidth = 1080`, `viewHeight = 1920` (portrait phone) throughout.

Computed helpers used in every test:
```kotlin
private val VW = 1080; private val VH = 1920
private val cellW = (VW - 300f) / 7
private val cellH = (VH - 280f) / 20
private val gridLeft = 150f; private val gridTop = 100f
```

### `GameEngineLineTest.kt`

Tests `checkLines()` and the score side-effect.

- `emptyGrid_noLinesCleared` — brand-new engine, call `checkLines()`, score stays 0, grid all null
- `partialRow_notCleared` — fill 6 of 7 cells in row 19, `checkLines()`, score 0, row intact
- `singleFullRow_cleared` — fill all 7 cells of row 19, call `checkLines()`, row 19 is all null, score = 100
- `singleFullRow_rowAboveShiftsDown` — row 18 has one block, row 19 is full; after `checkLines()`, the block from row 18 appears in row 19
- `twoFullRows_score200` — rows 18 and 19 full, call `checkLines()`, score = 200, both rows cleared
- `clearedRows_shiftDownCorrectly` — row 17 has a marker block, rows 18–19 full; after clear, marker is in row 19

### `GameEngineCollisionTest.kt`

Tests `isPieceAtBottom()` and `isPieceCollidingWithAnotherPiece()`.

**isPieceAtBottom:**
- `pieceWellAboveBottom_false` — place I-piece at pieceY = 100f, assert false
- `pieceJustAboveBottom_false` — pieceY such that bottom of last block is 1px above gridBottom
- `pieceTouchingBottom_true` — pieceY such that blockY + 100f > gridBottom
- `rotatedPieceTouchingBottom_true` — vertical I-piece at appropriate Y

**isPieceCollidingWithAnotherPiece:**
- `emptyGrid_false`
- `blockAtExactPieceCell_true` — set `grid[row][col] = Color.RED` at the cell the piece occupies
- `blockAdjacentToPiece_false` — block one cell to the right, no overlap
- `rotatedPieceOverBlock_true` — rotate piece 90°, place block under its new footprint

### `GameEngineLockTest.kt`

Tests `lockPieceAtBottom()` (renamed from `collideAtBottom`) and `turnPieceRigid()`.

- `lockAtBottom_blocksWrittenToGrid` — call `lockPieceAtBottom(cellW, cellH)`, verify grid cells at expected positions are non-null
- `lockAtBottom_callsOnPieceLocked` — use a flag lambda, assert it fires
- `turnPieceRigid_nextPieceSpawned` — `currentPiece` index advances to `nextPiece` after call
- `turnPieceRigid_pieceResetToTop` — `pieceY` ≈ 100f after call
- `turnPieceRigid_gameOverWhenTopRowOccupied` — fill grid row 0 with non-null, call `turnPieceRigid`, assert `isGameOver == true`
- `turnPieceRigid_rotationSnapsto90` — set `pieceRotation = 47f`, call `turnPieceRigid`, pieces in grid match 0° canonical shape

### `GameEngineStateTest.kt`

Tests `resetGame()` and `update()`.

**resetGame:**
- `afterReset_gridAllNull`
- `afterReset_scoreZero`
- `afterReset_notPaused`
- `afterReset_notGameOver`
- `afterReset_pieceYAtTop` — `pieceY` ≈ 100f

**update (gravity):**
- `whenPaused_pieceYUnchanged` — set `isPaused = true`, call `update(VW, VH)`, `pieceY` unchanged
- `whenGameOver_pieceYUnchanged` — set `isGameOver = true`, same check
- `whenDragging_pieceYUnchanged` — set `isDragging = true`, same check
- `normalUpdate_pieceYIncreasesByGravity` — none of the above, `update()` once, `pieceY` increases by `GRAVITY`

---

## Verification

```bash
# Phase 1 (run now)
.\gradlew test

# Phase 2 (run after each new test class is added)
.\gradlew test
```

All tests must be in `package com.libuy.fluidtris` and use JUnit 4. No Mockito or Robolectric — `GameEngine` has zero Android imports, so no mocking framework is needed.
