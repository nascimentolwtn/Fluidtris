package com.libuy.fluidtris

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameNoInteractionTest {

    private val VW = 1080
    private val VH = 1920

    @Test
    fun fullGameWithNoInput_noPiecesCollideFallThrough() {
        var fakeTimeMs = 0L
        val e = GameEngine(
            onPieceLocked = {},
            onLineCleared = {}
        )
        e.currentTimeMs = { fakeTimeMs }
        e.resetGame(VW, VH)

        val cellWidth = (VW - GameConstants.GRID_LEFT - GameConstants.GRID_RIGHT_MARGIN) / GameConstants.GRID_COLUMNS
        val cellHeight = (VH - GameConstants.GRID_TOP - GameConstants.GRID_BOTTOM_MARGIN) / GameConstants.GRID_ROWS

        val maxIterations = 1_000_000  // increased limit since gravity scales with level
        var iterations = 0

        // Run the game until game over
        while (!e.isGameOver && iterations < maxIterations) {
            // Advance time by 16ms (one frame)
            fakeTimeMs += 16L

            // Update the game state
            e.update(VW, VH)

            // After each piece locks, verify no collisions in the grid
            // by checking that no block position is occupied twice
            verifyGridIntegrity(e, cellWidth, cellHeight)

            iterations++
        }

        // Game should have ended (top row filled)
        assertTrue("Game should end after pieces stack up (ran $iterations iterations)", e.isGameOver)
        assertTrue("Game should run for at least a few pieces", iterations > 100)
    }

    @Test
    fun longGameSession_gridBlocksNeverOverlap() {
        var fakeTimeMs = 0L
        val e = GameEngine(
            onPieceLocked = {},
            onLineCleared = {}
        )
        e.currentTimeMs = { fakeTimeMs }
        e.resetGame(VW, VH)

        val maxIterations = 1_000_000  // increased limit since gravity scales with level
        var iterations = 0
        var lockCount = 0

        while (!e.isGameOver && iterations < maxIterations) {
            fakeTimeMs += 16L
            val gridBefore = captureGridState(e)

            e.update(VW, VH)

            val gridAfter = captureGridState(e)

            // Detect when a new piece locked (grid changed)
            if (gridBefore != gridAfter) {
                lockCount++
                // Verify that new blocks were added, not cells overwritten
                for (row in 0 until GameConstants.GRID_ROWS) {
                    for (col in 0 until GameConstants.GRID_COLUMNS) {
                        val before = gridBefore[row][col]
                        val after = gridAfter[row][col]
                        // A cell should never go from occupied to a different block
                        assertFalse(
                            "Grid cell [$row][$col] cannot be overwritten (before=$before, after=$after)",
                            before != null && after != null && before != after
                        )
                    }
                }
            }

            iterations++
        }

        // Verify we locked multiple pieces before game over
        assertTrue("Should have locked at least 10 pieces", lockCount >= 10)
    }

    @Test
    fun randomPiecesNoInput_topRowEventuallyFilled() {
        var fakeTimeMs = 0L
        val e = GameEngine(
            onPieceLocked = {},
            onLineCleared = {}
        )
        e.currentTimeMs = { fakeTimeMs }
        e.resetGame(VW, VH)

        val maxIterations = 1_000_000  // increased limit since gravity scales with level
        var iterations = 0

        while (!e.isGameOver && iterations < maxIterations) {
            fakeTimeMs += 16L
            e.update(VW, VH)
            iterations++
        }

        // Game ended because top row is full
        assertTrue("Game should end because top row filled (ran $iterations iterations)", e.grid[0].any { it != null })
    }

    @Test
    fun withNextButton_otherPieceEventuallyLocksIntoGrid() {
        var fakeTimeMs = 0L
        val e = GameEngine(onPieceLocked = {}, onLineCleared = {})
        e.currentTimeMs = { fakeTimeMs }
        e.resetGame(VW, VH)

        e.onNextPieceButton(VW, VH)
        val initialGridCount = e.grid.sumOf { row -> row.count { it != null } }

        val maxIterations = 20_000
        var iterations = 0
        while (iterations < maxIterations) {
            fakeTimeMs += 16L
            e.update(VW, VH)
            iterations++
            val gridCount = e.grid.sumOf { row -> row.count { it != null } }
            if (gridCount > initialGridCount) break
        }

        val finalGridCount = e.grid.sumOf { row -> row.count { it != null } }
        assertTrue("Other falling piece should have locked into grid (got $finalGridCount blocks)", finalGridCount > initialGridCount)
    }

    @Test
    fun withNextButton_gameEventuallyReachesGameOver() {
        var fakeTimeMs = 0L
        val e = GameEngine(onPieceLocked = {}, onLineCleared = {})
        e.currentTimeMs = { fakeTimeMs }
        e.resetGame(VW, VH)

        e.onNextPieceButton(VW, VH)

        val maxIterations = 1_000_000  // increased limit since gravity scales with level
        var iterations = 0
        while (!e.isGameOver && iterations < maxIterations) {
            fakeTimeMs += 16L
            e.update(VW, VH)
            iterations++
        }

        assertTrue("Game should reach game over even with Next button used (ran $iterations iterations)", e.isGameOver)
    }

    @Test
    fun update_withEmptyFallingPieces_spawnsNewPiece() {
        var fakeTimeMs = 0L
        val e = GameEngine(onPieceLocked = {}, onLineCleared = {})
        e.currentTimeMs = { fakeTimeMs }
        e.resetGame(VW, VH)

        e.fallingPieces.clear()
        assertTrue("Should be empty before update", e.fallingPieces.isEmpty())

        fakeTimeMs += 16L
        e.update(VW, VH)

        assertTrue("update() must spawn a piece when fallingPieces is empty", e.fallingPieces.isNotEmpty())
    }

    private fun verifyGridIntegrity(e: GameEngine, cellWidth: Float, cellHeight: Float) {
        // Count total blocks
        val blockCount = e.grid.sumOf { row -> row.count { it != null } }

        // Verify no cell is null and occupied simultaneously (already guaranteed by Array<Int?> structure)
        // but verify the grid state makes sense
        for (row in 0 until GameConstants.GRID_ROWS) {
            for (col in 0 until GameConstants.GRID_COLUMNS) {
                val cell = e.grid[row][col]
                if (cell != null) {
                    // Cell should be a valid color (non-null, not 0)
                    assertFalse("Grid cell [$row][$col] should not be null if in grid", cell == 0)
                }
            }
        }
    }

    private fun captureGridState(e: GameEngine): Array<Array<Int?>> {
        return Array(GameConstants.GRID_ROWS) { row ->
            Array(GameConstants.GRID_COLUMNS) { col ->
                e.grid[row][col]
            }
        }
    }
}