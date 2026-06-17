package com.libuy.fluidtris

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Snap-animating pieces act as pre-solid obstacles: fluid pieces bounce off them.
 * Two fluid pieces pass through each other freely.
 */
class LivePieceCollisionTest {

    private val VW = 1080
    private val VH = 1920

    private fun engine(): GameEngine {
        val e = GameEngine(onPieceLocked = {}, onLineCleared = {})
        e.resetGame(VW, VH)
        return e
    }

    // A fluid piece overlapping a snap-animating piece should be pushed away.
    @Test
    fun fluidPieceBouncesOffAnimatingPeer() {
        val e = engine()
        e.fallingPieces[0].type = 4
        e.fallingPieces[0].color = GameConstants.PIECE_COLORS[4]
        e.fallingPieces[0].x = 400f
        e.fallingPieces[0].y = 600f
        e.fallingPieces[0].rotation = 0f
        // This piece is fluid — it should bounce

        val animating = ActivePiece(
            type = 4, color = GameConstants.PIECE_COLORS[4],
            x = 400f, y = 600f, rotation = 0f,
            isSnapAnimating = true,
            isWaitingToTurnRigidAtBottom = true,
            bottomCollisionTime = Long.MAX_VALUE   // won't expire this tick
        )
        e.fallingPieces.add(animating)

        e.update(VW, VH)

        assertTrue(
            "fluid piece should bounce off a snap-animating peer",
            e.fallingPieces.firstOrNull { !it.isSnapAnimating }?.isSlidingOnContact ?: false
        )
    }

    // Two fluid pieces must pass through each other — no bounce.
    @Test
    fun twoFluidPiecesPassThroughEachOther() {
        val e = engine()
        e.fallingPieces[0].type = 4
        e.fallingPieces[0].color = GameConstants.PIECE_COLORS[4]
        e.fallingPieces[0].x = 400f
        e.fallingPieces[0].y = 600f
        e.fallingPieces[0].rotation = 0f

        val second = ActivePiece(
            type = 4, color = GameConstants.PIECE_COLORS[4],
            x = 400f, y = 600f, rotation = 0f
        )
        e.fallingPieces.add(second)

        e.update(VW, VH)

        assertFalse(
            "fluid pieces should not collide with each other",
            e.fallingPieces.any { it.isSlidingOnContact }
        )
    }

    // A fluid piece resting cleanly on top of a snap-animating peer must start its own lock timer.
    @Test
    fun fluidPieceStartsLockTimerWhenRestingOnAnimatingPeer() {
        val e = GameEngine(onPieceLocked = {}, onLineCleared = {})
        var fakeTime = 0L
        e.currentTimeMs = { fakeTime }
        e.resetGame(VW, VH)

        val cellHeight = (VH - GameConstants.GRID_TOP - GameConstants.GRID_BOTTOM_MARGIN) / GameConstants.GRID_ROWS
        // cellWidth = (1080-150-150)/7 ≈ 111.4f; cellHeight = (1920-100-180)/20 = 82f

        // Piece A (O-piece, snap-animating): top blocks at grid row 18
        // pieceA_y + 50 = GRID_TOP + 18 * cellHeight → pieceA_y = 100 + 18*82 - 50 = 1526
        val pieceA = ActivePiece(
            type = 1, color = GameConstants.PIECE_COLORS[1],
            x = GameConstants.GRID_LEFT, y = GameConstants.GRID_TOP + 18f * cellHeight - 50f,
            rotation = 0f,
            isSnapAnimating = true,
            isWaitingToTurnRigidAtBottom = true,
            bottomCollisionTime = Long.MAX_VALUE   // won't expire this tick
        )

        // Piece B (O-piece, fluid): bottom blocks at grid row 17 — one row above piece A's top blocks
        // pieceB_y + 150 = GRID_TOP + 17.5 * cellHeight → pieceB_y = 100 + 17.5*82 - 150 = 1385
        e.fallingPieces[0].type = 1
        e.fallingPieces[0].color = GameConstants.PIECE_COLORS[1]
        e.fallingPieces[0].x = GameConstants.GRID_LEFT
        e.fallingPieces[0].y = GameConstants.GRID_TOP + 17.5f * cellHeight - 150f
        e.fallingPieces[0].rotation = 0f
        e.fallingPieces.add(pieceA)

        fakeTime += 16L
        e.update(VW, VH)

        val pieceB = e.fallingPieces.firstOrNull { it !== pieceA }
        assertTrue(
            "fluid piece should start lock timer when resting cleanly on snap-animating peer",
            pieceB?.isWaitingToTurnRigidAtPiece ?: false
        )
    }

    // The snap-animating piece itself must not be pushed back by a fluid peer.
    @Test
    fun animatingPieceIsNotPushedByFluidPeer() {
        val e = engine()
        e.fallingPieces[0].type = 4
        e.fallingPieces[0].color = GameConstants.PIECE_COLORS[4]
        e.fallingPieces[0].x = 400f
        e.fallingPieces[0].y = 600f
        e.fallingPieces[0].rotation = 0f
        e.fallingPieces[0].isSnapAnimating = true
        e.fallingPieces[0].isWaitingToTurnRigidAtBottom = true
        e.fallingPieces[0].bottomCollisionTime = Long.MAX_VALUE

        val fluid = ActivePiece(
            type = 4, color = GameConstants.PIECE_COLORS[4],
            x = 400f, y = 600f, rotation = 0f
        )
        e.fallingPieces.add(fluid)

        e.update(VW, VH)

        val stillAnimating = e.fallingPieces.firstOrNull { it.isSnapAnimating }
        assertFalse(
            "snap-animating piece should not be pushed by a fluid peer",
            stillAnimating?.isSlidingOnContact ?: false
        )
    }
}
