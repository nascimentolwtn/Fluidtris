package com.libuy.fluidtris

import android.graphics.Color
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineCollisionTest {

    private val VW = 1080
    private val VH = 1920
    private val cellW = (VW - GameConstants.GRID_LEFT - GameConstants.GRID_RIGHT_MARGIN) / GameConstants.GRID_COLUMNS
    private val cellH = (VH - GameConstants.GRID_TOP - GameConstants.GRID_BOTTOM_MARGIN) / GameConstants.GRID_ROWS

    // gridBottom = VH - GRID_BOTTOM_MARGIN = 1920 - 180 = 1740
    // isPieceAtBottom triggers when any center's by + PIECE_SIZE/2 > 1740
    // For horizontal I-piece (0°): center by = pieceY + 50; condition: pieceY + 50 + 50 > 1740 → pieceY > 1640

    private fun engine(): GameEngine {
        val e = GameEngine(onPieceLocked = {}, onLineCleared = {})
        e.resetGame(VW, VH)
        return e
    }

    // ---- isPieceAtBottom ----

    @Test
    fun pieceWellAboveBottom_false() {
        val e = engine()
        // pieceY = 100 (default after resetGame): center at 150, + 50 = 200 << 1740
        assertFalse(e.isPieceAtBottom(VH))
    }

    @Test
    fun pieceJustAboveBottom_false() {
        val e = engine()
        e.currentPiece = 0  // I-piece (1 row): bottom center = pieceY + 50; 1639+50+50=1739 < 1740
        e.pieceRotation = 0f
        e.pieceY = 1639f
        assertFalse(e.isPieceAtBottom(VH))
    }

    @Test
    fun pieceTouchingBottom_true() {
        val e = engine()
        // pieceY = 1641: center at 1691, + 50 = 1741 > 1740
        e.pieceY = 1641f
        assertTrue(e.isPieceAtBottom(VH))
    }

    @Test
    fun rotatedPieceTouchingBottom_true() {
        val e = engine()
        // Vertical I-piece (90°): lowest center by = pieceY + 200.
        // Condition: pieceY + 200 + 50 > 1740 → pieceY > 1490
        e.pieceY = 1495f
        e.pieceRotation = 90f
        assertTrue(e.isPieceAtBottom(VH))
    }

    // ---- isPieceCollidingWithAnotherPiece ----

    @Test
    fun emptyGrid_false() {
        val e = engine()
        assertFalse(e.isPieceCollidingWithAnotherPiece(cellW, cellH))
    }

    @Test
    fun blockAtExactPieceCell_true() {
        val e = engine()
        // Default I-piece at pieceX=490f, pieceY=100f (0° rotation).
        // Block (0,0) center at (540, 150). Corner (490, 100):
        //   cellX = (490 - 150) / cellW = 340 / 111.43 ≈ 3  → grid col 3
        //   cellY = (100 - 100) / cellH = 0 / 82 = 0          → grid row 0
        e.grid[0][3] = Color.RED
        assertTrue(e.isPieceCollidingWithAnotherPiece(cellW, cellH))
    }

    @Test
    fun blockAdjacentToPiece_false() {
        val e = engine()
        // I-piece occupies cols 3-6 in row 0. Col 0 is safely to the left.
        e.grid[0][0] = Color.RED
        assertFalse(e.isPieceCollidingWithAnotherPiece(cellW, cellH))
    }

    @Test
    fun rotatedPieceOverBlock_true() {
        val e = engine()
        // Rotate I-piece 90°. pieceX=490f, pieceY=100f.
        // cx = 490 + 200 = 690, cy = 100 + 50 = 150.
        // Block (0,2): dx = (490 + 250) - 690 = 50, dy = 0.
        //   After 90° CW: bx = 690, by = 150 + 50 = 200.
        //   Corner (640, 150): cellX = (640-150)/cellW = 490/111.43 ≈ 4  → col 4
        //                      cellY = (150-100)/82 = 50/82 ≈ 0           → row 0
        e.pieceRotation = 90f
        e.grid[0][4] = Color.RED
        assertTrue(e.isPieceCollidingWithAnotherPiece(cellW, cellH))
    }
}
