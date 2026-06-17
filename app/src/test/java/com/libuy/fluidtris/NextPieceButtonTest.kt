package com.libuy.fluidtris

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NextPieceButtonTest {

    private val VW = 1080
    private val VH = 1920

    @Test
    fun onNextPieceButton_during_game_over_does_nothing() {
        val e = GameEngine(
            onPieceLocked = {},
            onLineCleared = {}
        )
        e.resetGame(VW, VH)

        e.isGameOver = true
        val beforeCurrentPiece = e.currentPiece
        val beforeOtherCount = e.otherFallingPieces.size

        e.onNextPieceButton(VW, VH)

        // State should not change when game is over
        assertEquals("Current piece should not change", beforeCurrentPiece, e.currentPiece)
        assertEquals("No new pieces should spawn", beforeOtherCount, e.otherFallingPieces.size)
    }

    @Test
    fun onNextPieceButton_spawns_new_piece_and_keeps_current() {
        val e = GameEngine(
            onPieceLocked = {},
            onLineCleared = {}
        )
        e.resetGame(VW, VH)

        val initialCurrentPiece = e.currentPiece
        val initialNextPiece = e.nextPiece

        // Call next piece button
        e.onNextPieceButton(VW, VH)

        // Current piece should now be the previous next piece
        assertEquals("Current should be previous next", initialNextPiece, e.currentPiece)

        // Old piece should be in other pieces
        assertEquals("Old piece should be stored", 1, e.otherFallingPieces.size)
        assertEquals("Old piece type should match", initialCurrentPiece, e.otherFallingPieces[0].type)

        // Next piece should be new random
        assertTrue("Next should be random", e.nextPiece in 0 until GameConstants.PIECES.size)
    }

    @Test
    fun onNextPieceButton_new_piece_at_spawn_position() {
        val e = GameEngine(
            onPieceLocked = {},
            onLineCleared = {}
        )
        e.resetGame(VW, VH)

        // Move current piece away from spawn
        e.pieceX = 300f
        e.pieceY = 600f

        // Click next
        e.onNextPieceButton(VW, VH)

        // New current piece should be at spawn
        assertEquals("New piece X should be center", (VW / 2).toFloat() - 50f, e.pieceX, 0.1f)
        assertEquals("New piece Y should be grid top", GameConstants.GRID_TOP, e.pieceY, 0.1f)

        // Old piece should retain its position
        assertEquals("Old piece X preserved", 300f, e.otherFallingPieces[0].x, 0.1f)
        assertEquals("Old piece Y preserved", 600f, e.otherFallingPieces[0].y, 0.1f)
    }

    @Test
    fun onNextPieceButton_multiple_clicks_spawn_multiple() {
        val e = GameEngine(
            onPieceLocked = {},
            onLineCleared = {}
        )
        e.resetGame(VW, VH)

        val piece0 = e.currentPiece
        val piece1 = e.nextPiece

        // First click
        e.onNextPieceButton(VW, VH)
        assertEquals("After click 1, should have 1 other piece", 1, e.otherFallingPieces.size)
        assertEquals("Current should be previous next", piece1, e.currentPiece)

        val piece2 = e.nextPiece

        // Second click
        e.onNextPieceButton(VW, VH)
        assertEquals("After click 2, should have 2 other pieces", 2, e.otherFallingPieces.size)
        assertEquals("Current should be piece2", piece2, e.currentPiece)
    }

    @Test
    fun onNextPieceButton_preserves_score() {
        val e = GameEngine(
            onPieceLocked = {},
            onLineCleared = {}
        )
        e.resetGame(VW, VH)

        e.score = 500

        e.onNextPieceButton(VW, VH)

        assertEquals("Score should be preserved", 500, e.score)
    }

    @Test
    fun touchOnOtherFallingPiece_canDragIt() {
        val e = GameEngine(onPieceLocked = {}, onLineCleared = {})
        e.resetGame(VW, VH)

        // Use O-piece (2x2 fully filled) at a known position, then press Next
        e.currentPiece = 1  // O-piece
        e.pieceX = 300f
        e.pieceY = 600f
        e.onNextPieceButton(VW, VH)

        // fallingPieces[1] is the old O-piece at (300, 600)
        // Touch dead center of the O-piece (all cells filled, guaranteed hit)
        val touchX = 300f + 2 * GameConstants.PIECE_SIZE / 2f  // 400f
        val touchY = 600f + 2 * GameConstants.PIECE_SIZE / 2f  // 700f

        val hit = e.onTouchDown(touchX, touchY)
        assertTrue("Should register a hit on the other falling piece", hit)
        assertTrue("Should be dragging after hitting another piece", e.isDragging)
    }

}
