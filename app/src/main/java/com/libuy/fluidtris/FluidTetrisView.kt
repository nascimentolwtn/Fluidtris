package com.libuy.fluidtris

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class FluidTetrisView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val GAME_LOOP_INTERVAL_MS = 16L
    }

    interface GameListener {
        fun onExitPressed()
    }

    var gameListener: GameListener? = null

    private val paint = Paint().apply { style = Paint.Style.FILL }
    private val jellyRect = RectF()
    private val backgroundBitmap: Bitmap =
        BitmapFactory.decodeResource(resources, R.drawable.game_background)

    private val soundManager = SoundManager(context)
    private val highScoreManager = HighScoreManager(context)
    private val engine = GameEngine(
        onPieceLocked = { soundManager.playRigid() },
        onLineCleared = { soundManager.playMove() },
        onHighScoreBeat = { newScore -> highScoreManager.saveHighScore(newScore) },
        onLevelUp = { soundManager.playLevelUpSound() },
        onGameOver = { beatHighScore ->
            soundManager.pauseBgMusic()
            if (beatHighScore) {
                soundManager.playHighScoreCheer()
            } else {
                soundManager.playGameOverSound()
            }
        }
    )
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            engine.update(width, height)
            invalidate()
            handler.postDelayed(this, GAME_LOOP_INTERVAL_MS)
        }
    }

    init {
        engine.highScore = highScoreManager.loadHighScore()
        handler.post(updateRunnable)
        soundManager.startBgMusic(context)
    }

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(w, h, oldW, oldH)
        engine.resetGame(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawBitmap(backgroundBitmap, null,
            RectF(0f, 0f, width.toFloat(), height.toFloat()), null)

        val gridLeft = GameConstants.GRID_LEFT
        val gridTop = GameConstants.GRID_TOP
        val gridRight = width - GameConstants.GRID_RIGHT_MARGIN
        val gridBottom = height - GameConstants.GRID_BOTTOM_MARGIN
        val cellWidth = (gridRight - gridLeft) / GameConstants.GRID_COLUMNS
        val cellHeight = (gridBottom - gridTop) / GameConstants.GRID_ROWS

        paint.color = Color.BLACK
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRect(gridLeft, gridTop, gridRight, gridBottom, paint)
        paint.style = Paint.Style.FILL

        for (i in 0 until GameConstants.GRID_ROWS) {
            for (j in 0 until GameConstants.GRID_COLUMNS) {
                engine.grid[i][j]?.let { color ->
                    paint.color = color
                    canvas.drawRect(gridLeft + j * cellWidth, gridTop + i * cellHeight,
                        gridLeft + (j + 1) * cellWidth, gridTop + (i + 1) * cellHeight, paint)
                    paint.color = Color.BLACK
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 2f
                    canvas.drawRect(gridLeft + j * cellWidth, gridTop + i * cellHeight,
                        gridLeft + (j + 1) * cellWidth, gridTop + (i + 1) * cellHeight, paint)
                    paint.style = Paint.Style.FILL
                }
            }
        }

        val pieceSize = GameConstants.PIECE_SIZE

        for (piece in engine.fallingPieces) {
            val shape = GameConstants.PIECES[piece.type]
            val solidity = engine.getCollisionSolidity(piece)
            val fluidBlockSize = pieceSize - (1f - solidity) * 4f
            val drawOffsetX = shape[0].size * (pieceSize - fluidBlockSize) / 2f
            val drawOffsetY = shape.size * (pieceSize - fluidBlockSize) / 2f
            canvas.save()
            canvas.rotate(piece.rotation,
                piece.x + (shape[0].size * pieceSize) / 2,
                piece.y + (shape.size * pieceSize) / 2)
            drawJellyPiece(canvas, shape,
                piece.x + drawOffsetX, piece.y + drawOffsetY,
                piece.color, solidity, fluidBlockSize)
            canvas.restore()
        }

        val nextShape = GameConstants.PIECES[engine.nextPiece]
        val previewBlockSize = 50f
        val previewCols = nextShape[0].size
        val previewRows = nextShape.size
        val previewBoxSize = 160f
        val previewX = width - previewBoxSize - 20f
        val previewY = 20f
        paint.color = Color.argb(180, 20, 60, 100)
        canvas.drawRect(previewX - 8f, previewY - 8f,
            previewX + previewBoxSize + 8f, previewY + previewBoxSize + 8f, paint)
        val pieceDrawX = previewX + (previewBoxSize - previewCols * previewBlockSize) / 2f
        val pieceDrawY = previewY + (previewBoxSize - previewRows * previewBlockSize) / 2f
        val previewCenterX = previewX + previewBoxSize / 2f
        val previewCenterY = previewY + previewBoxSize / 2f
        canvas.save()
        canvas.rotate(engine.nextPieceRotation, previewCenterX, previewCenterY)
        drawJellyPiece(canvas, nextShape, pieceDrawX, pieceDrawY,
            engine.nextPieceColor, 1f, previewBlockSize)
        canvas.restore()

        // Next buttons (left and right, spanning grid height)
        if (!engine.isGameOver && !engine.isPaused) {
            val gridTop = GameConstants.GRID_TOP
            val gridBottom = height - GameConstants.GRID_BOTTOM_MARGIN

            // Left button
            val leftButtonX = 0f
            paint.color = Color.argb(200, 80, 150, 100)
            canvas.drawRect(leftButtonX, gridTop, GameConstants.GRID_LEFT, gridBottom, paint)
            paint.color = Color.argb(255, 255, 255, 255)
            paint.textSize = 32f
            canvas.drawText("N", GameConstants.GRID_LEFT / 2 - 12f, gridTop + (gridBottom - gridTop) / 2 + 12f, paint)

            // Right button
            val rightButtonX = width - GameConstants.GRID_RIGHT_MARGIN
            paint.color = Color.argb(200, 80, 150, 100)
            canvas.drawRect(rightButtonX, gridTop, width.toFloat(), gridBottom, paint)
            paint.color = Color.argb(255, 255, 255, 255)
            canvas.drawText("N", rightButtonX + GameConstants.GRID_RIGHT_MARGIN / 2 - 12f, gridTop + (gridBottom - gridTop) / 2 + 12f, paint)
        }

        paint.color = Color.argb(180, 20, 60, 100)
        canvas.drawRect(10f, 5f, 400f, 155f, paint)
        paint.textSize = 40f
        paint.color = Color.argb(255, 100, 220, 200)
        canvas.drawText("Score: ${engine.score}", 20f, 40f, paint)
        paint.color = if (engine.isBeatingHighScore) Color.YELLOW else Color.argb(255, 100, 220, 200)
        canvas.drawText("High Score: ${engine.highScore}", 20f, 80f, paint)
        paint.color = Color.argb(255, 100, 220, 200)
        canvas.drawText("Level: ${engine.getLevel()}", 20f, 140f, paint)

        paint.color = Color.argb(200, 80, 120, 150)
        canvas.drawRect(10f, 170f, 280f, 270f, paint)
        paint.color = Color.argb(255, 200, 240, 230)
        paint.textSize = 48f
        val soundText = if (soundManager.enabled) "🔊" else "🔇"
        canvas.drawText(soundText, 110f, 235f, paint)

        // BG music toggle button
        paint.color = Color.argb(200, 80, 120, 150)
        canvas.drawRect(10f, 280f, 280f, 380f, paint)
        paint.color = Color.argb(255, 200, 240, 230)
        paint.textSize = 48f
        val bgText = if (soundManager.bgMusicEnabled) "🎵" else "🎵🔇"
        canvas.drawText(bgText, 100f, 345f, paint)

        paint.color = Color.argb(200, 50, 150, 130)
        canvas.drawRect(20f, height - 150f, 200f, height - 50f, paint)
        paint.color = Color.argb(255, 200, 240, 230)
        paint.textSize = 30f
        canvas.drawText("New Game", 35f, height - 100f, paint)

        // Pause button (center)
        paint.color = Color.argb(200, 100, 150, 180)
        canvas.drawRect(width / 2 - 100f, height - 150f, width / 2 + 100f, height - 50f, paint)
        paint.color = Color.argb(255, 200, 240, 230)
        val pauseButtonText = if (engine.isPaused) "Resume" else "Pause"
        canvas.drawText(pauseButtonText, width / 2 - 70f, height - 100f, paint)

        // Exit button (right)
        paint.color = Color.argb(200, 150, 80, 80)
        canvas.drawRect(width - 200f, height - 150f, width - 20f, height - 50f, paint)
        paint.color = Color.argb(255, 200, 240, 230)
        canvas.drawText("Exit", width - 160f, height - 100f, paint)

        if (engine.isGameOver) {
            paint.color = Color.argb(150, 20, 40, 80)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        }

        if (engine.isGameOver) {
            paint.color = Color.argb(255, 150, 200, 220)
            paint.textSize = 80f
            canvas.drawText("Game Over", width / 2 - 250f, height / 2 - 50f, paint)
            paint.textSize = 60f
            paint.color = if (engine.justBeatHighScore) Color.YELLOW else Color.argb(255, 120, 200, 220)
            canvas.drawText("Score: ${engine.score}", width / 2 - 250f, height / 2 + 80f, paint)
            canvas.drawText("High Score: ${engine.highScore}", width / 2 - 250f, height / 2 + 150f, paint)

            val buttonWidth = 320f
            val buttonHeight = 120f
            val buttonGap = 30f
            val newGameX = (width - 2 * buttonWidth - buttonGap) / 2
            val exitX = newGameX + buttonWidth + buttonGap
            val buttonY = height / 2 + 260f

            paint.color = Color.argb(220, 80, 180, 150)
            canvas.drawRect(newGameX, buttonY, newGameX + buttonWidth, buttonY + buttonHeight, paint)
            paint.color = Color.argb(255, 255, 255, 255)
            paint.textSize = 50f
            canvas.drawText("New Game", newGameX + 20f, buttonY + 80f, paint)

            paint.color = Color.argb(220, 180, 80, 80)
            canvas.drawRect(exitX, buttonY, exitX + buttonWidth, buttonY + buttonHeight, paint)
            paint.color = Color.argb(255, 255, 255, 255)
            canvas.drawText("Exit", exitX + 100f, buttonY + 80f, paint)

        }

        if (engine.isPaused && !engine.isGameOver) {
            paint.color = Color.argb(150, 20, 40, 80)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            paint.color = Color.argb(255, 150, 200, 220)
            paint.textSize = 80f
            canvas.drawText("PAUSED", width / 2 - 200f, height / 2 - 50f, paint)

            val buttonWidth = 320f
            val buttonHeight = 120f
            val buttonGap = 30f
            val totalWidth = 3 * buttonWidth + 2 * buttonGap
            val startX = (width - totalWidth) / 2
            val newGameX = startX
            val resumeX = startX + buttonWidth + buttonGap
            val exitX = startX + 2 * (buttonWidth + buttonGap)
            val buttonY = height / 2 + 200f

            paint.color = Color.argb(220, 80, 180, 150)
            canvas.drawRect(newGameX, buttonY, newGameX + buttonWidth, buttonY + buttonHeight, paint)
            paint.color = Color.argb(255, 255, 255, 255)
            paint.textSize = 50f
            canvas.drawText("New Game", newGameX + 20f, buttonY + 80f, paint)

            paint.color = Color.argb(220, 100, 180, 150)
            canvas.drawRect(resumeX, buttonY, resumeX + buttonWidth, buttonY + buttonHeight, paint)
            paint.color = Color.argb(255, 255, 255, 255)
            canvas.drawText("Resume", resumeX + 60f, buttonY + 80f, paint)

            paint.color = Color.argb(220, 180, 80, 80)
            canvas.drawRect(exitX, buttonY, exitX + buttonWidth, buttonY + buttonHeight, paint)
            paint.color = Color.argb(255, 255, 255, 255)
            canvas.drawText("Exit", exitX + 100f, buttonY + 80f, paint)
        }

        if (engine.isPaused) {
            paint.color = Color.argb(200, 80, 120, 150)
            canvas.drawRect(10f, 170f, 280f, 270f, paint)
            paint.color = Color.argb(255, 200, 240, 230)
            paint.textSize = 48f
            val soundText = if (soundManager.enabled) "🔊" else "🔇"
            canvas.drawText(soundText, 110f, 235f, paint)

            // BG music toggle button (pause overlay)
            paint.color = Color.argb(200, 80, 120, 150)
            canvas.drawRect(10f, 280f, 280f, 380f, paint)
            paint.color = Color.argb(255, 200, 240, 230)
            paint.textSize = 48f
            val bgText = if (soundManager.bgMusicEnabled) "🎵" else "🎵🔇"
            canvas.drawText(bgText, 100f, 345f, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (event.x in 10f..280f && event.y in 170f..270f) {
                    soundManager.enabled = !soundManager.enabled
                    invalidate()
                    return true
                }
                if (event.x in 10f..280f && event.y in 280f..380f) {
                    soundManager.toggleBgMusic(context)
                    invalidate()
                    return true
                }
                if (engine.isGameOver) {
                    val buttonWidth = 320f
                    val buttonHeight = 120f
                    val buttonGap = 30f
                    val newGameX = (width - 2 * buttonWidth - buttonGap) / 2
                    val exitX = newGameX + buttonWidth + buttonGap
                    val buttonY = height / 2 + 260f

                    if (event.x in newGameX..(newGameX + buttonWidth) && event.y in buttonY..(buttonY + buttonHeight)) {
                        engine.resetGame(width, height)
                        soundManager.startBgMusic(context)
                        invalidate()
                        return true
                    }
                    if (event.x in exitX..(exitX + buttonWidth) && event.y in buttonY..(buttonY + buttonHeight)) {
                        soundManager.stopBgMusic()
                        gameListener?.onExitPressed()
                        return true
                    }
                    return true
                }
                if (engine.isPaused && !engine.isGameOver) {
                    val buttonWidth = 320f
                    val buttonHeight = 120f
                    val buttonGap = 30f
                    val totalWidth = 3 * buttonWidth + 2 * buttonGap
                    val startX = (width - totalWidth) / 2
                    val newGameX = startX
                    val resumeX = startX + buttonWidth + buttonGap
                    val exitX = startX + 2 * (buttonWidth + buttonGap)
                    val buttonY = height / 2 + 200f

                    if (event.x in newGameX..(newGameX + buttonWidth) && event.y in buttonY..(buttonY + buttonHeight)) {
                        engine.resetGame(width, height)
                        soundManager.startBgMusic(context)
                        invalidate()
                        return true
                    }
                    if (event.x in resumeX..(resumeX + buttonWidth) && event.y in buttonY..(buttonY + buttonHeight)) {
                        engine.resume()
                        soundManager.resumeBgMusic()
                        invalidate()
                        return true
                    }
                    if (event.x in exitX..(exitX + buttonWidth) && event.y in buttonY..(buttonY + buttonHeight)) {
                        soundManager.stopBgMusic()
                        gameListener?.onExitPressed()
                        return true
                    }
                    return true
                }
                if (!engine.isPaused && engine.onTouchDown(event.x, event.y)) {
                    return true
                }
                if (event.x in 20f..200f && event.y in (height - 150f)..(height - 50f)) {
                    engine.resetGame(width, height)
                    soundManager.startBgMusic(context)
                    invalidate()
                    return true
                }
                if (event.x in (width / 2 - 100f)..(width / 2 + 100f) && event.y in (height - 150f)..(height - 50f)) {
                    engine.togglePause()
                    if (engine.isPaused) soundManager.pauseBgMusic() else soundManager.resumeBgMusic()
                    return true
                }
                if (event.x in (width - 200f)..(width - 20f) && event.y in (height - 150f)..(height - 50f)) {
                    soundManager.stopBgMusic()
                    gameListener?.onExitPressed()
                    return true
                }
                // Next button touch detection (left and right side buttons)
                if (!engine.isPaused && !engine.isGameOver) {
                    val gridTop = GameConstants.GRID_TOP
                    val gridBottom = height - GameConstants.GRID_BOTTOM_MARGIN
                    val rightButtonX = width - GameConstants.GRID_RIGHT_MARGIN

                    if ((event.x in 0f..GameConstants.GRID_LEFT && event.y in gridTop..gridBottom) ||
                        (event.x in rightButtonX..width.toFloat() && event.y in gridTop..gridBottom)) {
                        engine.onNextPieceButton(width, height)
                        invalidate()
                        return true
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (engine.isDragging) {
                    engine.onTouchMove(event.x, event.y, width, height)
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP -> {
                if (!engine.isPaused) {
                    engine.onTouchUp()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (!hasWindowFocus) engine.onFocusLost() else engine.onFocusGained()
    }

    fun onAppPause() {
        engine.onFocusLost()
        soundManager.pauseBgMusic()
    }

    fun onAppResume() {
        engine.onFocusGained()
        if (!engine.isPaused) soundManager.resumeBgMusic()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        soundManager.stopBgMusic()
    }

    private fun drawJellyPiece(
        canvas: Canvas, shape: List<List<Int>>,
        startX: Float, startY: Float, color: Int,
        solidity: Float, blockSize: Float = 100f
    ) {
        paint.isAntiAlias = true
        val jelly = 1f - solidity
        val inset = GameConstants.BLOCK_INSET
        val cornerRadius = jelly * blockSize * 0.38f + 8f
        val expand = jelly * blockSize * 0.06f
        val bridgeInset = blockSize * (0.12f + solidity * 0.14f)

        paint.color = color
        paint.style = Paint.Style.FILL

        for (row in shape.indices) {
            for (col in shape[row].indices) {
                if (shape[row][col] != 1) continue
                val x = startX + col * blockSize
                val y = startY + row * blockSize
                if (col + 1 < shape[row].size && shape[row][col + 1] == 1) {
                    jellyRect.set(x + blockSize * 0.4f, y + bridgeInset,
                        x + blockSize * 1.6f, y + blockSize - bridgeInset)
                    canvas.drawRoundRect(jellyRect, cornerRadius * 0.4f, cornerRadius * 0.4f, paint)
                }
                if (row + 1 < shape.size && shape[row + 1][col] == 1) {
                    jellyRect.set(x + bridgeInset, y + blockSize * 0.4f,
                        x + blockSize - bridgeInset, y + blockSize * 1.6f)
                    canvas.drawRoundRect(jellyRect, cornerRadius * 0.4f, cornerRadius * 0.4f, paint)
                }
            }
        }

        for (row in shape.indices) {
            for (col in shape[row].indices) {
                if (shape[row][col] != 1) continue
                val x = startX + col * blockSize
                val y = startY + row * blockSize

                paint.color = color
                jellyRect.set(x + inset - expand, y + inset - expand, x + blockSize - inset + expand, y + blockSize - inset + expand)
                canvas.drawRoundRect(jellyRect, cornerRadius, cornerRadius, paint)

                if (jelly > 0.05f) {
                    paint.color = Color.argb((jelly * 90).toInt(), 255, 255, 255)
                    val hlR = blockSize * 0.18f * jelly
                    jellyRect.set(x + blockSize * 0.18f, y + blockSize * 0.18f,
                        x + blockSize * 0.18f + hlR * 2, y + blockSize * 0.18f + hlR * 2)
                    canvas.drawOval(jellyRect, paint)
                }

                if (solidity > 0.15f) {
                    paint.color = Color.argb((solidity * 210).toInt(), 0, 0, 0)
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 2f
                    jellyRect.set(x + inset, y + inset, x + blockSize - inset, y + blockSize - inset)
                    canvas.drawRoundRect(jellyRect, 8f, 8f, paint)
                    paint.style = Paint.Style.FILL
                }
            }
        }

        paint.isAntiAlias = false
    }
}
