package com.libuy.fluidtris

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineStateTest {

    private val VW = 1080
    private val VH = 1920

    private fun engine(): GameEngine {
        val e = GameEngine(onPieceLocked = {}, onLineCleared = {})
        e.resetGame(VW, VH)
        return e
    }

    // ---- resetGame ----

    @Test
    fun afterReset_gridAllNull() {
        val e = engine()
        for (i in 0 until GameConstants.GRID_ROWS)
            for (j in 0 until GameConstants.GRID_COLUMNS)
                assertNull(e.grid[i][j])
    }

    @Test
    fun afterReset_scoreZero() {
        val e = engine()
        assertEquals(0, e.score)
    }

    @Test
    fun afterReset_notPaused() {
        val e = engine()
        assertFalse(e.isPaused)
    }

    @Test
    fun afterReset_notGameOver() {
        val e = engine()
        assertFalse(e.isGameOver)
    }

    @Test
    fun afterReset_pieceYAtTop() {
        val e = engine()
        assertEquals(GameConstants.GRID_TOP, e.pieceY, 0.001f)
    }

    // ---- update (gravity) ----

    @Test
    fun whenPaused_pieceYUnchanged() {
        val e = engine()
        val startY = e.pieceY
        e.isPaused = true
        e.update(VW, VH)
        assertEquals(startY, e.pieceY, 0.001f)
    }

    @Test
    fun whenGameOver_pieceYUnchanged() {
        val e = engine()
        val startY = e.pieceY
        e.isGameOver = true
        e.update(VW, VH)
        assertEquals(startY, e.pieceY, 0.001f)
    }

    @Test
    fun whenDragging_pieceYUnchanged() {
        val e = engine()
        val startY = e.pieceY
        e.isDragging = true
        e.update(VW, VH)
        assertEquals(startY, e.pieceY, 0.001f)
    }

    @Test
    fun normalUpdate_pieceYIncreasesByGravity() {
        val e = engine()
        val startY = e.pieceY
        e.update(VW, VH)
        assertEquals(startY + GameConstants.GRAVITY, e.pieceY, 0.001f)
    }
}
