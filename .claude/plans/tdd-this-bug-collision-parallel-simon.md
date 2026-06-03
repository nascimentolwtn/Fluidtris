# Plan: TDD Fix — Solidification Crash (rotatePiece for Non-Square Pieces)

## Context

When a piece solidifies (timer fires → `turnPieceRigid()` runs), the game crashes and Android kills the app. The user can reproduce it by rotating any non-O piece to ~90° or ~270° and letting it lock in.

**Observable symptoms**:
1. **App crashes on solidification**: When a rotated piece locks in (after 3 seconds of contact), the app crashes with `ArrayIndexOutOfBoundsException` and Android closes it.
2. **Pieces solidify in unrotated form**: When a piece is rotabotted visually and then locked, it solidifies in its initial unrotated orientation rather than the rotated form the player sees.

**Root cause**: `rotatePiece()` has an incorrect matrix-transpose algorithm that causes `ArrayIndexOutOfBoundsException` for non-square shapes at exactly 90° and 270°. Every piece except O (2×2) is non-square: I (1×4), T/L/J/S/Z (2×3). The crash is exposed at solidification because `turnPieceRigid()` snaps `pieceRotation` to the nearest exact 90° multiple *before* calling `rotatePiece()`.

**90° bug (line 466)**:
```kotlin
shape.mapIndexed { rowIndex, row ->
    row.mapIndexed { colIndex, _ -> shape[shape.size - 1 - colIndex][rowIndex] }
}
```
For I-piece (1 row × 4 cols), `colIndex` iterates 0–3 while `shape.size = 1`, so indices wrap: 1, 0, -1, -2. Negative list access → crash.

**270° bug (line 470)**:
```kotlin
shape.mapIndexed { rowIndex, row ->
    row.mapIndexed { colIndex, _ -> shape[colIndex][shape.size - 1 - rowIndex] }
}
```
`colIndex` iterates 0–3 but `shape` only has 1 row → `shape[1]`, `shape[2]`, `shape[3]` → crash.

**Secondary risk — double solidification**: Both `isWaitingToTurnRigidAtBottom` and `isWaitingToTurnRigidAtPiece` can fire in the same frame, calling `turnPieceRigid()` twice and corrupting game state.

---

## Implementation Plan

### Step 1 — Expose `rotatePiece` as package-level `internal fun`

Move `rotatePiece(shape, rotation)` from the class body (line 463) to the package-level section at the bottom of `FluidTetrisView.kt` (after existing functions like `clampPieceX`, `hitCellFromTouch`, etc.). Update three call sites to use the package-level function.

### Step 2 — Write failing tests in `RotationTest.kt`

New file: `app/src/test/java/com/libuy/fluidtris/RotationTest.kt`

Tests to verify:
- I-piece 90°/270°: produces 4×1 shape (currently crashes)
- T-piece 90°/270°: produces 3×2 shape (currently crashes)
- O-piece: unchanged at all rotations (regression guard)
- Four 90° rotations = identity

### Step 3 — Fix `rotatePiece()` (correct non-square matrix rotation)

Replace 90° and 270° branches with correct algorithm:

```kotlin
internal fun rotatePiece(shape: List<List<Int>>, rotation: Float): List<List<Int>> {
    val rows = shape.size
    val cols = if (rows > 0) shape[0].size else 0
    return when (rotation.toInt() % 360) {
        90 -> List(cols) { newRow ->
            List(rows) { newCol -> shape[rows - 1 - newCol][newRow] }
        }
        180 -> shape.map { it.reversed() }.reversed()
        270 -> List(cols) { newRow ->
            List(rows) { newCol -> shape[newCol][cols - 1 - newRow] }
        }
        else -> shape
    }
}
```

### Step 4 — Guard double solidification in `update()`

Add `didSolidify` flag to prevent second `turnPieceRigid()` call in same frame.

---

## Verification

1. Tests pass: `./gradlew test` 
2. Build succeeds: `.\gradlew assembleDebug`
3. On emulator: rotate piece to 90°, let it lock → no crash, solidifies in rotated form
4. Rotate to 270°, drag onto existing piece → no crash