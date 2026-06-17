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
