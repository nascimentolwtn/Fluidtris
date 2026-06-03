# Plan: Fix Rotation Wall-Check Bug (TDD)

## Context

`keepPiecesInsideWalls()` clamps `pieceX` to keep the active piece inside the left/right walls.
It uses `pieces[currentPiece]` — the **raw, un-rotated** shape — so when a piece is visually
rotated via `canvas.rotate(pieceRotation, ...)`, the wall clamp uses the wrong column footprint.

`rotatePiece()` only returns a transformed shape for exact 90/180/270° values; all other angles
fall through to `else -> shape` (un-rotated). So the wall check always uses the wrong shape
while the piece is in motion.

## TDD Steps

### Red: test first
Create `WallClampTest.kt` (unit test, JVM-only) that calls `clampPieceX(...)` — a function
that doesn't exist yet → compile failure = red.

Key assertion: a 1-wide rotated shape clamps at `rightWall - pieceSize`, NOT at
`rightWall - 4*pieceSize` (the wrong result that would come from using the un-rotated 4-wide shape).

### Green: extract + implement
Add `internal fun clampPieceX(shape, pieceX, leftWall, rightWall, pieceSize): Float` as a
**top-level function** in `FluidTetrisView.kt` (accessible to unit tests in the same module).

Correct algorithm (replaces the buggy iterative formula in the existing code):
- Find `minCol` / `maxCol` of occupied cells in `shape`
- Left clamp: `if x + minCol*pieceSize < leftWall → x = leftWall - minCol*pieceSize`
- Right clamp: `if x + maxCol*pieceSize + pieceSize > rightWall → x = rightWall - (maxCol+1)*pieceSize`

### Wire-up: fix the rotation bug
Replace `keepPiecesInsideWalls()` body with:
```kotlin
val shape = rotatePiece(pieces[currentPiece], pieceRotation)
pieceX = clampPieceX(shape, pieceX, 50f, width - 50f, 100f)
```

## Files

- **New**: `app/src/test/java/com/libuy/fluidtris/WallClampTest.kt`
- **Modified**: `app/src/main/java/com/libuy/fluidtris/FluidTetrisView.kt`
  - Add `internal fun clampPieceX(...)` as top-level (before or after the class)
  - Rewrite `keepPiecesInsideWalls()` body (~26 lines → 2 lines)

## Verification

```bash
./gradlew test   # WallClampTest passes; ExampleUnitTest still passes
```

Manual: install APK, rotate I-piece visually, drag to right wall — piece stops flush instead of clipping through.
