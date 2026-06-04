package com.libuy.fluidtris

import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameEngineLineTest {

    private val VW = 1080
    private val VH = 1920

    private fun engine(): GameEngine {
        val e = GameEngine(onPieceLocked = {}, onLineCleared = {})
        e.resetGame(VW, VH)
        return e
    }

    @Test
    fun emptyGrid_noLinesCleared() {
        val e = engine()
        e.checkLines()
        assertEquals(0, e.score)
        for (i in 0 until GameConstants.GRID_ROWS)
            for (j in 0 until GameConstants.GRID_COLUMNS)
                assertNull(e.grid[i][j])
    }

    @Test
    fun partialRow_notCleared() {
        val e = engine()
        for (j in 0 until 6) e.grid[19][j] = Color.RED
        e.checkLines()
        assertEquals(0, e.score)
        for (j in 0 until 6) assertEquals(Color.RED, e.grid[19][j])
        assertNull(e.grid[19][6])
    }

    @Test
    fun singleFullRow_cleared() {
        val e = engine()
        for (j in 0 until GameConstants.GRID_COLUMNS) e.grid[19][j] = Color.RED
        e.checkLines()
        assertEquals(100, e.score)
        for (j in 0 until GameConstants.GRID_COLUMNS) assertNull(e.grid[19][j])
    }

    @Test
    fun singleFullRow_rowAboveShiftsDown() {
        val e = engine()
        e.grid[18][0] = Color.BLUE
        for (j in 0 until GameConstants.GRID_COLUMNS) e.grid[19][j] = Color.RED
        e.checkLines()
        assertNull(e.grid[18][0])
        assertEquals(Color.BLUE, e.grid[19][0])
    }

    @Test
    fun twoFullRows_score200() {
        val e = engine()
        for (j in 0 until GameConstants.GRID_COLUMNS) e.grid[18][j] = Color.RED
        for (j in 0 until GameConstants.GRID_COLUMNS) e.grid[19][j] = Color.RED
        e.checkLines()
        assertEquals(200, e.score)
        for (j in 0 until GameConstants.GRID_COLUMNS) {
            assertNull(e.grid[18][j])
            assertNull(e.grid[19][j])
        }
    }

    @Test
    fun clearedRows_shiftDownCorrectly() {
        val e = engine()
        e.grid[17][3] = Color.GREEN
        for (j in 0 until GameConstants.GRID_COLUMNS) e.grid[18][j] = Color.RED
        for (j in 0 until GameConstants.GRID_COLUMNS) e.grid[19][j] = Color.RED
        e.checkLines()
        assertEquals(200, e.score)
        assertNull(e.grid[17][3])
        assertNull(e.grid[18][3])
        assertEquals(Color.GREEN, e.grid[19][3])
    }
}
