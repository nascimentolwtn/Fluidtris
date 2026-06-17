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

        // Start with score at 900 (level 1)
        e.score = 900
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
            if (e.score > 900) {
                break
            }
        }

        // Verify level increased to 2
        assertEquals("Level should be 2 after score crosses 1000", 2, e.getLevel())
        assertEquals("Score should be at least 1000", true, e.score >= 1000)
    }

    @Test
    fun levelScallingWithScore() {
        val e = GameEngine(
            onPieceLocked = {},
            onLineCleared = {}
        )

        // Test various score thresholds
        e.score = 0
        assertEquals("Score 0 should be level 1", 1, e.getLevel())

        e.score = 999
        assertEquals("Score 999 should be level 1", 1, e.getLevel())

        e.score = 1000
        assertEquals("Score 1000 should be level 2", 2, e.getLevel())

        e.score = 1999
        assertEquals("Score 1999 should be level 2", 2, e.getLevel())

        e.score = 2000
        assertEquals("Score 2000 should be level 3", 3, e.getLevel())

        e.score = 5000
        assertEquals("Score 5000 should be level 6", 6, e.getLevel())
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

}
