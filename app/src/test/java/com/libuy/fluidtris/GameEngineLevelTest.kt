package com.libuy.fluidtris

import org.junit.Assert.assertEquals
import org.junit.Test

class GameEngineLevelTest {

    private val VW = 1080
    private val VH = 1920

    @Test
    fun levelIncreasesWhenScoreCrosses1000() {
        var fakeTimeMs = 0L
        val e = GameEngine(
            onPieceLocked = {},
            onLineCleared = {}
        )
        e.currentTimeMs = { fakeTimeMs }
        e.resetGame(VW, VH)

        // Start with score at 150 (level 1)
        e.score = 150
        assertEquals("Initial level should be 1", 1, e.getLevel())

        val cellWidth = (VW - GameConstants.GRID_LEFT - GameConstants.GRID_RIGHT_MARGIN) / GameConstants.GRID_COLUMNS
        val cellHeight = (VH - GameConstants.GRID_TOP - GameConstants.GRID_BOTTOM_MARGIN) / GameConstants.GRID_ROWS

        // Manually fill row 19 (bottom row) with all blocks except one column
        for (col in 0 until GameConstants.GRID_COLUMNS - 1) {
            e.grid[GameConstants.GRID_ROWS - 1][col] = 0xFF0000  // Red block
        }

        // Position I-piece (4 blocks in a row) at the top, rotated horizontally
        e.currentPiece = 0  // I-piece
        e.pieceY = GameConstants.GRID_TOP + cellHeight * 2
        e.pieceX = GameConstants.GRID_LEFT + cellWidth * (GameConstants.GRID_COLUMNS - 2)
        e.pieceRotation = 90f  // Horizontal orientation

        // Simulate game loop until piece locks
        val maxIterations = 10_000
        var iterations = 0
        while (!e.isGameOver && iterations < maxIterations) {
            fakeTimeMs += 16L
            e.update(VW, VH)
            iterations++

            // Stop once score has increased (line cleared)
            if (e.score > 150) {
                break
            }
        }

        // Verify level increased (score crossed NEXT_LEVEL_SCORE threshold)
        assertEquals("Level should increase after score crosses NEXT_LEVEL_SCORE", true, e.getLevel() > 1)
        assertEquals("Score should be at least NEXT_LEVEL_SCORE", true, e.score >= GameConstants.NEXT_LEVEL_SCORE)
    }

    @Test
    fun levelScallingWithScore() {
        val e = GameEngine(
            onPieceLocked = {},
            onLineCleared = {}
        )
        val threshold = GameConstants.NEXT_LEVEL_SCORE

        // Test various score thresholds
        e.score = 0
        assertEquals("Score 0 should be level 1", 1, e.getLevel())

        e.score = threshold - 1
        assertEquals("Score below threshold should be level 1", 1, e.getLevel())

        e.score = threshold
        assertEquals("Score at threshold should be level 2", 2, e.getLevel())

        e.score = threshold * 2 - 1
        assertEquals("Score below 2x threshold should be level 2", 2, e.getLevel())

        e.score = threshold * 2
        assertEquals("Score at 2x threshold should be level 3", 3, e.getLevel())

        e.score = threshold * 5
        assertEquals("Score at 5x threshold should be level 6", 6, e.getLevel())
    }

    @Test
    fun levelMultiplierIncreasesGravity() {
        var fakeTimeMs = 0L
        val e = GameEngine(
            onPieceLocked = {},
            onLineCleared = {}
        )
        e.currentTimeMs = { fakeTimeMs }
        e.resetGame(VW, VH)

        // Record piece Y position at level 1
        val initialY = e.pieceY
        val framesToAdvance = 100

        fakeTimeMs = 0L
        for (i in 0 until framesToAdvance) {
            fakeTimeMs += 16L
            e.update(VW, VH)
        }
        val yAtLevel1 = e.pieceY
        val distanceAtLevel1 = yAtLevel1 - initialY

        // Reset to same initial state but with higher score (level 4)
        fakeTimeMs = 0L
        e.resetGame(VW, VH)
        e.score = 3000  // Level 4
        val initialY2 = e.pieceY

        for (i in 0 until framesToAdvance) {
            fakeTimeMs += 16L
            e.update(VW, VH)
        }
        val yAtLevel4 = e.pieceY
        val distanceAtLevel4 = yAtLevel4 - initialY2

        // Piece should fall further at level 4 due to increased gravity
        assertEquals("Level 4 should have 4x gravity multiplier", true, distanceAtLevel4 > distanceAtLevel1)
    }

    @Test
    fun levelUpCallbackTriggeredOnLineCleared() {
        var fakeTimeMs = 0L
        var levelUpCount = 0
        val e = GameEngine(
            onPieceLocked = {},
            onLineCleared = {},
            onLevelUp = { levelUpCount++ }
        )
        e.currentTimeMs = { fakeTimeMs }
        e.resetGame(VW, VH)

        // Start with score at 150 (level 1)
        e.score = 150
        assertEquals("Initial level should be 1", 1, e.getLevel())
        assertEquals("Level up should not have been called", 0, levelUpCount)

        val cellWidth = (VW - GameConstants.GRID_LEFT - GameConstants.GRID_RIGHT_MARGIN) / GameConstants.GRID_COLUMNS
        val cellHeight = (VH - GameConstants.GRID_TOP - GameConstants.GRID_BOTTOM_MARGIN) / GameConstants.GRID_ROWS

        // Fill bottom row to trigger line clear
        for (col in 0 until GameConstants.GRID_COLUMNS - 1) {
            e.grid[GameConstants.GRID_ROWS - 1][col] = 0xFF0000
        }

        // Position I-piece to complete the line
        e.currentPiece = 0  // I-piece
        e.pieceY = GameConstants.GRID_TOP + cellHeight * 2
        e.pieceX = GameConstants.GRID_LEFT + cellWidth * (GameConstants.GRID_COLUMNS - 2)
        e.pieceRotation = 90f

        // Simulate until line is cleared and score crosses NEXT_LEVEL_SCORE
        val maxIterations = 10_000
        var iterations = 0
        while (!e.isGameOver && iterations < maxIterations) {
            fakeTimeMs += 16L
            e.update(VW, VH)
            iterations++

            if (e.score > 150) break
        }

        // Verify level up callback was called
        assertEquals("Level up callback should have been called", 1, levelUpCount)
        assertEquals("Level should increase after crossing threshold", true, e.getLevel() > 1)
    }

}
