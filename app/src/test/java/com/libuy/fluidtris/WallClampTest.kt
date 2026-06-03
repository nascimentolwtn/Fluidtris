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
        // Core regression: a 1-wide (rotated) piece must stop exactly at the right wall,
        // not 4 blocks in — which would happen if the un-rotated 4-wide shape were used.
        val result = clampPieceX(iVertical, rightWall, leftWall, rightWall, pieceSize)
        assertEquals(rightWall - pieceSize, result, 0.001f)
    }

    @Test
    fun noClampWhenInsideBounds() {
        val inBounds = leftWall + 50f
        assertEquals(inBounds, clampPieceX(iHorizontal, inBounds, leftWall, rightWall, pieceSize), 0.001f)
    }

    // --- clampPieceXByCenters: mirrors the canvas.rotate() visual extents ---

    // For a vertical I piece (iHorizontal rotated 90°), all blocks land at bx = pieceX+200.
    // Visual extent: pieceX+150 … pieceX+250. Clamping must use these actual screen positions.

    @Test
    fun verticalIPieceCanReachLeftWall() {
        // pieceX=-200: all block centers at bx=0, visual left edge = -50 < leftWall=50
        val centers = rotatedBlockCenters(iHorizontal, -200f, 0f, 90f)
        val result = clampPieceXByCenters(centers, -200f, leftWall, rightWall, pieceSize / 2)
        // Expected: visual left edge == leftWall → bx=100 → pieceX=-100
        assertEquals(-100f, result, 0.001f)
    }

    @Test
    fun verticalIPieceCanReachRightWall() {
        // pieceX=900: all block centers at bx=1100, visual right edge=1150 > rightWall=750
        val centers = rotatedBlockCenters(iHorizontal, 900f, 0f, 90f)
        val result = clampPieceXByCenters(centers, 900f, leftWall, rightWall, pieceSize / 2)
        // Expected: visual right edge == rightWall → bx=700 → pieceX=500
        assertEquals(500f, result, 0.001f)
    }

    @Test
    fun horizontalIPieceLeftWallMatchesByCenters() {
        // 0° rotation: block centers at pieceX+50, +150, +250, +350; left edge = pieceX
        val centers = rotatedBlockCenters(iHorizontal, -50f, 0f, 0f)
        val result = clampPieceXByCenters(centers, -50f, leftWall, rightWall, pieceSize / 2)
        assertEquals(leftWall, result, 0.001f)
    }

    @Test
    fun noCenterClampWhenInsideBounds() {
        val centers = rotatedBlockCenters(iHorizontal, leftWall + 100f, 0f, 0f)
        val result = clampPieceXByCenters(centers, leftWall + 100f, leftWall, rightWall, pieceSize / 2)
        assertEquals(leftWall + 100f, result, 0.001f)
    }
}
