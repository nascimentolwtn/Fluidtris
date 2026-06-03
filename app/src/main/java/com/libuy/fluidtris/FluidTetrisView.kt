package com.libuy.fluidtris

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

class FluidTetrisView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        style = Paint.Style.FILL
    }
    private val jellyRect = RectF()

    private val backgroundBitmap: Bitmap =
        BitmapFactory.decodeResource(resources, R.drawable.game_background)

    private var pieceX = 0f
    private var pieceY = 0f
    private var pieceRotation = 0f
    private var isDragging = false
    private var isDraggingCenter = false
    private var touchedBlockRow = 0
    private var touchedBlockCol = 0
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    // Spring simulation variables
    private var springForceX = 0f
    private var springForceY = 0f
    private val springConstant = 0.2f
    private val damping = 0.9f

    // Gravity effect
    private val gravity = 2.0f  // px/frame; at 60 fps a piece takes ~16 s to cross the grid
    private var velocityY = 0f
    private val upwardDragFactor = 0.4f   // how much upward drag counteracts gravity
    private val rotationSensitivity = 30f // degrees per unit of torque

    // Game grid: 7 columns by 20 lines
    private val gridColumns = 7
    private val gridRows = 20
    private val grid = Array(gridRows) { Array<Int?>(gridColumns) { null } }
    private var score = 0
    private var highScore = 0

    // Tetris pieces
    private val pieces = listOf(
        listOf(listOf(1, 1, 1, 1)), // I
        listOf(listOf(1, 1), listOf(1, 1)), // O
        listOf(listOf(1, 1, 1), listOf(0, 1, 0)), // T
        listOf(listOf(1, 1, 1), listOf(1, 0, 0)), // L
        listOf(listOf(1, 1, 1), listOf(0, 0, 1)), // J
        listOf(listOf(1, 1, 0), listOf(0, 1, 1)), // S
        listOf(listOf(0, 1, 1), listOf(1, 1, 0))  // Z
    )

    private val pieceColors = listOf(
        Color.CYAN, // I
        Color.YELLOW, // O
        Color.MAGENTA, // T
        Color.RED, // L
        Color.BLUE, // J
        Color.GREEN, // S
        Color.RED // Z
    )

    // (row, col) pairs in the canonical shape that trigger movement; all other blocks trigger rotation
    private val pieceCenterCells: List<Set<Pair<Int, Int>>> = listOf(
        setOf(0 to 1, 0 to 2),                         // I – both middle blocks
        setOf(0 to 0, 0 to 1, 1 to 0, 1 to 1),         // O – whole piece is movement zone
        setOf(0 to 1),                                  // T – middle of top bar
        setOf(0 to 1),                                  // L – middle of long bar
        setOf(0 to 1),                                  // J – middle of long bar
        setOf(0 to 1),                                  // S – top middle block
        setOf(0 to 1),                                  // Z – top middle block
    )

    private var currentPiece = 0
    private var currentPieceColor = pieceColors[0]
    private var nextPiece = 1
    private var nextPieceColor = pieceColors[1]

    // Sound effects
    private var mediaPlayer: MediaPlayer? = null
    private var rigidSoundPlayer: MediaPlayer? = null

    // Game state
    private var isPaused = false
    private var isGameOver = false
    private var wasManuallyPausedBeforeSystemPause = false

    // Game loop
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            update()
            handler.postDelayed(this, 16) // 60 FPS
        }
    }

    // Add new variables to track collision times
    private var bottomCollisionTime = 0L
    private var pieceCollisionTime = 0L
    private var isWaitingToTurnRigidAtBottom = false
    private var isWaitingToTurnRigidAtPiece = false

    init {
        // Initialize sound effects
        mediaPlayer = MediaPlayer.create(context, R.raw.move_sound)
        rigidSoundPlayer = MediaPlayer.create(context, R.raw.rigid_sound)
        // Initialize game state
        resetGame()
        // Start the game loop
        handler.post(updateRunnable)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw background
        canvas.drawBitmap(backgroundBitmap, null,
            RectF(0f, 0f, width.toFloat(), height.toFloat()), null)

        // Define grid area (centered within the "glass" dark area of the background)
        val gridLeft = 150f
        val gridTop = 100f
        val gridRight = width - 150f
        val gridBottom = height - 180f
        val cellWidth = (gridRight - gridLeft) / gridColumns
        val cellHeight = (gridBottom - gridTop) / gridRows

        // Draw grid border
        paint.color = Color.BLACK
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRect(gridLeft, gridTop, gridRight, gridBottom, paint)
        paint.style = Paint.Style.FILL

        // Draw the grid
        for (i in 0 until gridRows) {
            for (j in 0 until gridColumns) {
                grid[i][j]?.let { color ->
                    paint.color = color
                    canvas.drawRect(gridLeft + j * cellWidth, gridTop + i * cellHeight, gridLeft + (j + 1) * cellWidth, gridTop + (i + 1) * cellHeight, paint)
                    // Draw border
                    paint.color = Color.BLACK
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 2f
                    canvas.drawRect(gridLeft + j * cellWidth, gridTop + i * cellHeight, gridLeft + (j + 1) * cellWidth, gridTop + (i + 1) * cellHeight, paint)
                    paint.style = Paint.Style.FILL
                }
            }
        }

        // Draw the current piece
        val currentPieceShape = pieces[currentPiece]
        val pieceSize = 100f
        canvas.save()
        canvas.rotate(pieceRotation, pieceX + (currentPieceShape[0].size * pieceSize) / 2, pieceY + (currentPieceShape.size * pieceSize) / 2)
        drawJellyPiece(canvas, currentPieceShape, pieceX, pieceY, currentPieceColor, getCollisionSolidity())
        canvas.restore()

        // Draw the next piece preview
        val previewX = width - 200f
        val previewY = 50f
        val previewPath = Path()
        previewPath.moveTo(previewX, previewY)
        previewPath.lineTo(previewX + pieceSize, previewY)
        previewPath.lineTo(previewX + pieceSize, previewY + pieceSize)
        previewPath.lineTo(previewX, previewY + pieceSize)
        previewPath.close()
        paint.color = nextPieceColor
        canvas.drawPath(previewPath, paint)
        // Draw border
        paint.color = Color.BLACK
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawPath(previewPath, paint)
        paint.style = Paint.Style.FILL

        // Draw the score and high score with semi-transparent background
        paint.color = Color.argb(180, 20, 60, 100)  // Dark teal overlay
        canvas.drawRect(10f, 5f, 400f, 95f, paint)
        paint.color = Color.argb(255, 100, 220, 200)  // Cyan-teal text
        paint.textSize = 40f
        canvas.drawText("Score: $score", 20f, 40f, paint)
        canvas.drawText("High Score: $highScore", 20f, 80f, paint)

        // Draw the new game and pause buttons with thematic colors
        paint.color = Color.argb(200, 50, 150, 130)  // Teal button
        canvas.drawRect(20f, height - 150f, 200f, height - 50f, paint)
        paint.color = Color.argb(255, 200, 240, 230)  // Light text
        paint.textSize = 30f
        canvas.drawText("New Game", 35f, height - 100f, paint)

        paint.color = Color.argb(200, 100, 150, 180)  // Blue button
        canvas.drawRect(width - 200f, height - 150f, width - 20f, height - 50f, paint)
        paint.color = Color.argb(255, 200, 240, 230)  // Light text
        canvas.drawText("Pause", width - 160f, height - 100f, paint)

        // Draw game over screen if game is over
        if (isGameOver) {
            // Semi-transparent overlay
            paint.color = Color.argb(150, 20, 40, 80)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

            paint.color = Color.argb(255, 150, 200, 220)  // Light blue text
            paint.textSize = 80f
            canvas.drawText("Game Over", width / 2 - 250f, height / 2 - 50f, paint)
            paint.textSize = 60f
            paint.color = Color.argb(255, 120, 200, 220)
            canvas.drawText("High Score: $highScore", width / 2 - 250f, height / 2 + 80f, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val hitCell = getHitCell(event.x, event.y)
                if (!isPaused && hitCell != null) {
                    isDragging = true
                    isDraggingCenter = (hitCell in pieceCenterCells[currentPiece])
                    touchedBlockRow = hitCell.first
                    touchedBlockCol = hitCell.second
                    lastTouchX = event.x
                    lastTouchY = event.y
                    return true
                }
                // Check if the new game button is pressed
                if (event.x in 20f..200f && event.y in (height - 150f)..(height - 50f)) {
                    resetGame()
                    return true
                }
                // Check if the pause button is pressed
                if (event.x in (width - 200f)..(width - 20f) && event.y in (height - 150f)..(height - 50f)) {
                    isPaused = !isPaused
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    var appliedDx = 0f
                    var appliedDy = 0f
                    val prevRotation = pieceRotation

                    if (isDraggingCenter) {
                        appliedDx = dx
                        appliedDy = effectiveVerticalDrag(dy, upwardDragFactor)
                        pieceX += appliedDx
                        pieceY += appliedDy
                        keepPiecesInsideWalls()
                    } else {
                        // Torque-based rotation: cross product of (block→center) with drag vector
                        val shape = pieces[currentPiece]
                        val pieceSize = 100f
                        val rcx = pieceX + shape[0].size * pieceSize / 2f
                        val rcy = pieceY + shape.size * pieceSize / 2f
                        val angle = pieceRotation * Math.PI / 180.0
                        val cosA = cos(angle)
                        val sinA = sin(angle)
                        val bux = pieceX + (touchedBlockCol + 0.5f) * pieceSize
                        val buy = pieceY + (touchedBlockRow + 0.5f) * pieceSize
                        val bdx = (bux - rcx).toDouble()
                        val bdy = (buy - rcy).toDouble()
                        val bx = (rcx + bdx * cosA - bdy * sinA).toFloat()
                        val by = (rcy + bdx * sinA + bdy * cosA).toFloat()
                        val vx = rcx - bx
                        val vy = rcy - by
                        pieceRotation -= rotationDeltaFromDrag(vx, vy, dx, dy, rotationSensitivity)
                    }

                    lastTouchX = event.x
                    lastTouchY = event.y

                    // Check for collision and update grid
                    val cellWidth = (width - 300f) / gridColumns
                    val cellHeight = (height - 280f) / gridRows
                    val gridLeft = 150f
                    val gridTop = 100f
                    val cellX = ((pieceX - gridLeft) / cellWidth).toInt()
                    val cellY = ((pieceY - gridTop) / cellHeight).toInt()
                    if (cellX in 0 until gridColumns && cellY in 0 until gridRows) {

                        if (isPieceAtBottom()) {
                            isDragging = false
                            collideAtBottom(cellWidth, cellHeight)
                        }

                        if (isPieceCollidingWithAnotherPiece(cellWidth, cellHeight)) {
                            pieceX -= appliedDx
                            pieceY -= appliedDy
                            pieceRotation = prevRotation
                            isDragging = false
                        }
                    }
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP -> {
                if (!isPaused && isDragging) {
                    isDragging = false
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (!hasWindowFocus) {
            wasManuallyPausedBeforeSystemPause = isPaused
            isPaused = true
        } else {
            isPaused = wasManuallyPausedBeforeSystemPause
        }
    }

    private fun getCollisionSolidity(): Float {
        val elapsed = when {
            isWaitingToTurnRigidAtBottom -> System.currentTimeMillis() - bottomCollisionTime
            isWaitingToTurnRigidAtPiece -> System.currentTimeMillis() - pieceCollisionTime
            else -> return 0f
        }
        val t = (elapsed / 3000f).coerceIn(0f, 1f)
        // Oscillate with sin that trends toward solid (1.0) as t → 1
        val oscillation = sin(t * Math.PI * 6.0).toFloat()
        return (t + oscillation * (1f - t) * 0.6f).coerceIn(0f, 1f)
    }

    private fun drawJellyPiece(
        canvas: Canvas, shape: List<List<Int>>,
        startX: Float, startY: Float, color: Int,
        solidity: Float, blockSize: Float = 100f
    ) {
        paint.isAntiAlias = true
        val jelly = 1f - solidity
        val cornerRadius = jelly * blockSize * 0.38f + 4f
        val expand = jelly * blockSize * 0.06f
        val bridgeInset = blockSize * (0.12f + solidity * 0.14f)

        paint.color = color
        paint.style = Paint.Style.FILL

        // Bridges fill concave gaps between rounded adjacent blocks
        for (row in shape.indices) {
            for (col in shape[row].indices) {
                if (shape[row][col] != 1) continue
                val x = startX + col * blockSize
                val y = startY + row * blockSize
                if (col + 1 < shape[row].size && shape[row][col + 1] == 1) {
                    jellyRect.set(x + blockSize * 0.4f, y + bridgeInset, x + blockSize * 1.6f, y + blockSize - bridgeInset)
                    canvas.drawRoundRect(jellyRect, cornerRadius * 0.4f, cornerRadius * 0.4f, paint)
                }
                if (row + 1 < shape.size && shape[row + 1][col] == 1) {
                    jellyRect.set(x + bridgeInset, y + blockSize * 0.4f, x + blockSize - bridgeInset, y + blockSize * 1.6f)
                    canvas.drawRoundRect(jellyRect, cornerRadius * 0.4f, cornerRadius * 0.4f, paint)
                }
            }
        }

        // Blocks drawn on top of bridges
        for (row in shape.indices) {
            for (col in shape[row].indices) {
                if (shape[row][col] != 1) continue
                val x = startX + col * blockSize
                val y = startY + row * blockSize

                paint.color = color
                jellyRect.set(x - expand, y - expand, x + blockSize + expand, y + blockSize + expand)
                canvas.drawRoundRect(jellyRect, cornerRadius, cornerRadius, paint)

                // Bright oval highlight gives jelly sheen
                if (jelly > 0.05f) {
                    paint.color = Color.argb((jelly * 90).toInt(), 255, 255, 255)
                    val hlR = blockSize * 0.18f * jelly
                    jellyRect.set(x + blockSize * 0.18f, y + blockSize * 0.18f,
                        x + blockSize * 0.18f + hlR * 2, y + blockSize * 0.18f + hlR * 2)
                    canvas.drawOval(jellyRect, paint)
                }

                // Block border fades in as piece solidifies
                if (solidity > 0.15f) {
                    paint.color = Color.argb((solidity * 210).toInt(), 0, 0, 0)
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 2f
                    jellyRect.set(x, y, x + blockSize, y + blockSize)
                    canvas.drawRoundRect(jellyRect, 4f, 4f, paint)
                    paint.style = Paint.Style.FILL
                }
            }
        }

        paint.isAntiAlias = false
    }

    private fun getHitCell(touchX: Float, touchY: Float): Pair<Int, Int>? =
        hitCellFromTouch(touchX, touchY, pieceX, pieceY, pieceRotation, pieces[currentPiece])

    private fun checkLines() {
        var linesCleared = 0
        for (i in gridRows - 1 downTo 0) {
            if (grid[i].all { it != null }) {
                // Remove the line
                for (j in i downTo 1) {
                    grid[j] = grid[j - 1].clone()
                }
                grid[0] = Array(gridColumns) { null }
                linesCleared++
            }
        }
        if (linesCleared > 0) {
            score += linesCleared * 100
            // Update high score if necessary
            if (score > highScore) {
                highScore = score
            }
            // Play sound effect for line clear
            mediaPlayer?.start()
        }
    }

    private fun checkCollisions() {
        // Calculate grid cell size (matches grid boundaries: left=150f, right=width-150f, top=100f, bottom=height-180f)
        val cellWidth = (width - 300f) / gridColumns
        val cellHeight = (height - 280f) / gridRows

        // Check for collision with the bottom
        if (isPieceAtBottom()) {
            if (!isWaitingToTurnRigidAtBottom) {
                isWaitingToTurnRigidAtBottom = true
                bottomCollisionTime = System.currentTimeMillis() // Start the timer
            }
        } else {
            // Reset waiting state if not colliding with the bottom
            isWaitingToTurnRigidAtBottom = false;
        }

        // Check if the bottom of the piece is at or below the top of the grid cell
        if (isPieceCollidingWithAnotherPiece(cellWidth, cellHeight)) {
            if (!isWaitingToTurnRigidAtPiece) {
                isWaitingToTurnRigidAtPiece = true
                pieceCollisionTime = System.currentTimeMillis() // Start the timer
            }
        } else {
            // Reset waiting state if not colliding with another piece
            isWaitingToTurnRigidAtPiece = false;
        }
    }

    private fun turnPieceRigid() {
        // Play sound effect for rigid transformation
        rigidSoundPlayer?.start()

        // Normalize rotation to 0, 90, 180, or 360 degrees
        pieceRotation = when {
            pieceRotation < 0 -> 360f + pieceRotation % 360 // Normalize to positive
            pieceRotation >= 360 -> pieceRotation % 360 // Normalize to 0-360
            pieceRotation % 90 != 0f -> (pieceRotation / 90).toInt() * 90f // Snap to nearest 90 degrees
            else -> pieceRotation
        }

        // Calculate grid cell size (matches grid boundaries: left=150f, right=width-150f, top=100f, bottom=height-180f)
        val cellWidth = (width - 300f) / gridColumns
        val cellHeight = (height - 280f) / gridRows

        // Get the current piece shape and apply rotation
        val currentPieceShape = pieces[currentPiece]
        val rotatedShape = rotatePiece(currentPieceShape, pieceRotation)
        val gridLeft = 150f
        val gridTop = 100f

        // Place the current piece in the grid based on its last position and shape
        for (row in rotatedShape.indices) {
            for (col in rotatedShape[row].indices) {
                if (rotatedShape[row][col] == 1) { // Only check filled blocks
                    val gridX = ((pieceX - gridLeft + col * 100f) / cellWidth).toInt()
                    val gridY = ((pieceY - gridTop + row * 100f) / cellHeight).toInt()

                    // Only place piece if it's within grid bounds
                    if (gridX in 0 until gridColumns && gridY in 0 until gridRows) {
                        grid[gridY][gridX] = currentPieceColor
                    }
                }
            }
        }

        checkLines()

        // Update current and next piece
        currentPiece = nextPiece
        currentPieceColor = nextPieceColor
        nextPiece = (nextPiece + 1) % pieces.size
        nextPieceColor = pieceColors[nextPiece]

        // Reset piece position for the new piece
        pieceX = (width / 2) - 50f
        pieceY = 100f  // Start at the top
        velocityY = 0f
        pieceRotation = 0f

        // Check if game is over
        if (grid[0].any { it != null }) {
            isGameOver = true
        }
    }

    private fun isPieceAtBottom(): Boolean {
        val currentPieceShape = pieces[currentPiece]
        val pieceSize = 100f // Size of each block
        val gridBottom = height - 180f // Grid bottom boundary matches onDraw()
        val cellHeight = (gridBottom - 100f) / gridRows // Calculate grid cell height (gridTop = 100f)

        for (row in currentPieceShape.indices) {
            for (col in currentPieceShape[row].indices) {
                if (currentPieceShape[row][col] == 1) { // Only check filled blocks
                    val blockY = pieceY + row * pieceSize
                    // Check if any part of the piece is at or below the bottom of the grid
                    if (blockY + pieceSize > gridBottom) {
                        return true // Piece is at the bottom
                    }
                }
            }
        }
        return false // Piece is not at the bottom
    }

    private fun collideAtBottom(cellWidth: Float, cellHeight: Float) {
        val currentPieceShape = pieces[currentPiece]
        val gridLeft = 150f
        val gridTop = 100f

        for (row in currentPieceShape.indices) {
            for (col in currentPieceShape[row].indices) {
                if (currentPieceShape[row][col] == 1) { // Only check filled blocks
                    val gridX = ((pieceX - gridLeft + col * 100f) / cellWidth).toInt()
                    val gridY = ((pieceY - gridTop + row * 100f) / cellHeight).toInt()

                    // Only place piece if it's within grid bounds
                    if (gridX in 0 until gridColumns && gridY in 0 until gridRows) {
                        grid[gridY][gridX] = currentPieceColor
                    }
                }
            }
        }
        turnPieceRigid() // Call to turn the piece rigid after placing it
    }

    private fun isPieceCollidingWithAnotherPiece(cellWidth: Float, cellHeight: Float): Boolean {
        // Get the rotated shape of the current piece
        val rotatedShape = rotatePiece(pieces[currentPiece], pieceRotation)
        val gridLeft = 150f
        val gridTop = 100f

        // Check for collision with other rigid pieces
        for (row in rotatedShape.indices) {
            for (col in rotatedShape[row].indices) {
                if (rotatedShape[row][col] == 1) { // Only check filled blocks
                    val cellX = ((pieceX - gridLeft + col * 100f) / cellWidth).toInt()
                    val cellY = ((pieceY - gridTop + row * 100f) / cellHeight).toInt()

                    // Check if the piece is colliding with the grid cell
                    if (cellX in 0 until gridColumns && cellY in 0 until gridRows) {
                        if (grid[cellY][cellX] != null) {
                            return true // Collision detected
                        }
                    }
                }
            }
        }
        return false // No collision
    }

    private fun collideWithAnotherPiece(cellWidth: Float, cellHeight: Float) {
        // Get the rotated shape of the current piece
        val rotatedShape = rotatePiece(pieces[currentPiece], pieceRotation)
        val gridLeft = 150f
        val gridTop = 100f

        // Check for collision with other rigid pieces
        for (row in rotatedShape.indices) {
            for (col in rotatedShape[row].indices) {
                if (rotatedShape[row][col] == 1) { // Only check filled blocks
                    val cellX = ((pieceX - gridLeft + col * 100f) / cellWidth).toInt()
                    val cellY = ((pieceY - gridTop + row * 100f) / cellHeight).toInt()

                    // Check if the piece is colliding with the grid cell
                    if (cellX in 0 until gridColumns && cellY in 0 until gridRows) {
                        // Collision with a rigid piece - place the shape in the grid
                        if (cellY - 1 >= 0) {
                            for (r in rotatedShape.indices) {
                                for (c in rotatedShape[r].indices) {
                                    if (rotatedShape[r][c] == 1) {
                                        val gridRow = cellY - 1 + r
                                        val gridCol = cellX + c
                                        if (gridRow in 0 until gridRows && gridCol in 0 until gridColumns) {
                                            grid[gridRow][gridCol] = currentPieceColor
                                        }
                                    }
                                }
                            }
                        }
                        turnPieceRigid() // Call to turn the piece rigid after placing it
                        return
                    }
                }
            }
        }
    }

    // Update method to handle gravity
    fun update() {
        if (!isPaused && !isGameOver) {
            // Any active drag (movement or rotation) pauses gravity; resumes on finger lift
            if (!isWaitingToTurnRigidAtBottom && !isWaitingToTurnRigidAtPiece
                && !isDragging) {
                pieceY += gravity
            }

            keepPiecesInsideWalls()
            checkCollisions()

            var didSolidify = false

            if (isWaitingToTurnRigidAtBottom) {
                if (System.currentTimeMillis() - bottomCollisionTime >= 3000) {
                    isDragging = false
                    turnPieceRigid()
                    isWaitingToTurnRigidAtBottom = false
                    isWaitingToTurnRigidAtPiece = false
                    didSolidify = true
                }
            }

            if (!didSolidify && isWaitingToTurnRigidAtPiece) {
                if (System.currentTimeMillis() - pieceCollisionTime >= 3000) {
                    isDragging = false
                    turnPieceRigid()
                    isWaitingToTurnRigidAtPiece = false
                }
            }

            invalidate()
        }
    }

    private fun keepPiecesInsideWalls() {
        val shape = rotatePiece(pieces[currentPiece], pieceRotation)
        pieceX = clampPieceX(shape, pieceX, 150f, width - 150f, 100f)
    }

    private fun resetGame() {
        // Stop the game loop
        handler.removeCallbacks(updateRunnable)
        
        // First, clear the grid completely
        for (i in 0 until gridRows) {
            for (j in 0 until gridColumns) {
                grid[i][j] = null
            }
        }

        // Reset all game state variables
        score = 0
        isGameOver = false
        isPaused = false
        isDragging = false
        isDraggingCenter = false
        touchedBlockRow = 0
        touchedBlockCol = 0
        springForceX = 0f
        springForceY = 0f
        velocityY = 0f
        pieceRotation = 0f
        lastTouchX = 0f
        lastTouchY = 0f
        
        // Reset piece types and colors
        currentPiece = 0
        currentPieceColor = pieceColors[0]
        nextPiece = 1
        nextPieceColor = pieceColors[1]
        
        // Reset piece position
        pieceX = (width / 2) - 50f  // Center horizontally
        pieceY = 100f  // Start at the top
        
        // Force a redraw
        invalidate()
        
        // Restart the game loop after a short delay to ensure everything is reset
        handler.postDelayed(updateRunnable, 100)
    }
}

internal fun clampPieceX(
    shape: List<List<Int>>,
    pieceX: Float,
    leftWall: Float,
    rightWall: Float,
    pieceSize: Float
): Float {
    var minCol = Int.MAX_VALUE
    var maxCol = Int.MIN_VALUE
    for (row in shape) {
        for ((col, cell) in row.withIndex()) {
            if (cell == 1) {
                if (col < minCol) minCol = col
                if (col > maxCol) maxCol = col
            }
        }
    }
    if (minCol == Int.MAX_VALUE) return pieceX
    var x = pieceX
    if (x + minCol * pieceSize < leftWall) x = leftWall - minCol * pieceSize
    if (x + maxCol * pieceSize + pieceSize > rightWall) x = rightWall - maxCol * pieceSize - pieceSize
    return x
}

// Maps a touch point to the canonical (row, col) of the hit block, accounting for pieceRotation.
// Returns null if the touch is outside all filled blocks.
internal fun hitCellFromTouch(
    touchX: Float,
    touchY: Float,
    pieceX: Float,
    pieceY: Float,
    pieceRotation: Float,
    shape: List<List<Int>>,
    pieceSize: Float = 100f
): Pair<Int, Int>? {
    val rcx = pieceX + shape[0].size * pieceSize / 2f
    val rcy = pieceY + shape.size * pieceSize / 2f
    val angle = -pieceRotation * Math.PI / 180.0
    val cosA = cos(angle)
    val sinA = sin(angle)
    val dx = (touchX - rcx).toDouble()
    val dy = (touchY - rcy).toDouble()
    val ux = (rcx + dx * cosA - dy * sinA).toFloat()
    val uy = (rcy + dx * sinA + dy * cosA).toFloat()
    val col = floor((ux - pieceX) / pieceSize).toInt()
    val row = floor((uy - pieceY) / pieceSize).toInt()
    if (row in shape.indices && col in shape[0].indices && shape[row][col] == 1) {
        return row to col
    }
    return null
}

// Upward drag is attenuated so the player can slow the fall but not reverse it.
internal fun effectiveVerticalDrag(dy: Float, upwardDragFactor: Float): Float =
    if (dy < 0f) dy * upwardDragFactor else dy

// Torque formula: cross product of (block→center) with drag vector, normalised by distance².
// The caller subtracts the result from pieceRotation.
internal fun rotationDeltaFromDrag(
    vx: Float, vy: Float, dx: Float, dy: Float, sensitivity: Float
): Float {
    val cross = vx * dy - vy * dx
    val dist2 = vx * vx + vy * vy + 1f
    return cross / dist2 * sensitivity
}

// Rotate a tetris piece shape around its center. Handles non-square shapes correctly.
// For 90° CW: (r,c) → (c, rows-1-r) producing a cols×rows output.
// For 270° CW: (r,c) → (cols-1-c, r) producing a cols×rows output.
internal fun rotatePiece(shape: List<List<Int>>, rotation: Float): List<List<Int>> {
    val rows = shape.size
    val cols = if (rows > 0) shape[0].size else 0
    return when (rotation.toInt() % 360) {
        90 -> List(cols) { newRow ->
            List(rows) { newCol -> shape[rows - 1 - newCol][newRow] }
        }
        180 -> shape.map { it.reversed() }.reversed()
        270 -> List(cols) { newRow ->
            List(rows) { newCol -> shape[newCol][cols - 1 - newRow] }
        }
        else -> shape
    }
}