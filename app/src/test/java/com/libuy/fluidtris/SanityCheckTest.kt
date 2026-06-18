package com.libuy.fluidtris

import org.junit.Assert.assertNotNull
import org.junit.Test

class SanityCheckTest {

    private val VW = 1080
    private val VH = 1920
    // gridBottom = VH - GRID_BOTTOM_MARGIN = 1920 - 180 = 1740
    // cellWidth  = (1080 - 200) / 8 = 110f
    // cellHeight = (1920 - 280) / 20 = 82f

    private fun engine(): GameEngine {
        val e = GameEngine(onPieceLocked = {}, onLineCleared = {})
        e.resetGame(VW, VH)
        return e
    }

    // I-piece (type 0) at 0°, pieceX=440f: block centers at bx=490,590,690,790 → gx=3,4,5,6
    // At pieceY=2000: by=2050 → gy=23 (out of bounds). Push-up loop nudges piece up until gy=19.
    @Test
    fun pieceBelowGridBottom_isForceLockedToTopOfColumn() {
        val e = engine()
        e.currentPiece = 0   // I-piece
        e.pieceX = 440f
        e.pieceY = 2000f     // well past gridBottom (1740)
        e.pieceRotation = 0f
        e.update(VW, VH)
        assertNotNull("I-piece col 3 must land at row 19", e.grid[19][3])
        assertNotNull("I-piece col 4 must land at row 19", e.grid[19][4])
        assertNotNull("I-piece col 5 must land at row 19", e.grid[19][5])
        assertNotNull("I-piece col 6 must land at row 19", e.grid[19][6])
    }

    // Edge case: piece just barely past gridBottom (block center at gridBottom + 1f)
    @Test
    fun pieceBarelyPastGridBottom_isForceLockedNotDropped() {
        val e = engine()
        e.currentPiece = 1   // O-piece (2x2), all blocks at same row
        e.pieceX = 440f
        // gridBottom=1740; O-piece block centers at by=pieceY+50 and pieceY+150
        // Set so that bottom block center (pieceY+150) is just past gridBottom:
        // pieceY + 150 = 1741 → pieceY = 1591
        e.pieceY = 1591f
        e.pieceRotation = 0f
        e.update(VW, VH)
        // At least one grid row near the bottom must be written (piece was not silently dropped)
        val anyLocked = (15..19).any { row ->
            (0 until GameConstants.GRID_COLUMNS).any { col -> e.grid[row][col] != null }
        }
        assert(anyLocked) { "O-piece barely past bottom must be locked into grid, not dropped" }
    }
}
