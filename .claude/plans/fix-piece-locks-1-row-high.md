# Plan: Fix "piece locks 1 row too high" bug

## Status
- [x] **Fix applied** — `doesPieceCollideWithGridAtY` in `GameEngine.kt` lines 590–599 rewritten to use center-based detection (removed ±50 axis-aligned corner logic, now matches `turnPieceRigid`'s while-loop approach). NOT yet committed.
- [ ] **Regression test** — write and run
- [ ] **Run full test suite** — `./gradlew testDebugUnitTest`
- [ ] **Commit**

## Root Cause
`doesPieceCollideWithGridAtY` used axis-aligned ±50 corners (PIECE_SIZE/2=50). Since cellHeight=82, 50 > cellHeight/2=41, so bottom corners ALWAYS reach into the next row. For rotated pieces (e.g. 45°), the footprint was even more inflated. `moveUpUntilClear()` used this function and spuriously pushed the piece up 1 extra row before `turnPieceRigid()` ran. Only affects the `isWaitingToTurnRigidAtPiece` path (piece-on-piece contact).

## Fix Already Applied
`GameEngine.kt` lines 590–599 — replaced with:
```kotlin
private fun doesPieceCollideWithGridAtY(testY: Float, cellWidth: Float, cellHeight: Float): Boolean {
    for ((bx, by) in rotatedBlockCenters(GameConstants.PIECES[currentPiece], pieceX, testY, pieceRotation)) {
        val gx = ((bx - GameConstants.GRID_LEFT) / cellWidth).toInt()
        val gy = ((by - GameConstants.GRID_TOP) / cellHeight).toInt()
        if (gx in 0 until GameConstants.GRID_COLUMNS && gy in 0 until GameConstants.GRID_ROWS && grid[gy][gx] != null) {
            return true
        }
    }
    return false
}
```

## Regression Test To Write
Add to `app/src/test/java/com/libuy/fluidtris/SnapToGridTest.kt` (or new file):

**Setup:**
- VW=1080, VH=1920
- cellHeight = (1920-100-180)/20 = 82f
- cellWidth = (1080-150-150)/7 ≈ 111.43f
- Pre-fill grid row 17, columns 1–4 (not all 7 — avoids line clear):
  `for (gx in 1..4) { e.grid[17][gx] = 1 }` — use `Int` not `android.graphics.Color`
- L piece (index 3), rotation = 45f
- pieceX = GRID_LEFT + 2*cellWidth ≈ 372.86f
- pieceY: place so bottom block is at row 16, just above row 17.
  - At 45°, lowest block center is at pieceY + 135.36. For it to be in row 16 (center Y=1453): pieceY ≈ 1317f
  - Use pieceY = 1280f so piece falls a few ticks before contact

**Verify:**
- Run 220 ticks (fake clock +16ms/tick)
- Find all non-null cells in grid
- `bottomGy = filled.maxOf { it.second }`
- Assert `bottomGy == 16` — NOT 15 (which was the old buggy result)
- Also assert `filled.size == 4` (piece placed cleanly, no line clear happened)

**Why this catches the bug:**
- Before fix: corner at center_y+50 reached into row 17 → `moveUpUntilClear` pushed piece 1 extra row up → bottommost block lands in row 15
- After fix: center at row 16 → no spurious push → bottommost block correctly in row 16
