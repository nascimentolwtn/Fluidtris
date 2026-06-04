package com.libuy.fluidtris

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameOverScreenTest {

    private val VW = 1080
    private val VH = 1920

    private fun engine(): GameEngine {
        val e = GameEngine(onPieceLocked = {}, onLineCleared = {})
        e.resetGame(VW, VH)
        return e
    }

    @Test
    fun gameOverScreen_displaysCurrentScore() {
        val e = engine()
        e.score = 500
        e.turnPieceRigid(VW, VH)
        assertTrue("Game should be over after locking piece at top", e.isGameOver)
        assertEquals("Current score should be displayed", 500, e.score)
    }

    @Test
    fun gameOverScreen_displaysHighScore() {
        val e = engine()
        e.score = 500
        e.highScore = 1000
        e.turnPieceRigid(VW, VH)
        assertTrue("Game should be over", e.isGameOver)
        assertEquals("High score should be retained", 1000, e.highScore)
    }

    @Test
    fun gameOverScreen_scoreLessThanHighScore() {
        val e = engine()
        e.score = 500
        e.highScore = 1000
        e.turnPieceRigid(VW, VH)
        assertTrue("Game should be over", e.isGameOver)
        assertTrue("Current score should be less than high score", e.score < e.highScore)
    }

    @Test
    fun gameOverScreen_scoreBeatHighScore() {
        val newScoreReached = mutableListOf<Int>()
        val e = GameEngine(
            onPieceLocked = {},
            onLineCleared = {},
            onHighScoreBeat = { newScore -> newScoreReached.add(newScore) }
        )
        e.resetGame(VW, VH)
        e.highScore = 100
        e.score = 500
        // Manually check the line clear logic
        val initialGrid = e.grid.map { it.clone() }
        for (i in 0 until GameConstants.GRID_ROWS) {
            for (j in 0 until GameConstants.GRID_COLUMNS) {
                e.grid[i][j] = GameConstants.PIECE_COLORS[0]
            }
        }
        e.checkLines()
        // After clearing all lines, score should be 500 + (20 * 100)
        assertEquals("Score should increase after line clears", 500 + GameConstants.GRID_ROWS * 100, e.score)
        assertTrue("High score callback should have been called", newScoreReached.size > 0)
    }

    @Test
    fun gameOverState_doesNotAllowMovement() {
        val e = engine()
        e.turnPieceRigid(VW, VH)
        assertTrue("Game should be over", e.isGameOver)
        val startY = e.pieceY
        e.update(VW, VH)
        assertEquals("Piece should not move when game is over", startY, e.pieceY, 0.001f)
    }

    @Test
    fun gameOverState_canBeReset() {
        val e = engine()
        e.score = 500
        e.turnPieceRigid(VW, VH)
        assertTrue("Game should be over", e.isGameOver)

        e.resetGame(VW, VH)
        assertFalse("Game should no longer be over after reset", e.isGameOver)
        assertEquals("Score should be reset to zero", 0, e.score)
    }

    @Test
    fun gameOver_highScorePersistsAcrossResets() {
        val e = engine()
        e.highScore = 2000
        e.score = 500
        e.turnPieceRigid(VW, VH)

        val originalHighScore = e.highScore
        e.resetGame(VW, VH)

        assertEquals("High score should persist after reset", originalHighScore, e.highScore)
    }
}