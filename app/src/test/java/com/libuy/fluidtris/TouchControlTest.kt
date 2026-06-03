package com.libuy.fluidtris

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests for the three pure functions that back the touch-control overhaul:
 *   hitCellFromTouch     – rotation-aware hit detection
 *   effectiveVerticalDrag – upward drag attenuation
 *   rotationDeltaFromDrag – torque-based rotation
 */
class TouchControlTest {

    // -------------------------------------------------------------------------
    // hitCellFromTouch
    // -------------------------------------------------------------------------

    // T-piece: [[1,1,1],[0,1,0]] – 3 cols × 2 rows
    private val tShape = listOf(listOf(1, 1, 1), listOf(0, 1, 0))

    // I-piece: [[1,1,1,1]] – 4 cols × 1 row
    private val iShape = listOf(listOf(1, 1, 1, 1))

    private val pieceX = 100f
    private val pieceY = 200f
    private val pieceSize = 100f

    // T-piece rotation center at 0°: (100+150, 200+100) = (250, 300)
    // Block pixel origins: (row, col) → (pieceX + col*100, pieceY + row*100)

    @Test
    fun hitCell_zeroRotation_leftBlock() {
        // Block (0,0) spans x[100,200] y[200,300] – touch near its center
        val result = hitCellFromTouch(150f, 250f, pieceX, pieceY, 0f, tShape, pieceSize)
        assertEquals(0 to 0, result)
    }

    @Test
    fun hitCell_zeroRotation_centerBlock() {
        // Block (0,1) – the defined movement block for T
        val result = hitCellFromTouch(250f, 250f, pieceX, pieceY, 0f, tShape, pieceSize)
        assertEquals(0 to 1, result)
    }

    @Test
    fun hitCell_zeroRotation_stemBlock() {
        // Block (1,1) – the stem below center
        val result = hitCellFromTouch(250f, 350f, pieceX, pieceY, 0f, tShape, pieceSize)
        assertEquals(1 to 1, result)
    }

    @Test
    fun hitCell_zeroRotation_emptyCell_returnsNull() {
        // (1,0) is 0 in the T-piece shape – no block there
        val result = hitCellFromTouch(150f, 350f, pieceX, pieceY, 0f, tShape, pieceSize)
        assertNull(result)
    }

    @Test
    fun hitCell_zeroRotation_outsidePiece_returnsNull() {
        // Touch far to the left of the piece
        val result = hitCellFromTouch(10f, 250f, pieceX, pieceY, 0f, tShape, pieceSize)
        assertNull(result)
    }

    @Test
    fun hitCell_zeroRotation_touchJustInsideRightEdge() {
        // One pixel inside the right edge of block (0,2): x = pieceX + 2*100 + 99 = 399
        val result = hitCellFromTouch(399f, 250f, pieceX, pieceY, 0f, tShape, pieceSize)
        assertEquals(0 to 2, result)
    }

    @Test
    fun hitCell_zeroRotation_touchJustOutsideRightEdge() {
        // One pixel past the right edge of block (0,2): x = 400 (= pieceX + 3*100)
        val result = hitCellFromTouch(400f, 250f, pieceX, pieceY, 0f, tShape, pieceSize)
        assertNull(result)
    }

    // I-piece rotation center at 0°: (100+200, 200+50) = (300, 250)
    // After 90° CW, each canonical block (0,c) maps to screen position:
    //   bx = 300,  by = 250 + (c*100+50 - 200) = 50 + c*100
    //   i.e. block (0,0) → (300, 100), (0,1) → (300, 200), (0,2) → (300, 300), (0,3) → (300, 400)

    @Test
    fun hitCell_90degRotation_mapsVisualPositionToCanonicalCell() {
        // Touching where block (0,1) appears after 90° rotation → canonical (0,1)
        val result = hitCellFromTouch(300f, 200f, pieceX, pieceY, 90f, iShape, pieceSize)
        assertEquals(0 to 1, result)
    }

    @Test
    fun hitCell_90degRotation_endBlock() {
        // Touching where block (0,3) appears after 90° rotation → canonical (0,3)
        val result = hitCellFromTouch(300f, 400f, pieceX, pieceY, 90f, iShape, pieceSize)
        assertEquals(0 to 3, result)
    }

    @Test
    fun hitCell_90degRotation_outsidePiece_returnsNull() {
        // The rotated I-piece column is at x≈300; touching x=150 misses entirely
        val result = hitCellFromTouch(150f, 250f, pieceX, pieceY, 90f, iShape, pieceSize)
        assertNull(result)
    }

    // -------------------------------------------------------------------------
    // effectiveVerticalDrag
    // -------------------------------------------------------------------------

    @Test
    fun effectiveDrag_downward_appliedInFull() {
        assertEquals(80f, effectiveVerticalDrag(80f, 0.4f), 0.001f)
    }

    @Test
    fun effectiveDrag_upward_isAttenuated() {
        assertEquals(-100f * 0.4f, effectiveVerticalDrag(-100f, 0.4f), 0.001f)
    }

    @Test
    fun effectiveDrag_zero_isZero() {
        assertEquals(0f, effectiveVerticalDrag(0f, 0.4f), 0.001f)
    }

    @Test
    fun effectiveDrag_upward_cannotExceedGravity() {
        // With factor 0.4 the attenuated drag is always less than the raw value,
        // so gravity (9.8/frame) is never fully cancelled by a single event.
        val rawUpward = -100f
        val effective = effectiveVerticalDrag(rawUpward, 0.4f)
        assert(effective > rawUpward) { "Effective drag must be less negative than raw" }
    }

    // -------------------------------------------------------------------------
    // rotationDeltaFromDrag
    // -------------------------------------------------------------------------
    // Convention: caller does  pieceRotation -= rotationDeltaFromDrag(...)
    // Positive result  → pieceRotation decreases → CCW in Android canvas (positive = CW)
    // Negative result  → pieceRotation increases → CW

    private val sensitivity = 30f

    // Block directly to the right of rotation center: vx = rcx - bx < 0
    // Dragging it downward should produce CW rotation (negative delta).
    @Test
    fun rotationDelta_blockRight_dragDown_isCW() {
        val vx = -150f  // block is 150px to the right of center
        val vy = 0f
        val delta = rotationDeltaFromDrag(vx, vy, dx = 0f, dy = 10f, sensitivity)
        assert(delta < 0f) { "Expected CW (negative) delta, got $delta" }
    }

    // Block directly to the left: dragging it downward should be CCW (positive delta).
    @Test
    fun rotationDelta_blockLeft_dragDown_isCCW() {
        val vx = 150f   // block is 150px to the left of center
        val vy = 0f
        val delta = rotationDeltaFromDrag(vx, vy, dx = 0f, dy = 10f, sensitivity)
        assert(delta > 0f) { "Expected CCW (positive) delta, got $delta" }
    }

    // Block directly above center (by < rcy → vy > 0): dragging right should be CW.
    @Test
    fun rotationDelta_blockAbove_dragRight_isCW() {
        val vx = 0f
        val vy = 50f    // block is 50px above center
        val delta = rotationDeltaFromDrag(vx, vy, dx = 10f, dy = 0f, sensitivity)
        assert(delta < 0f) { "Expected CW (negative) delta, got $delta" }
    }

    // Purely radial drag (along the line from block to center) produces no torque.
    @Test
    fun rotationDelta_radialDrag_isZero() {
        val vx = -150f
        val vy = 0f
        // Drag in the same direction as the vector from block to center (rightward)
        val delta = rotationDeltaFromDrag(vx, vy, dx = 10f, dy = 0f, sensitivity)
        assertEquals(0f, delta, 0.001f)
    }

    // Left–right symmetry: mirroring both the block position and the drag negates the delta.
    @Test
    fun rotationDelta_leftRightSymmetry() {
        val deltaRight = rotationDeltaFromDrag(-100f, 0f, 0f, 10f, sensitivity)
        val deltaLeft  = rotationDeltaFromDrag( 100f, 0f, 0f, 10f, sensitivity)
        assertEquals(-deltaRight, deltaLeft, 0.001f)
    }
}
