package com.libuy.fluidtris

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Snap-to-grid tests: all 7 pieces × 4 canonical rotations.
// Each test places the piece just above the floor, runs 220 ticks (LOCK_DELAY_MS ≈ 188),
// then verifies the piece locked and wrote exactly 4 distinct, in-bounds grid cells.
//
// pieceX = GRID_LEFT + 2*cellWidth ≈ 372.86 — chosen so no two blocks map to the same
// (gx, gy) pair for any piece at any of the four canonical rotations.
// (PIECE_SIZE=100 < cellWidth=111.43, so horizontal neighbours always land in adjacent cols;
//  PIECE_SIZE=100 > cellHeight=82, so vertical neighbours always land in different rows.)
class SnapToGridTest {

    private val VW = 1080
    private val VH = 1920
    private val cellWidth = (VW - GameConstants.GRID_LEFT - GameConstants.GRID_RIGHT_MARGIN) / GameConstants.GRID_COLUMNS
    // ≈ 372.86 — keeps all pieces well inside the grid for every rotation
    private val PIECE_X = GameConstants.GRID_LEFT + 2f * cellWidth

    // Returns the pieceY that leaves a 3 px gap above first floor contact.
    // Uses pieceX=0 because `by` from rotatedBlockCenters is independent of pieceX.
    private fun floorY(pieceIndex: Int, rotation: Float): Float {
        val shape = GameConstants.PIECES[pieceIndex]
        val maxByAtZero = rotatedBlockCenters(shape, 0f, 0f, rotation).maxOf { it.second }
        val gridBottom = VH.toFloat() - GameConstants.GRID_BOTTOM_MARGIN
        val targetBottomCenter = gridBottom - GameConstants.PIECE_SIZE / 2f - 3f
        return targetBottomCenter - maxByAtZero
    }

    private fun assertSnapsAndLocksCleanly(pieceIndex: Int, rotation: Float) {
        var locked = false
        var fakeTime = 0L
        val e = GameEngine(onPieceLocked = { locked = true }, onLineCleared = {})
        e.currentTimeMs = { fakeTime }
        e.resetGame(VW, VH)
        e.currentPiece = pieceIndex
        e.pieceRotation = rotation
        e.pieceX = PIECE_X
        e.pieceY = floorY(pieceIndex, rotation)

        repeat(220) { fakeTime += 16L; e.update(VW, VH) }

        val name = listOf("I", "O", "T", "L", "J", "S", "Z")[pieceIndex]
        val label = "$name@${rotation.toInt()}°"

        assertTrue("$label should lock after timer", locked)

        val filled = mutableSetOf<Pair<Int, Int>>()
        for (gy in 0 until GameConstants.GRID_ROWS)
            for (gx in 0 until GameConstants.GRID_COLUMNS)
                if (e.grid[gy][gx] != null) filled.add(gx to gy)

        assertEquals("$label: expected 4 distinct grid cells, got $filled", 4, filled.size)

        for ((gx, gy) in filled) {
            assertTrue("$label: gx=$gx out of range", gx in 0 until GameConstants.GRID_COLUMNS)
            assertTrue("$label: gy=$gy out of range", gy in 0 until GameConstants.GRID_ROWS)
        }

        val bottomGy = filled.maxOf { it.second }
        assertTrue("$label: bottommost cell gy=$bottomGy should be >= 18", bottomGy >= 18)
    }

    // ── I piece (index 0) ──────────────────────────────────────────────────
    @Test fun i_0deg()   = assertSnapsAndLocksCleanly(0,   0f)
    @Test fun i_90deg()  = assertSnapsAndLocksCleanly(0,  90f)
    @Test fun i_180deg() = assertSnapsAndLocksCleanly(0, 180f)
    @Test fun i_270deg() = assertSnapsAndLocksCleanly(0, 270f)

    // ── O piece (index 1) ──────────────────────────────────────────────────
    @Test fun o_0deg()   = assertSnapsAndLocksCleanly(1,   0f)
    @Test fun o_90deg()  = assertSnapsAndLocksCleanly(1,  90f)
    @Test fun o_180deg() = assertSnapsAndLocksCleanly(1, 180f)
    @Test fun o_270deg() = assertSnapsAndLocksCleanly(1, 270f)

    // ── T piece (index 2) ──────────────────────────────────────────────────
    @Test fun t_0deg()   = assertSnapsAndLocksCleanly(2,   0f)
    @Test fun t_90deg()  = assertSnapsAndLocksCleanly(2,  90f)
    @Test fun t_180deg() = assertSnapsAndLocksCleanly(2, 180f)
    @Test fun t_270deg() = assertSnapsAndLocksCleanly(2, 270f)

    // ── L piece (index 3) ──────────────────────────────────────────────────
    @Test fun l_0deg()   = assertSnapsAndLocksCleanly(3,   0f)
    @Test fun l_90deg()  = assertSnapsAndLocksCleanly(3,  90f)
    @Test fun l_180deg() = assertSnapsAndLocksCleanly(3, 180f)
    @Test fun l_270deg() = assertSnapsAndLocksCleanly(3, 270f)

    // ── J piece (index 4) ──────────────────────────────────────────────────
    @Test fun j_0deg()   = assertSnapsAndLocksCleanly(4,   0f)
    @Test fun j_90deg()  = assertSnapsAndLocksCleanly(4,  90f)
    @Test fun j_180deg() = assertSnapsAndLocksCleanly(4, 180f)
    @Test fun j_270deg() = assertSnapsAndLocksCleanly(4, 270f)

    // ── S piece (index 5) ──────────────────────────────────────────────────
    @Test fun s_0deg()   = assertSnapsAndLocksCleanly(5,   0f)
    @Test fun s_90deg()  = assertSnapsAndLocksCleanly(5,  90f)
    @Test fun s_180deg() = assertSnapsAndLocksCleanly(5, 180f)
    @Test fun s_270deg() = assertSnapsAndLocksCleanly(5, 270f)

    // ── Z piece (index 6) ──────────────────────────────────────────────────
    @Test fun z_0deg()   = assertSnapsAndLocksCleanly(6,   0f)
    @Test fun z_90deg()  = assertSnapsAndLocksCleanly(6,  90f)
    @Test fun z_180deg() = assertSnapsAndLocksCleanly(6, 180f)
    @Test fun z_270deg() = assertSnapsAndLocksCleanly(6, 270f)
}
