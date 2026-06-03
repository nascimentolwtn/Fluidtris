package com.libuy.fluidtris

import org.junit.Test
import org.junit.Assert.assertEquals

private const val EPS = 0.5f

class CollisionTest {

    private val iPiece = listOf(listOf(1, 1, 1, 1))          // 1×4
    private val oPiece = listOf(listOf(1, 1), listOf(1, 1))  // 2×2

    // At 0° the block centers must equal pieceX + (col+0.5)*100, pieceY + (row+0.5)*100
    @Test
    fun opiece_0deg_centers_are_at_expected_positions() {
        val centers = rotatedBlockCenters(oPiece, 0f, 0f, 0f)
        assertEquals(4, centers.size)
        assertPair(50f, 50f, centers[0])
        assertPair(150f, 50f, centers[1])
        assertPair(50f, 150f, centers[2])
        assertPair(150f, 150f, centers[3])
    }

    @Test
    fun ipiece_0deg_centers_form_horizontal_line() {
        val centers = rotatedBlockCenters(iPiece, 0f, 0f, 0f)
        assertEquals(4, centers.size)
        // All Y values must be equal (single row); X values spaced by 100
        val y = centers[0].second
        for ((x, cy) in centers) assertEquals(y, cy, EPS)
        assertPair(50f, 50f, centers[0])
        assertPair(150f, 50f, centers[1])
        assertPair(250f, 50f, centers[2])
        assertPair(350f, 50f, centers[3])
    }

    // At 90° CW the I-piece rotates around its center (200, 50).
    // All blocks land on X=200; Y values span from -100 to 200.
    @Test
    fun ipiece_90deg_all_blocks_share_same_x() {
        val centers = rotatedBlockCenters(iPiece, 0f, 0f, 90f)
        assertEquals(4, centers.size)
        for ((bx, _) in centers) assertEquals(200f, bx, EPS)
    }

    @Test
    fun ipiece_90deg_blocks_spaced_vertically() {
        val centers = rotatedBlockCenters(iPiece, 0f, 0f, 90f)
        val ys = centers.map { it.second }.sorted()
        for (i in 1 until ys.size) {
            assertEquals(100f, ys[i] - ys[i - 1], EPS)
        }
    }

    // 360° is identity: centers should match 0°
    @Test
    fun opiece_360deg_same_as_0deg() {
        val at0 = rotatedBlockCenters(oPiece, 100f, 200f, 0f)
        val at360 = rotatedBlockCenters(oPiece, 100f, 200f, 360f)
        assertEquals(at0.size, at360.size)
        for (i in at0.indices) {
            assertPair(at0[i].first, at0[i].second, at360[i])
        }
    }

    // pieceX/pieceY offset is applied correctly
    @Test
    fun ipiece_0deg_with_offset() {
        val dx = 500f; val dy = 300f
        val centered = rotatedBlockCenters(iPiece, 0f, 0f, 0f)
        val offset = rotatedBlockCenters(iPiece, dx, dy, 0f)
        assertEquals(centered.size, offset.size)
        for (i in centered.indices) {
            assertEquals(centered[i].first + dx, offset[i].first, EPS)
            assertEquals(centered[i].second + dy, offset[i].second, EPS)
        }
    }

    private fun assertPair(expectedX: Float, expectedY: Float, actual: Pair<Float, Float>) {
        assertEquals("x mismatch", expectedX, actual.first, EPS)
        assertEquals("y mismatch", expectedY, actual.second, EPS)
    }
}
