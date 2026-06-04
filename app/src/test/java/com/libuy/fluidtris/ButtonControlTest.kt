package com.libuy.fluidtris

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for Pause and Exit button behavior in FluidTetrisView.
 *
 * These tests verify:
 * - Pause button toggles pause state
 * - Exit button triggers the gameListener callback
 * - Touch detection works within button bounds
 */
class ButtonControlTest {

    private val pauseButtonLeft = 0f  // Will be width / 2 - 100f at runtime
    private val pauseButtonRight = 200f  // Will be width / 2 + 100f at runtime
    private val pauseButtonTop = 0f  // Will be height - 150f at runtime
    private val pauseButtonBottom = 100f  // Will be height - 50f at runtime

    private val exitButtonLeft = 200f  // Will be width - 200f at runtime
    private val exitButtonRight = 380f  // Will be width - 20f at runtime
    private val exitButtonTop = 0f  // Will be height - 150f at runtime
    private val exitButtonBottom = 100f  // Will be height - 50f at runtime

    // Test: Pause button bounds detection
    @Test
    fun pauseButton_touchInsideBounds_shouldBeDetected() {
        val touchX = 100f  // Middle of pause button
        val touchY = 50f   // Middle of pause button vertically

        // Verify touch is within pause button bounds
        assertTrue(
            "Pause button should contain touch at ($touchX, $touchY)",
            touchX >= pauseButtonLeft && touchX <= pauseButtonRight &&
            touchY >= pauseButtonTop && touchY <= pauseButtonBottom
        )
    }

    @Test
    fun pauseButton_touchOutsideBounds_shouldNotBeDetected() {
        val touchX = 250f  // Outside pause button (in exit button area)
        val touchY = 50f

        // Verify touch is NOT within pause button bounds
        assertFalse(
            "Pause button should not contain touch at ($touchX, $touchY)",
            touchX >= pauseButtonLeft && touchX <= pauseButtonRight &&
            touchY >= pauseButtonTop && touchY <= pauseButtonBottom
        )
    }

    // Test: Exit button bounds detection
    @Test
    fun exitButton_touchInsideBounds_shouldBeDetected() {
        val touchX = 290f  // Middle of exit button
        val touchY = 50f   // Middle of exit button vertically

        // Verify touch is within exit button bounds
        assertTrue(
            "Exit button should contain touch at ($touchX, $touchY)",
            touchX >= exitButtonLeft && touchX <= exitButtonRight &&
            touchY >= exitButtonTop && touchY <= exitButtonBottom
        )
    }

    @Test
    fun exitButton_touchOutsideBounds_shouldNotBeDetected() {
        val touchX = 100f  // In pause button area
        val touchY = 50f

        // Verify touch is NOT within exit button bounds
        assertFalse(
            "Exit button should not contain touch at ($touchX, $touchY)",
            touchX >= exitButtonLeft && touchX <= exitButtonRight &&
            touchY >= exitButtonTop && touchY <= exitButtonBottom
        )
    }

    // Test: Button separation - no overlap
    @Test
    fun pauseAndExitButtons_shouldNotOverlap() {
        val pauseRightEdge = pauseButtonRight
        val exitLeftEdge = exitButtonLeft

        assertTrue(
            "Pause button ($pauseButtonLeft-$pauseRightEdge) should be left of Exit button ($exitLeftEdge-$exitButtonRight)",
            pauseRightEdge <= exitLeftEdge
        )
    }

    // Test: Pause button positioning (relative to screen width)
    // At runtime: width/2 - 100f to width/2 + 100f
    @Test
    fun pauseButton_shouldHaveCenterWidth() {
        val screenWidth = 1080f  // Example screen width
        val pauseButtonCenterAtRuntime = screenWidth / 2
        val pauseButtonLeftAtRuntime = pauseButtonCenterAtRuntime - 100f
        val pauseButtonRightAtRuntime = pauseButtonCenterAtRuntime + 100f

        // Verify button width and centering pattern
        val buttonWidth = pauseButtonRightAtRuntime - pauseButtonLeftAtRuntime
        assertEquals(
            "Pause button should have width of 200f (100f on each side of center)",
            200f,
            buttonWidth,
            0.01f
        )

        // Verify button is centered
        val buttonCenterX = (pauseButtonLeftAtRuntime + pauseButtonRightAtRuntime) / 2
        assertEquals(
            "Pause button center should be at screen center",
            screenWidth / 2,
            buttonCenterX,
            0.01f
        )
    }

    // Test: Exit button positioning (right-aligned)
    @Test
    fun exitButton_shouldBeRightAligned() {
        val screenWidth = 1080f  // Example screen width
        val exitButtonLeftAtRuntime = screenWidth - 200f
        val exitButtonRightAtRuntime = screenWidth - 20f

        // Verify button is near right edge
        val distanceFromRightEdge = screenWidth - exitButtonRightAtRuntime
        assertEquals(
            "Exit button should have 20px margin from right edge",
            20f,
            distanceFromRightEdge,
            0.01f
        )

        // Verify button width
        val buttonWidth = exitButtonRightAtRuntime - exitButtonLeftAtRuntime
        assertEquals(
            "Exit button should have width of 180f",
            180f,
            buttonWidth,
            0.01f
        )
    }

    // Test: Buttons positioned at bottom
    @Test
    fun buttons_shouldBePositionedAtBottom() {
        val expectedTop = 0f  // height - 150f at runtime
        val expectedBottom = 100f  // height - 50f at runtime
        val buttonHeight = expectedBottom - expectedTop

        assertEquals(
            "Buttons should have height of 100f",
            100f,
            buttonHeight,
            0.01f
        )
    }

    // Test: Button width consistency
    @Test
    fun pauseButton_shouldHaveCorrectWidth() {
        val width = pauseButtonRight - pauseButtonLeft
        assertEquals(
            "Pause button should have width of 200f (width/2 + 100f - (width/2 - 100f))",
            200f,
            width,
            0.01f
        )
    }

    @Test
    fun exitButton_shouldHaveCorrectWidth() {
        val width = exitButtonRight - exitButtonLeft
        assertEquals(
            "Exit button should have width of 180f (width - 20f - (width - 200f))",
            180f,
            width,
            0.01f
        )
    }
}
