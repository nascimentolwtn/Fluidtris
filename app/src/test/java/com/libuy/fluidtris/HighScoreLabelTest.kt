package com.libuy.fluidtris

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HighScoreLabelTest {

    private val VW = 1080
    private val VH = 1920

    @Test
    fun isBeatingHighScoreFlagStartsFalse() {
        val e = GameEngine(
            onPieceLocked = {},
            onLineCleared = {}
        )
        e.resetGame(VW, VH)

        // On game start with 0 score and 0 highScore, should not be beating
        assertFalse("Should not be beating high score at start", e.isBeatingHighScore)
    }

    @Test
    fun isBeatingHighScoreFlagSetWhenScoreBeatsPreviousHigh() {
        val e = GameEngine(
            onPieceLocked = {},
            onLineCleared = {}
        )

        // Set a previous high score of 500
        e.highScore = 500
        e.score = 0

        // Initially not beating the high score
        assertFalse("Should not be beating high score of 500 with score 0", e.isBeatingHighScore)

        // Simulate score beating high score by directly calling clearLines after setting up grid
        // Fill bottom row to simulate a line that will be cleared
        for (col in 0 until GameConstants.GRID_COLUMNS) {
            e.grid[GameConstants.GRID_ROWS - 1][col] = 0xFF0000
        }

        // Call checkLines() to clear the row and update score
        e.checkLines()

        // After clearing 1 line, score should be 100 (still less than 500)
        assertFalse("Score of 100 should not beat high score of 500", e.isBeatingHighScore)

        // Fill 4 complete rows to get 800 points
        for (row in GameConstants.GRID_ROWS - 4 until GameConstants.GRID_ROWS) {
            for (col in 0 until GameConstants.GRID_COLUMNS) {
                e.grid[row][col] = 0xFF0000
            }
        }

        e.checkLines()

        // After clearing 4 lines (800 points), should be beating high score of 500
        assertTrue("Score of 900 should beat high score of 500", e.isBeatingHighScore)
    }

    @Test
    fun isBeatingHighScoreResetOnGameReset() {
        val e = GameEngine(
            onPieceLocked = {},
            onLineCleared = {}
        )
        e.resetGame(VW, VH)

        // Set flags manually
        e.isBeatingHighScore = true
        e.score = 1000
        e.highScore = 500

        // Reset game
        e.resetGame(VW, VH)

        // Flags should be reset
        assertFalse("isBeatingHighScore should be false after game reset", e.isBeatingHighScore)
        assertTrue("Score should be 0 after reset", e.score == 0)
    }
}
