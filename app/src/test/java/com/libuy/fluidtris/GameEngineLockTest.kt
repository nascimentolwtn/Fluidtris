package com.libuy.fluidtris

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineLockTest {

    private val VW = 1080
    private val VH = 1920

    // Default I-piece (index 0) after resetGame: pieceX = VW/2 - 50 = 490f, pieceY = 100f
    // cellW = (1080 - 300) / 7 ≈ 111.43f
    // For each block (0,c): gridX = (490 + c*100 + 50 - 150) / cellW
    //   c=0 → 390/111.43 ≈ 3   c=1 → 490/111.43 ≈ 4   c=2 → 5   c=3 → 6
    //   gridY = (100 + 0*100 + 50 - 100) / 82 = 50/82 ≈ 0

    private fun engine(): GameEngine {
        val e = GameEngine(onPieceLocked = {}, onLineCleared = {})
        e.resetGame(VW, VH)
        return e
    }

    @Test
    fun lockAtBottom_blocksWrittenToGrid() {
        val e = engine()
        e.currentPiece = 0  // I-piece: row 0, cols 3-6 at default spawn position
        e.pieceRotation = 0f
        e.lockPieceAtBottom(VW, VH)
        assertNotNull(e.grid[0][3])
        assertNotNull(e.grid[0][4])
        assertNotNull(e.grid[0][5])
        assertNotNull(e.grid[0][6])
    }

    @Test
    fun lockAtBottom_callsOnPieceLocked() {
        var called = false
        val e = GameEngine(onPieceLocked = { called = true }, onLineCleared = {})
        e.resetGame(VW, VH)
        e.lockPieceAtBottom(VW, VH)
        assertTrue("onPieceLocked should have been called", called)
    }

    @Test
    fun turnPieceRigid_nextPieceSpawned() {
        val e = engine()
        e.currentPiece = 0
        e.pieceRotation = 0f
        val savedNext = e.nextPiece
        e.turnPieceRigid(VW, VH)
        assertEquals(savedNext, e.currentPiece)
        assertTrue(e.nextPiece in 0 until GameConstants.PIECES.size)
    }

    @Test
    fun turnPieceRigid_pieceResetToTop() {
        val e = engine()
        e.currentPiece = 0
        e.pieceRotation = 0f
        val savedNextRotation = e.nextPieceRotation
        e.turnPieceRigid(VW, VH)
        assertEquals(GameConstants.GRID_TOP, e.pieceY, 0.001f)
        assertEquals(savedNextRotation, e.pieceRotation, 0.001f)
    }

    @Test
    fun turnPieceRigid_gameOverWhenTopRowOccupied() {
        val e = engine()
        // Default I-piece at pieceY=100 places blocks in row 0 (cols 3-6), not a full row →
        // line not cleared → grid[0].any { != null } = true → isGameOver
        e.turnPieceRigid(VW, VH)
        assertTrue(e.isGameOver)
    }

    @Test
    fun turnPieceRigid_rotationSnaps() {
        val e = engine()
        e.currentPiece = 0  // I-piece required for col assertions below
        // 91° rounds to 90° (Math.round(91/90) = 1).
        // At 90° the I-piece occupies a single column (col 3), not four columns.
        e.pieceRotation = 91f
        e.turnPieceRigid(VW, VH)
        // Col 3 is filled (same gridX for all 4 blocks at 90°)
        assertNotNull(e.grid[0][3])
        // Col 4 would be filled at 0° but is empty at 90°, confirming the snap happened
        assertNull(e.grid[0][4])
    }
}
