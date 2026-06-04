package com.libuy.fluidtris

import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

internal class GameEngine(
    private val onPieceLocked: () -> Unit,
    private val onLineCleared: () -> Unit,
    private val onHighScoreBeat: (newScore: Int) -> Unit = {}
) {
    val grid = Array(GameConstants.GRID_ROWS) { Array<Int?>(GameConstants.GRID_COLUMNS) { null } }
    var score = 0
    var highScore = 0

    var pieceX = 0f
    var pieceY = 0f
    var pieceRotation = 0f
    private var velocityY = 0f

    var isDragging = false
    private var isDraggingCenter = false
    private var touchedBlockRow = 0
    private var touchedBlockCol = 0
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    var isPaused = false
    var isGameOver = true  // stays true until resetGame() is called with real dimensions
    var wasManuallyPausedBeforeSystemPause = false

    var currentPiece = 0
    var currentPieceColor = GameConstants.PIECE_COLORS[0]
    var nextPiece = 1
    var nextPieceColor = GameConstants.PIECE_COLORS[1]
    var nextPieceRotation = 0f

    private var bottomCollisionTime = 0L
    private var pieceCollisionTime = 0L
    private var isWaitingToTurnRigidAtBottom = false
    private var isWaitingToTurnRigidAtPiece = false

    fun update(viewWidth: Int, viewHeight: Int) {
        if (viewWidth == 0 || viewHeight == 0) return
        if (!isPaused && !isGameOver) {
            if (!isWaitingToTurnRigidAtBottom && !isWaitingToTurnRigidAtPiece && !isDragging) {
                pieceY += GameConstants.GRAVITY
            }

            keepPiecesInsideWalls(viewWidth)
            checkCollisions(viewWidth, viewHeight)

            var didSolidify = false

            if (isWaitingToTurnRigidAtBottom) {
                if (System.currentTimeMillis() - bottomCollisionTime >= GameConstants.LOCK_DELAY_MS) {
                    isDragging = false
                    turnPieceRigid(viewWidth, viewHeight)
                    isWaitingToTurnRigidAtBottom = false
                    isWaitingToTurnRigidAtPiece = false
                    didSolidify = true
                }
            }

            if (!didSolidify && isWaitingToTurnRigidAtPiece) {
                if (System.currentTimeMillis() - pieceCollisionTime >= GameConstants.LOCK_DELAY_MS) {
                    isDragging = false
                    moveUpUntilClear(viewWidth, viewHeight)
                    turnPieceRigid(viewWidth, viewHeight)
                    isWaitingToTurnRigidAtPiece = false
                }
            }
        }
    }

    fun resetGame(viewWidth: Int, viewHeight: Int) {
        for (i in 0 until GameConstants.GRID_ROWS) {
            for (j in 0 until GameConstants.GRID_COLUMNS) {
                grid[i][j] = null
            }
        }

        score = 0
        isGameOver = false
        isPaused = false
        isDragging = false
        isDraggingCenter = false
        touchedBlockRow = 0
        touchedBlockCol = 0
        velocityY = 0f
        pieceRotation = 0f
        lastTouchX = 0f
        lastTouchY = 0f
        isWaitingToTurnRigidAtBottom = false
        isWaitingToTurnRigidAtPiece = false

        currentPiece = Random.nextInt(GameConstants.PIECES.size)
        currentPieceColor = GameConstants.PIECE_COLORS[currentPiece]
        nextPiece = Random.nextInt(GameConstants.PIECES.size)
        nextPieceColor = GameConstants.PIECE_COLORS[nextPiece]
        nextPieceRotation = Random.nextInt(4) * 90f

        pieceX = (viewWidth / 2) - 50f
        pieceY = GameConstants.GRID_TOP
    }

    fun onTouchDown(x: Float, y: Float): Boolean {
        val hitCell = hitCellFromTouch(x, y, pieceX, pieceY, pieceRotation, GameConstants.PIECES[currentPiece])
        if (hitCell != null) {
            isDragging = true
            isDraggingCenter = hitCell in GameConstants.PIECE_CENTER_CELLS[currentPiece]
            touchedBlockRow = hitCell.first
            touchedBlockCol = hitCell.second
            lastTouchX = x
            lastTouchY = y
            return true
        }
        return false
    }

    fun onTouchMove(x: Float, y: Float, viewWidth: Int, viewHeight: Int) {
        if (!isDragging) return
        val dx = x - lastTouchX
        val dy = y - lastTouchY
        var appliedDx = 0f
        var appliedDy = 0f
        val prevRotation = pieceRotation

        if (isDraggingCenter) {
            appliedDx = dx
            appliedDy = effectiveVerticalDrag(dy, GameConstants.UPWARD_DRAG_FACTOR)
            pieceX += appliedDx
            pieceY += appliedDy
            keepPiecesInsideWalls(viewWidth)
        } else {
            val shape = GameConstants.PIECES[currentPiece]
            val rcx = pieceX + shape[0].size * GameConstants.PIECE_SIZE / 2f
            val rcy = pieceY + shape.size * GameConstants.PIECE_SIZE / 2f
            val angle = pieceRotation * Math.PI / 180.0
            val cosA = cos(angle)
            val sinA = sin(angle)
            val bux = pieceX + (touchedBlockCol + 0.5f) * GameConstants.PIECE_SIZE
            val buy = pieceY + (touchedBlockRow + 0.5f) * GameConstants.PIECE_SIZE
            val bdx = (bux - rcx).toDouble()
            val bdy = (buy - rcy).toDouble()
            val bx = (rcx + bdx * cosA - bdy * sinA).toFloat()
            val by = (rcy + bdx * sinA + bdy * cosA).toFloat()
            val vx = rcx - bx
            val vy = rcy - by
            pieceRotation -= rotationDeltaFromDrag(vx, vy, dx, dy, GameConstants.ROTATION_SENSITIVITY)
            appliedDx = dx
            appliedDy = effectiveVerticalDrag(dy, GameConstants.UPWARD_DRAG_FACTOR)
            pieceX += appliedDx
            pieceY += appliedDy
            keepPiecesInsideWalls(viewWidth)
        }

        lastTouchX = x
        lastTouchY = y

        val cellWidth = (viewWidth - GameConstants.GRID_LEFT - GameConstants.GRID_RIGHT_MARGIN) / GameConstants.GRID_COLUMNS
        val cellHeight = (viewHeight - GameConstants.GRID_TOP - GameConstants.GRID_BOTTOM_MARGIN) / GameConstants.GRID_ROWS
        val cellX = ((pieceX - GameConstants.GRID_LEFT) / cellWidth).toInt()
        val cellY = ((pieceY - GameConstants.GRID_TOP) / cellHeight).toInt()
        if (cellX in 0 until GameConstants.GRID_COLUMNS && cellY in 0 until GameConstants.GRID_ROWS) {
            if (isPieceAtBottom(viewHeight)) {
                isDragging = false
                lockPieceAtBottom(viewWidth, viewHeight)
            }
            if (isPieceCollidingWithAnotherPiece(cellWidth, cellHeight)) {
                pieceX -= appliedDx
                pieceY -= appliedDy
                pieceRotation = prevRotation
                isDragging = false
            }
        }
    }

    fun onTouchUp() {
        isDragging = false
    }

    fun getCollisionSolidity(): Float {
        val elapsed = when {
            isWaitingToTurnRigidAtBottom -> System.currentTimeMillis() - bottomCollisionTime
            isWaitingToTurnRigidAtPiece -> System.currentTimeMillis() - pieceCollisionTime
            else -> return 0f
        }
        val t = (elapsed / GameConstants.LOCK_DELAY_MS.toFloat()).coerceIn(0f, 1f)
        val oscillation = sin(t * Math.PI * 6.0).toFloat()
        return (t + oscillation * (1f - t) * 0.6f).coerceIn(0f, 1f)
    }

    private fun keepPiecesInsideWalls(viewWidth: Int) {
        val centers = rotatedBlockCenters(GameConstants.PIECES[currentPiece], pieceX, pieceY, pieceRotation)
        pieceX = clampPieceXByCenters(
            centers, pieceX,
            GameConstants.GRID_LEFT,
            viewWidth - GameConstants.GRID_RIGHT_MARGIN,
            GameConstants.PIECE_SIZE / 2
        )
    }

    private fun checkCollisions(viewWidth: Int, viewHeight: Int) {
        val cellWidth = (viewWidth - GameConstants.GRID_LEFT - GameConstants.GRID_RIGHT_MARGIN) / GameConstants.GRID_COLUMNS
        val cellHeight = (viewHeight - GameConstants.GRID_TOP - GameConstants.GRID_BOTTOM_MARGIN) / GameConstants.GRID_ROWS

        if (isPieceAtBottom(viewHeight)) {
            if (!isWaitingToTurnRigidAtBottom) {
                isWaitingToTurnRigidAtBottom = true
                bottomCollisionTime = System.currentTimeMillis()
            }
        } else {
            isWaitingToTurnRigidAtBottom = false
        }

        if (isPieceCollidingWithAnotherPiece(cellWidth, cellHeight)) {
            if (!isWaitingToTurnRigidAtPiece) {
                isWaitingToTurnRigidAtPiece = true
                pieceCollisionTime = System.currentTimeMillis()
            }
        } else {
            isWaitingToTurnRigidAtPiece = false
        }
    }

    internal fun turnPieceRigid(viewWidth: Int, viewHeight: Int) {
        onPieceLocked()

        var normalized = pieceRotation % 360f
        if (normalized < 0f) normalized += 360f
        pieceRotation = (Math.round(normalized / 90f).toInt() % 4) * 90f

        val cellWidth = (viewWidth - GameConstants.GRID_LEFT - GameConstants.GRID_RIGHT_MARGIN) / GameConstants.GRID_COLUMNS
        val cellHeight = (viewHeight - GameConstants.GRID_TOP - GameConstants.GRID_BOTTOM_MARGIN) / GameConstants.GRID_ROWS

        val currentPieceShape = GameConstants.PIECES[currentPiece]
        val rotatedShape = rotatePiece(currentPieceShape, pieceRotation)

        for (row in rotatedShape.indices) {
            for (col in rotatedShape[row].indices) {
                if (rotatedShape[row][col] == 1) {
                    val gridX = ((pieceX + col * GameConstants.PIECE_SIZE + GameConstants.PIECE_SIZE / 2 - GameConstants.GRID_LEFT) / cellWidth).toInt()
                    val gridY = ((pieceY + row * GameConstants.PIECE_SIZE + GameConstants.PIECE_SIZE / 2 - GameConstants.GRID_TOP) / cellHeight).toInt()
                    if (gridX in 0 until GameConstants.GRID_COLUMNS && gridY in 0 until GameConstants.GRID_ROWS) {
                        grid[gridY][gridX] = currentPieceColor
                    }
                }
            }
        }

        checkLines()

        currentPiece = nextPiece
        currentPieceColor = nextPieceColor
        val spawnRotation = nextPieceRotation
        nextPiece = Random.nextInt(GameConstants.PIECES.size)
        nextPieceColor = GameConstants.PIECE_COLORS[nextPiece]
        nextPieceRotation = Random.nextInt(4) * 90f

        pieceX = (viewWidth / 2) - 50f
        pieceY = GameConstants.GRID_TOP
        velocityY = 0f
        pieceRotation = spawnRotation

        if (grid[0].any { it != null }) {
            isGameOver = true
        }
    }

    internal fun checkLines() {
        var linesCleared = 0
        var i = GameConstants.GRID_ROWS - 1
        while (i >= 0) {
            if (grid[i].all { it != null }) {
                for (j in i downTo 1) {
                    grid[j] = grid[j - 1].clone()
                }
                grid[0] = Array(GameConstants.GRID_COLUMNS) { null }
                linesCleared++
                // stay at i — the shifted row needs re-checking
            } else {
                i--
            }
        }
        if (linesCleared > 0) {
            score += linesCleared * 100
            if (score > highScore) {
                highScore = score
                onHighScoreBeat(score)
            }
            onLineCleared()
        }
    }

    internal fun isPieceAtBottom(viewHeight: Int): Boolean {
        val gridBottom = viewHeight - GameConstants.GRID_BOTTOM_MARGIN
        return rotatedBlockCenters(GameConstants.PIECES[currentPiece], pieceX, pieceY, pieceRotation)
            .any { (_, by) -> by + GameConstants.PIECE_SIZE / 2 > gridBottom }
    }

    internal fun lockPieceAtBottom(viewWidth: Int, viewHeight: Int) {
        turnPieceRigid(viewWidth, viewHeight)
    }

    internal fun isPieceCollidingWithAnotherPiece(cellWidth: Float, cellHeight: Float): Boolean {
        val blockSize = GameConstants.PIECE_SIZE
        for ((bx, by) in rotatedBlockCenters(GameConstants.PIECES[currentPiece], pieceX, pieceY, pieceRotation)) {
            val corners = listOf(
                bx - blockSize / 2f to by - blockSize / 2f,
                bx + blockSize / 2f to by - blockSize / 2f,
                bx - blockSize / 2f to by + blockSize / 2f,
                bx + blockSize / 2f to by + blockSize / 2f
            )
            for ((cx, cy) in corners) {
                val cellX = ((cx - GameConstants.GRID_LEFT) / cellWidth).toInt()
                val cellY = ((cy - GameConstants.GRID_TOP) / cellHeight).toInt()
                if (cellX in 0 until GameConstants.GRID_COLUMNS && cellY in 0 until GameConstants.GRID_ROWS) {
                    if (grid[cellY][cellX] != null) return true
                }
            }
        }
        return false
    }

    private fun moveUpUntilClear(viewWidth: Int, viewHeight: Int) {
        val cellWidth = (viewWidth - GameConstants.GRID_LEFT - GameConstants.GRID_RIGHT_MARGIN) / GameConstants.GRID_COLUMNS
        val cellHeight = (viewHeight - GameConstants.GRID_TOP - GameConstants.GRID_BOTTOM_MARGIN) / GameConstants.GRID_ROWS
        val step = 5f
        var moved = false

        while (doesPieceCollideWithGridAtY(pieceY, cellWidth, cellHeight)) {
            pieceY -= step
            moved = true
            if (pieceY < GameConstants.GRID_TOP) break
        }

        if (moved) {
            keepPiecesInsideWalls(viewWidth)
        }
    }

    private fun doesPieceCollideWithGridAtY(testY: Float, cellWidth: Float, cellHeight: Float): Boolean {
        val blockSize = GameConstants.PIECE_SIZE

        for ((bx, _) in rotatedBlockCenters(GameConstants.PIECES[currentPiece], pieceX, testY, pieceRotation)) {
            val by = testY + blockSize / 2f

            val corners = listOf(
                bx - blockSize / 2f to by - blockSize / 2f,
                bx + blockSize / 2f to by - blockSize / 2f,
                bx - blockSize / 2f to by + blockSize / 2f,
                bx + blockSize / 2f to by + blockSize / 2f
            )
            for ((cx, cy) in corners) {
                val cellX = ((cx - GameConstants.GRID_LEFT) / cellWidth).toInt()
                val cellY = ((cy - GameConstants.GRID_TOP) / cellHeight).toInt()
                if (cellX in 0 until GameConstants.GRID_COLUMNS && cellY in 0 until GameConstants.GRID_ROWS) {
                    if (grid[cellY][cellX] != null) return true
                }
            }
        }
        return false
    }
}
