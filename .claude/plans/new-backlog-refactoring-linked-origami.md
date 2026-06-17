# Plan: Add 8th Column by Reclaiming Half-Margin Each Side

## Context
The Fluidtris grid is 7×20 with equal 150px margins on left and right. By trimming each margin
to 100px (reclaiming ~50px per side = ~1 column-width), we get enough horizontal space for an
8th column at almost the same cell size. This increases playfield width without changing visible
proportions.

**Math check (at VW=1080):**
- Old: `cellWidth = (1080 - 300) / 7 = 111.4px`
- New: `cellWidth = (1080 - 200) / 8 = 110.0px`  
- Change: < 1.3% smaller per cell — imperceptible.

**Spawn alignment issue:** With GRID_LEFT=100 and cellWidth=110, the default spawn
`(viewWidth/2) - 50 = 490` places block 0 at bx=540, which is exactly on the cell-4/5 boundary
(`100 + 4*110 = 540`). Two blocks would share column 4. Fix: shift spawn by 50px left
(`(viewWidth/2) - 100 = 440`), placing blocks at cols 3, 4, 5, 6 — matching existing test
expectations.

---

## Changes

### 1. `app/src/main/java/com/libuy/fluidtris/GameConstants.kt`

```kotlin
const val GRID_COLUMNS = 8   // was 7
const val GRID_LEFT = 100f   // was 150f  (reclaim half-column on left)
const val GRID_RIGHT_MARGIN = 100f  // was 150f  (reclaim half-column on right)
```

### 2. `app/src/main/java/com/libuy/fluidtris/GameEngine.kt`

Three spawn points — all change `(viewWidth / 2) - 50f` → `(viewWidth / 2) - 100f`:

- Line 171 (`resetGame`)
- Line 303 (`onNextPieceButton`)
- Line 376 (`spawnNextPiece`)

Why `-100f`: shifts block 0 center from 540 (cell boundary) to 490 (cell 3.55 → col 3),
keeping all 4 blocks in distinct columns 3–6 as tests expect.

---

## Files NOT requiring changes

- `FluidTetrisView.kt` — derives `cellWidth` from constants dynamically
- `GameMath.kt` — pure math, no grid constants
- All test files — tests derive `cellW` from constants; column assertions still hold at
  the new spawn position 440 (cols 3,4,5,6 identical to before)

**Verified column mappings** at pieceX=440, GRID_LEFT=100, cellWidth=110:
- 0° I-piece: bx=490,590,690,790 → cols 3,4,5,6 ✓ (`GameEngineLockTest` assertions unchanged)
- 90° I-piece: bx=640 → col 4 ✓ (`turnPieceRigid_rotationSnaps` unchanged)
- `SnapToGridTest`: uses `PIECE_X = GRID_LEFT + 2*cellWidth` which auto-updates to 320;
  at cellWidth=110, PIECE_SIZE=100 < cellWidth=110 so adjacent blocks still map to distinct cols ✓

---

## Napkin update

Update in `.claude/napkin.md` under **Domain Behavior Guardrails**:

> Grid is 7×20; margins: left=150f, right=150f …  
> cellWidth = (w - 300f) / 7

→

> Grid is 8×20; margins: left=100f, right=100f, top=100f, bottom=180f  
> cellWidth = (w - 200f) / 8

---

## Verification

```bash
./gradlew testDebugUnitTest
```

All 175+ tests should pass with no modifications. Install on device to confirm the extra
column renders, pieces span 8 columns, and wall-clamping holds on both sides.
