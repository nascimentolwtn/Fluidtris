package com.libuy.fluidtris

import org.junit.Assert.assertEquals
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
    //   left/right gx = 0, 1  ✓

    private val O_PIECE = 1
    private val O_PIECEx = GameConstants.GRID_LEFT  // 150f
    private val O_PIECEy = 1549f

    /** Engine with a fake clock advancing [msPerFrame] each [update] call. */
    private fun engine(
        msPerFrame: Long = 16L,
        onLocked: () -> Unit = {}
    ): GameEngine {
        var fakeTime = 0L
        val e = GameEngine(onPieceLocked = onLocked, onLineCleared = {})
        e.currentTimeMs = { fakeTime }
        e.resetGame(VW, VH)
        e.currentPiece = O_PIECE
        e.pieceRotation = 0f
        e.pieceX = O_PIECEx
        e.pieceY = O_PIECEy

        // Wrap update so fakeTime advances before each call.
        val realUpdate = e::update
        // Store the closure so callers can call e.update(VW, VH) normally — we expose the
        // time-advancing version through the returned engine.  Callers use the helper below.
        // (Direct e.update still works because currentTimeMs was already injected.)
        fakeTime = 0L
        // Advance clock whenever update is invoked by storing it in the engine closure:
        // simplest approach — just advance fakeTime before each test call via the tick helper.
        return e
    }

    /** Tick [n] frames, advancing the injected clock by [msPerFrame] each time. */
    private fun tick(e: GameEngine, n: Int, msPerFrame: Long = 16L) {
        // We need to advance the fake clock; since it's captured in a closure inside engine(),
        // we route through a shared mutable ref instead.  Re-implement here cleanly:
        repeat(n) { e.update(VW, VH) }
    }

    // ────────────────────────────────────────────────────────────────────────
    // To keep things simple, each test builds its own engine with a shared
    // mutable clock so update() can advance it.
    // ────────────────────────────────────────────────────────────────────────

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

    // ── lerpAngleDeg (pure math — no clock needed) ────────────────────────

    @Test
    fun lerpAngleDeg_midpoint() {
        assertEquals(45f, lerpAngleDeg(0f, 90f, 0.5f), 0.01f)
    }

    @Test
    fun lerpAngleDeg_shortestPath_acrossZero() {
        // 350° → 10°: shortest arc +20°, at t=0.5 result = 360° ≡ 0°
        assertEquals(0f, lerpAngleDeg(350f, 10f, 0.5f) % 360f, 0.01f)
    }

    @Test
    fun lerpAngleDeg_noChangeWhenAtTarget() {
        assertEquals(90f, lerpAngleDeg(90f, 90f, 1f), 0.001f)
    }

    // ── Y must not change while snap animation is running ────────────────

    @Test
    fun snapPull_doesNotChangeY_duringAnimation() {
        val (e, tick) = engineWithClock()

        // First tick: gravity moves Y by 2, then contact detected → timer starts.
        tick()
        val yAtContact = e.pieceY

        // Run 100 more ticks (1 600 ms simulated — well within the 3 s delay).
        repeat(100) { tick() }

        assertEquals(
            "pieceY must be stable during snap animation (Y pull would break contact → timer reset)",
            yAtContact, e.pieceY, 0.5f
        )
    }

    // ── lock timer must elapse and solidify the piece ─────────────────────

    @Test
    fun snapPull_pieceLocksAfterDelay() {
        var locked = false
        val (_, tick) = engineWithClock(onLocked = { locked = true })

        // LOCK_DELAY_MS = 3 000 ms / 16 ms per tick ≈ 188 ticks; 220 to be safe.
        repeat(220) { tick() }

        assertTrue(
            "piece should lock after LOCK_DELAY_MS (snap-pull Y bug would prevent this)",
            locked
        )
    }

    // ── X must drift toward the nearest grid column centre ────────────────

    @Test
    fun snapPull_driftsPieceX_towardGridCentre() {
        val (e, tick) = engineWithClock()
        e.pieceX = O_PIECEx + 20f   // 20 px right of aligned position
        val xBefore = e.pieceX

        // ~2 s simulated; pull is quadratic so pull is visible around t=0.67.
        repeat(125) { tick() }

        assertTrue(
            "pieceX should drift left toward grid column centre (was $xBefore, now ${e.pieceX})",
            e.pieceX < xBefore
        )
    }

    // ── rotation must lerp toward nearest 90° ─────────────────────────────

    @Test
    fun snapPull_rotationLerpsTowardNearest90() {
        val (e, tick) = engineWithClock()
        e.pieceRotation = 40f   // nearest target = 0°

        repeat(125) { tick() }

        assertTrue(
            "rotation should move from 40° toward 0° (was 40, now ${e.pieceRotation})",
            e.pieceRotation < 39f
        )
    }
}
