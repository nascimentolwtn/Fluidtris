package com.libuy.fluidtris

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SnapPullTest {

    private val VW = 1080
    private val VH = 1920
    // cellHeight = (1920 - 100 - 180) / 20 = 82f
    // cellWidth  = (1080 - 150 - 150) / 7  ≈ 111.43f
    // gridBottom = 1920 - 180 = 1740
    //
    // O-piece floor position (bottom blocks in row 19, columns 0-1):
    //   bottom block centre Y = GRID_TOP + 19.5 * cellHeight = 100 + 1599 = 1699
    //   pieceY = 1699 - 150 = 1549  (O-piece: bottom block centre = pieceY + 150)
    //   block edge = 1699 + 50 = 1749 > gridBottom 1740  →  contact ✓

    private val O_PIECE = 1
    private val O_PIECEx = GameConstants.GRID_LEFT  // 150f
    private val O_PIECEy = 1549f

    private fun engineWithClock(onLocked: () -> Unit = {}): Pair<GameEngine, () -> Unit> {
        var fakeTime = 0L
        val e = GameEngine(onPieceLocked = onLocked, onLineCleared = {})
        e.currentTimeMs = { fakeTime }
        e.resetGame(VW, VH)
        e.currentPiece = O_PIECE
        e.pieceRotation = 0f
        e.pieceX = O_PIECEx
        e.pieceY = O_PIECEy
        val tick: () -> Unit = {
            fakeTime += 16L
            e.update(VW, VH)
        }
        return e to tick
    }

    // ── lerpAngleDeg (pure math) ──────────────────────────────────────────

    @Test
    fun lerpAngleDeg_midpoint() {
        assertEquals(45f, lerpAngleDeg(0f, 90f, 0.5f), 0.01f)
    }

    @Test
    fun lerpAngleDeg_shortestPath_acrossZero() {
        assertEquals(0f, lerpAngleDeg(350f, 10f, 0.5f) % 360f, 0.01f)
    }

    @Test
    fun lerpAngleDeg_noChangeWhenAtTarget() {
        assertEquals(90f, lerpAngleDeg(90f, 90f, 1f), 0.001f)
    }

    // ── snap animation commits target once and converges ──────────────────

    @Test
    fun snapPull_pieceLocksAfterDelay() {
        var locked = false
        val (_, tick) = engineWithClock(onLocked = { locked = true })
        // LOCK_DELAY_MS = 3 000 ms / 16 ms per tick ≈ 188 ticks; 220 to be safe.
        repeat(220) { tick() }
        assertTrue("piece should lock after LOCK_DELAY_MS", locked)
    }

    @Test
    fun snapPull_driftsPieceX_towardGridCentre() {
        val (e, tick) = engineWithClock()
        e.pieceX = O_PIECEx + 20f
        val xBefore = e.pieceX
        repeat(125) { tick() }
        assertTrue(
            "pieceX should drift left toward grid column centre (was $xBefore, now ${e.pieceX})",
            e.pieceX < xBefore
        )
    }

    @Test
    fun snapPull_rotationLerpsTowardNearest90() {
        val (e, tick) = engineWithClock()
        e.pieceRotation = 40f
        repeat(125) { tick() }
        assertTrue(
            "rotation should move from 40° toward 0° (was 40, now ${e.pieceRotation})",
            e.pieceRotation < 39f
        )
    }

    @Test
    fun snapPull_movesYTowardSnapTarget() {
        // Piece starts 8px below the grid-aligned row-19 centre; snap should pull it up.
        val (e, tick) = engineWithClock()
        e.pieceY = O_PIECEy + 8f   // below target → snapTargetY will be above this
        tick()                      // gravity + contact → timer starts, beginSnapAnimation
        val yAfterContact = e.pieceY
        repeat(125) { tick() }
        assertTrue(
            "pieceY should move toward snap target (upward): was $yAfterContact, now ${e.pieceY}",
            e.pieceY < yAfterContact
        )
    }

    // ── user drag cancels snap → piece goes fluid ─────────────────────────

    @Test
    fun dragToOpenSpace_cancelsSanpAnimation_pieceGoesFluid() {
        val (e, tick) = engineWithClock()
        // Let the piece settle and start the timer.
        repeat(5) { tick() }
        assertTrue("snap animation should be running", e.getCollisionSolidity() > 0f)

        // Simulate the user dragging the piece upward far enough to break floor contact.
        // pieceY -= 200px puts the bottom block centre well above gridBottom-halfBlock.
        e.isDragging = true
        e.pieceY -= 200f
        tick()   // checkCollisions sees no contact while isDragging → resets snap + timers

        assertEquals("solidity should reset to 0 after drag to open space",
            0f, e.getCollisionSolidity(), 0.001f)
    }
}
