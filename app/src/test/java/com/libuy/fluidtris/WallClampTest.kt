package com.libuy.fluidtris

import org.junit.Assert.assertEquals
import org.junit.Test

class WallClampTest {

    private val pieceSize = 100f
    private val leftWall = 50f
    private val rightWall = 750f

    // I-piece un-rotated: 4 wide × 1 tall
    private val iHorizontal = listOf(listOf(1, 1, 1, 1))

    // I-piece rotated 90°: 1 wide × 4 tall
    private val iVertical = listOf(listOf(1), listOf(1), listOf(1), listOf(1))

    @Test
    fun leftWallClampsToFirstOccupiedColumn() {
        val result = clampPieceX(iHorizontal, -50f, leftWall, rightWall, pieceSize)
        assertEquals(leftWall, result, 0.001f)
    }

    @Test
    fun rightWallClamps4WideByFourBlockWidths() {
        val result = clampPieceX(iHorizontal, rightWall, leftWall, rightWall, pieceSize)
        assertEquals(rightWall - 4 * pieceSize, result, 0.001f)
    }

    @Test
    fun rightWallClamps1WideByOneBlockWidth() {
        // Core regression: a 1-wide (rotated) piece must stop 1 block from the right wall,
        // not 4 blocks — which would happen if the un-rotated 4-wide shape were used.
        val result = clampPieceX(iVertical, rightWall, leftWall, rightWall, pieceSize)
        assertEquals(rightWall - pieceSize, result, 0.001f)
    }

    @Test
    fun noClampWhenInsideBounds() {
        val inBounds = leftWall + 50f
        assertEquals(inBounds, clampPieceX(iHorizontal, inBounds, leftWall, rightWall, pieceSize), 0.001f)
    }
}
