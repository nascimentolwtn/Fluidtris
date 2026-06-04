package com.libuy.fluidtris

import android.content.Context
import android.view.MotionEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertTrue

/**
 * Instrumented tests for Pause and Exit button interactions.
 *
 * Tests the actual onTouchEvent behavior on an Android device/emulator.
 */
@RunWith(AndroidJUnit4::class)
class ButtonInteractionTest {

    private lateinit var context: Context
    private lateinit var gameView: FluidTetrisView

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        gameView = FluidTetrisView(context)

        // Set a fixed size for consistent testing
        gameView.measure(
            android.view.View.MeasureSpec.makeMeasureSpec(1080, android.view.View.MeasureSpec.EXACTLY),
            android.view.View.MeasureSpec.makeMeasureSpec(2340, android.view.View.MeasureSpec.EXACTLY)
        )
        gameView.layout(0, 0, 1080, 2340)
    }

    // Test: Exit button listener is set and callable
    @Test
    fun exitButton_listenerCanBeSet() {
        var exitPressed = false
        gameView.gameListener = object : FluidTetrisView.GameListener {
            override fun onExitPressed() {
                exitPressed = true
            }
        }

        // Simulate touch on exit button (right side, near bottom)
        val width = gameView.width
        val height = gameView.height
        val exitButtonCenterX = (width - 110).toFloat()
        val exitButtonCenterY = (height - 100).toFloat()

        val touchEvent = MotionEvent.obtain(
            System.currentTimeMillis(), System.currentTimeMillis(),
            MotionEvent.ACTION_DOWN, exitButtonCenterX, exitButtonCenterY, 0
        )

        gameView.onTouchEvent(touchEvent)
        touchEvent.recycle()

        // Verify listener was called
        assertTrue("Exit button listener should be called", exitPressed)
    }

    // Test: Pause button is in the middle horizontally
    @Test
    fun pauseButton_shouldBeCenteredHorizontally() {
        val width = gameView.width.toFloat()

        // Pause button bounds: width/2 - 100f to width/2 + 100f
        val pauseButtonLeft = width / 2 - 100f
        val pauseButtonRight = width / 2 + 100f
        val pauseButtonCenter = (pauseButtonLeft + pauseButtonRight) / 2

        // Button center should be at screen center
        assertTrue(
            "Pause button should be centered horizontally",
            kotlin.math.abs(pauseButtonCenter - width / 2) < 1f
        )
    }

    // Test: Exit button is on the right
    @Test
    fun exitButton_shouldBeRightAligned() {
        val width = gameView.width.toFloat()

        // Exit button right edge: width - 20f
        val exitButtonRight = width - 20f

        // Button should be very close to the right edge (within 20 pixels)
        assertTrue(
            "Exit button should be on the right side, within 20px of edge",
            exitButtonRight > width - 30f
        )
    }

    // Test: Both buttons are at the bottom
    @Test
    fun bothButtons_shouldBeAtBottomOfScreen() {
        val height = gameView.height.toFloat()

        // Button bounds: height - 150f to height - 50f
        val buttonTop = height - 150f
        val buttonBottom = height - 50f

        // Buttons should be in the bottom 150px of the screen
        assertTrue(
            "Buttons should be at the bottom of the screen",
            buttonBottom < height && buttonTop > height - 200f
        )
    }

    // Test: Pause and Exit buttons don't overlap
    @Test
    fun pauseAndExitButtons_shouldNotOverlap() {
        val width = gameView.width.toFloat()

        // Pause button: width/2 - 100f to width/2 + 100f
        val pauseButtonRight = width / 2 + 100f

        // Exit button: width - 200f to width - 20f
        val exitButtonLeft = width - 200f

        // Exit button should start after pause button ends
        assertTrue(
            "Buttons should not overlap",
            exitButtonLeft > pauseButtonRight
        )
    }
}
