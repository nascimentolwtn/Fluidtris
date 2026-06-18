package com.libuy.fluidtris

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SnapSlideTest {

    private val VW = 1080
    private val VH = 1920
    // cellWidth  = (1080 - 200) / 8 = 110f
    // cellHeight = (1920 - 280) / 20 = 82f
    // gridBottom = 1920 - 180 = 1740
    // O-piece bottom edge at pieceY+200; contact when pieceY+200 > 1740 → pieceY > 1540
    // Use pieceY=1549f.

    private val O_PIECE = 1
    private val O_PIECEx = GameConstants.GRID_LEFT  // 100f → cols 0-1
    private val O_PIECEy = 1549f

    private fun engineWithClock(): Pair<GameEngine, () -> Unit> {
        var fakeTime = 0L
        val e = GameEngine(onPieceLocked = {}, onLineCleared = {})
        e.currentTimeMs = { fakeTime }
        e.resetGame(VW, VH)
        e.currentPiece = O_PIECE
        e.pieceRotation = 0f
        e.pieceX = O_PIECEx
        e.pieceY = O_PIECEy
        val tick: () -> Unit = { fakeTime += 16L; e.update(VW, VH) }
        return e to tick
    }

    private fun centerTouchX(e: GameEngine) = e.pieceX + 50f
    private fun centerTouchY(e: GameEngine) = e.pieceY + 50f

    // Any center drag during snap → snap cancelled, piece slides to new X, piece is fluid
    @Test
    fun snapRelease_centerDragRight_movesPieceAndCancelsSnap() {
        val (e, tick) = engineWithClock()
        repeat(5) { tick() }
        assertTrue("snap animation must be active before drag", e.fallingPieces[0].isSnapAnimating)

        val touchX = centerTouchX(e)
        val touchY = centerTouchY(e)
        e.onTouchDown(touchX, touchY)

        val xBefore = e.pieceX
        e.onTouchMove(touchX + 60f, touchY, VW, VH)

        assertTrue("pieceX must increase after right drag", e.pieceX > xBefore)
        assertFalse("snap animation must be CANCELLED after drag", e.fallingPieces[0].isSnapAnimating)
        assertFalse("lock timer must be reset", e.fallingPieces[0].isWaitingToTurnRigidAtBottom)
    }

    // Dragging into a solid block during snap → snap cancelled, position unchanged, spring bounces away
    @Test
    fun snapRelease_centerDragIntoBlock_bouncesAndReleasesSnap() {
        val (e, tick) = engineWithClock()
        repeat(5) { tick() }
        assertTrue(e.fallingPieces[0].isSnapAnimating)

        // Place a blocking column to the right (cols 2-3, rows 18-19)
        e.grid[18][2] = android.graphics.Color.RED
        e.grid[19][2] = android.graphics.Color.RED

        val touchX = centerTouchX(e)
        val touchY = centerTouchY(e)
        e.onTouchDown(touchX, touchY)

        val xBefore = e.pieceX
        e.onTouchMove(touchX + 110f, touchY, VW, VH)

        assertEquals("pieceX must not change into blocked column", xBefore, e.pieceX, 0.001f)
        assertFalse("snap must be cancelled even on blocked slide", e.fallingPieces[0].isSnapAnimating)
        assertTrue("spring must push left (bounce away)", e.fallingPieces[0].springForceX < 0f)
    }

    // Rotation gesture during snap → snap cancelled, rotation changes
    @Test
    fun snapRelease_rotationDrag_cancelsSnapAndRotates() {
        // T-piece: block (0,0) is non-center; touching it enables rotation drag
        var fakeTime = 0L
        val e = GameEngine(onPieceLocked = {}, onLineCleared = {})
        e.currentTimeMs = { fakeTime }
        e.resetGame(VW, VH)
        e.currentPiece = 2  // T-piece; PIECE_CENTER_CELLS[2] = {(0,1)} only
        e.pieceRotation = 0f
        e.pieceX = O_PIECEx
        e.pieceY = O_PIECEy
        repeat(5) { fakeTime += 16L; e.update(VW, VH) }
        assertTrue("snap animation must be active", e.fallingPieces[0].isSnapAnimating)

        // Touch block (0,0) → non-center → rotation drag
        val nonCenterX = e.pieceX + 50f
        val nonCenterY = e.pieceY + 50f
        assertTrue("must grab T-piece", e.onTouchDown(nonCenterX, nonCenterY))

        val rotBefore = e.pieceRotation
        e.onTouchMove(nonCenterX + 50f, nonCenterY - 30f, VW, VH)

        assertFalse("snap must be cancelled after rotation gesture", e.fallingPieces[0].isSnapAnimating)
        assertNotEquals("rotation must change during snap-release rotation gesture", rotBefore, e.pieceRotation, 0.001f)
    }

    // Normal drag collision (non-snap) also gets a bounce spring
    @Test
    fun normalDrag_collisionWithBlock_springBouncesAwayFromWall() {
        // Set up O-piece mid-air with no snap state.
        val e = GameEngine(onPieceLocked = {}, onLineCleared = {})
        e.resetGame(VW, VH)
        e.currentPiece = O_PIECE
        e.pieceX = 100f
        e.pieceY = 500f
        e.pieceRotation = 0f

        // checkPieceCollidingWithGrid uses block CORNERS (±50px).
        // O-piece at x=100, y=500: block (0,1) center bx=360, by=556.
        // After drag to x=210: bx=360, top-right corner=(410, 506).
        // gx=(410-100)/110=2, gy=(506-100)/82=4. Place block there.
        e.grid[4][2] = android.graphics.Color.BLUE

        val touchX = 150f   // center of block (0,0): pieceX+50
        val touchY = 550f   // pieceY+50
        e.onTouchDown(touchX, touchY)

        val xBefore = e.pieceX
        e.onTouchMove(touchX + 110f, touchY, VW, VH)  // push right into filled column

        assertEquals("piece must not move into blocked column", xBefore, e.pieceX, 0.001f)
        assertTrue("spring must push back (away from block)", e.fallingPieces[0].springForceX < 0f)
    }
}
