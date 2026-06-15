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

    private var springForceX = 0f

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

    internal var currentTimeMs: () -> Long = System::currentTimeMillis

    private var bottomCollisionTime = 0L
    private var pieceCollisionTime = 0L
    private var isWaitingToTurnRigidAtBottom = false
    private var isWaitingToTurnRigidAtPiece = false
    private var isSlidingOnContact = false
    private var slideDirection = 0f
    private var bounceCount = 0

    private var isSnapAnimating = false
    private var snapTargetX = 0f
    private var snapTargetY = 0f
    private var snapTargetRotation = 0f

    fun update(viewWidth: Int, viewHeight: Int) {
        if (viewWidth == 0 || viewHeight == 0) return
        if (!isPaused && !isGameOver) {
            if (!isWaitingToTurnRigidAtBottom && !isWaitingToTurnRigidAtPiece && !isDragging) {
                pieceY += GameConstants.GRAVITY
            }

            if (!isDragging && springForceX != 0f) {
                pieceX += springForceX
                springForceX *= GameConstants.SPRING_DAMPING
                if (kotlin.math.abs(springForceX) < 0.1f) springForceX = 0f
            }

            keepPiecesInsideWalls(viewWidth)
            checkCollisions(viewWidth, viewHeight)

            if (!isDragging && isSnapAnimating && (isWaitingToTurnRigidAtBottom || isWaitingToTurnRigidAtPiece)) {
                val elapsed = when {
                    isWaitingToTurnRigidAtBottom -> currentTimeMs() - bottomCollisionTime
                    else                         -> currentTimeMs() - pieceCollisionTime
                }
                val t = (elapsed / GameConstants.LOCK_DELAY_MS.toFloat()).coerceIn(0f, 1f)
                applySnapPull(t)
            }

            var didSolidify = false

            if (isWaitingToTurnRigidAtBottom) {
                if (currentTimeMs() - bottomCollisionTime >= GameConstants.LOCK_DELAY_MS) {
                    isDragging = false
                    turnPieceRigid(viewWidth, viewHeight)
                    isWaitingToTurnRigidAtBottom = false
                    isWaitingToTurnRigidAtPiece = false
                    didSolidify = true
                }
            }

            if (!didSolidify && isWaitingToTurnRigidAtPiece) {
                if (currentTimeMs() - pieceCollisionTime >= GameConstants.LOCK_DELAY_MS) {
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
        springForceX = 0f
        pieceRotation = 0f
        lastTouchX = 0f
        lastTouchY = 0f
        isWaitingToTurnRigidAtBottom = false
        isWaitingToTurnRigidAtPiece = false
        isSlidingOnContact = false
        slideDirection = 0f
        bounceCount = 0
        isSnapAnimating = false

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
            springForceX = 0f
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
        springForceX = dx * GameConstants.SPRING_CARRY
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
            isWaitingToTurnRigidAtBottom -> currentTimeMs() - bottomCollisionTime
            isWaitingToTurnRigidAtPiece -> currentTimeMs() - pieceCollisionTime
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

        val bottomContacts = countContactBlocksAtBottom(viewHeight)
        val pieceContacts  = countContactBlocksWithPiece(cellWidth, cellHeight)
        val totalContacts  = bottomContacts + pieceContacts

        when {
            bounceCount >= 4 && totalContacts >= 1 -> {
                // Exhausted bounces — force-lock as safety net; cancel any snap animation.
                isSnapAnimating = false
                isSlidingOnContact = false
                slideDirection = 0f
                if (bottomContacts > 0) {
                    if (!isWaitingToTurnRigidAtBottom) {
                        isWaitingToTurnRigidAtBottom = true
                        bottomCollisionTime = currentTimeMs()
                        springForceX = 0f
                    }
                } else { isWaitingToTurnRigidAtBottom = false }
                if (pieceContacts > 0) {
                    if (!isWaitingToTurnRigidAtPiece) {
                        isWaitingToTurnRigidAtPiece = true
                        pieceCollisionTime = currentTimeMs()
                        springForceX = 0f
                    }
                } else { isWaitingToTurnRigidAtPiece = false }
            }
            totalContacts >= 1 && canLockCleanly(viewWidth, viewHeight) -> {
                // Piece is resting cleanly on valid empty cells — start lock timer.
                isSlidingOnContact = false
                slideDirection = 0f
                if (bottomContacts > 0) {
                    if (!isWaitingToTurnRigidAtBottom) {
                        isWaitingToTurnRigidAtBottom = true
                        bottomCollisionTime = currentTimeMs()
                        springForceX = 0f
                        if (!isSnapAnimating) beginSnapAnimation(viewWidth, viewHeight)
                    }
                } else { isWaitingToTurnRigidAtBottom = false }
                if (pieceContacts > 0) {
                    if (!isWaitingToTurnRigidAtPiece) {
                        isWaitingToTurnRigidAtPiece = true
                        pieceCollisionTime = currentTimeMs()
                        springForceX = 0f
                        if (!isSnapAnimating) beginSnapAnimation(viewWidth, viewHeight)
                    }
                } else { isWaitingToTurnRigidAtPiece = false }
            }
            totalContacts >= 1 -> {
                // Contact but position not yet clean.
                // During snap animation the piece is in transit — let it continue, no bounce.
                if (!isSnapAnimating) {
                    isWaitingToTurnRigidAtBottom = false
                    isWaitingToTurnRigidAtPiece = false
                    if (!isSlidingOnContact) {
                        bounceCount++
                        val force = computeSlideForceX(viewHeight, cellWidth, cellHeight)
                        slideDirection = if (force >= 0f) 1f else -1f
                        springForceX = slideDirection * GameConstants.SLIDE_IMPULSE
                        pieceY -= GameConstants.SLIDE_IMPULSE
                        pieceRotation += slideDirection * GameConstants.BOUNCE_ROTATION_DEG
                        isSlidingOnContact = true
                    } else {
                        springForceX = 0f
                    }
                }
            }
            else -> {
                isSlidingOnContact = false
                slideDirection = 0f
                // If snap animation is running and the user is NOT dragging, the piece is in
                // transit (e.g. moving upward toward its grid target) — keep the timer running.
                // If the user IS dragging to open space, cancel snap and let the piece go fluid.
                if (!isSnapAnimating || isDragging) {
                    bounceCount = 0
                    isWaitingToTurnRigidAtBottom = false
                    isWaitingToTurnRigidAtPiece = false
                    isSnapAnimating = false
                }
            }
        }
    }

    private fun canLockCleanly(viewWidth: Int, viewHeight: Int): Boolean {
        val cellWidth = (viewWidth - GameConstants.GRID_LEFT - GameConstants.GRID_RIGHT_MARGIN) / GameConstants.GRID_COLUMNS
        val cellHeight = (viewHeight - GameConstants.GRID_TOP - GameConstants.GRID_BOTTOM_MARGIN) / GameConstants.GRID_ROWS
        val gridBottom = viewHeight - GameConstants.GRID_BOTTOM_MARGIN

        var normalized = pieceRotation % 360f
        if (normalized < 0f) normalized += 360f
        val snappedRotation = (Math.round(normalized / 90f).toInt() % 4) * 90f

        val centers = rotatedBlockCenters(GameConstants.PIECES[currentPiece], pieceX, pieceY, snappedRotation)
        var resting = false
        for ((bx, by) in centers) {
            val gx = ((bx - GameConstants.GRID_LEFT) / cellWidth).toInt()
            val gy = ((by - GameConstants.GRID_TOP) / cellHeight).toInt()
            if (gx !in 0 until GameConstants.GRID_COLUMNS || gy !in 0 until GameConstants.GRID_ROWS) return false
            if (grid[gy][gx] != null) return false
            // Resting: at the floor, would overflow grid bottom, or solid cell directly below.
            if (by + GameConstants.PIECE_SIZE / 2f >= gridBottom) resting = true
            else if (gy + 1 >= GameConstants.GRID_ROWS) resting = true
            else if (grid[gy + 1][gx] != null) resting = true
        }
        return resting
    }

    internal fun turnPieceRigid(viewWidth: Int, viewHeight: Int) {
        onPieceLocked()

        var normalized = pieceRotation % 360f
        if (normalized < 0f) normalized += 360f
        pieceRotation = (Math.round(normalized / 90f).toInt() % 4) * 90f

        val cellWidth = (viewWidth - GameConstants.GRID_LEFT - GameConstants.GRID_RIGHT_MARGIN) / GameConstants.GRID_COLUMNS
        val cellHeight = (viewHeight - GameConstants.GRID_TOP - GameConstants.GRID_BOTTOM_MARGIN) / GameConstants.GRID_ROWS

        val currentPieceShape = GameConstants.PIECES[currentPiece]

        val step = 5f
        while (true) {
            var blocked = false
            for ((bx, by) in rotatedBlockCenters(currentPieceShape, pieceX, pieceY, pieceRotation)) {
                val gx = ((bx - GameConstants.GRID_LEFT) / cellWidth).toInt()
                val gy = ((by - GameConstants.GRID_TOP) / cellHeight).toInt()
                if (gx in 0 until GameConstants.GRID_COLUMNS && gy in 0 until GameConstants.GRID_ROWS && grid[gy][gx] != null) {
                    blocked = true
                    break
                }
            }
            if (!blocked) break
            pieceY -= step
            if (pieceY < GameConstants.GRID_TOP) break
        }

        for ((bx, by) in rotatedBlockCenters(currentPieceShape, pieceX, pieceY, pieceRotation)) {
            val gridX = ((bx - GameConstants.GRID_LEFT) / cellWidth).toInt()
            val gridY = ((by - GameConstants.GRID_TOP) / cellHeight).toInt()
            if (gridX in 0 until GameConstants.GRID_COLUMNS && gridY in 0 until GameConstants.GRID_ROWS) {
                grid[gridY][gridX] = currentPieceColor
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
        springForceX = 0f
        pieceRotation = spawnRotation
        isSlidingOnContact = false
        slideDirection = 0f
        bounceCount = 0
        isSnapAnimating = false

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
            val points = when (linesCleared) {
                1 -> 100
                2 -> 300
                3 -> 500
                4 -> 800
                else -> linesCleared * 100
            }
            score += points
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

    private fun countContactBlocksAtBottom(viewHeight: Int): Int {
        val gridBottom = viewHeight - GameConstants.GRID_BOTTOM_MARGIN
        val halfBlock = GameConstants.PIECE_SIZE / 2 - GameConstants.BLOCK_INSET
        return rotatedBlockCenters(GameConstants.PIECES[currentPiece], pieceX, pieceY, pieceRotation)
            .count { (_, by) -> by + halfBlock > gridBottom }
    }

    private fun countContactBlocksWithPiece(cellWidth: Float, cellHeight: Float): Int {
        val blockSize = GameConstants.PIECE_SIZE
        val h = blockSize / 2f - GameConstants.BLOCK_INSET
        return rotatedBlockCenters(GameConstants.PIECES[currentPiece], pieceX, pieceY, pieceRotation)
            .count { (bx, by) ->
                listOf(
                    bx - h to by - h,
                    bx + h to by - h,
                    bx - h to by + h,
                    bx + h to by + h
                ).any { (cx, cy) ->
                    val cellX = ((cx - GameConstants.GRID_LEFT) / cellWidth).toInt()
                    val cellY = ((cy - GameConstants.GRID_TOP) / cellHeight).toInt()
                    cellX in 0 until GameConstants.GRID_COLUMNS &&
                    cellY in 0 until GameConstants.GRID_ROWS &&
                    grid[cellY][cellX] != null
                }
            }
    }

    private fun computeSlideForceX(viewHeight: Int, cellWidth: Float, cellHeight: Float): Float {
        val blockSize = GameConstants.PIECE_SIZE
        val gridBottom = viewHeight - GameConstants.GRID_BOTTOM_MARGIN
        val centers = rotatedBlockCenters(GameConstants.PIECES[currentPiece], pieceX, pieceY, pieceRotation)

        val h = blockSize / 2f - GameConstants.BLOCK_INSET
        val contactXs = mutableListOf<Float>()
        // Bottom-wall contacts: use the falling block's own center X
        centers.forEach { (bx, by) ->
            if (by + h > gridBottom) contactXs.add(bx)
        }
        // Piece-to-piece contacts: use the placed block's center X so that direction
        // is determined by where the obstacle actually is, not which side of the falling
        // piece the contacting block happens to sit on.
        val hitCells = mutableSetOf<Pair<Int, Int>>()
        centers.forEach { (bx, by) ->
            listOf(
                bx - h to by - h,
                bx + h to by - h,
                bx - h to by + h,
                bx + h to by + h
            ).forEach { (cx, cy) ->
                val cellX = ((cx - GameConstants.GRID_LEFT) / cellWidth).toInt()
                val cellY = ((cy - GameConstants.GRID_TOP) / cellHeight).toInt()
                if (cellX in 0 until GameConstants.GRID_COLUMNS &&
                    cellY in 0 until GameConstants.GRID_ROWS &&
                    grid[cellY][cellX] != null) {
                    hitCells.add(cellX to cellY)
                }
            }
        }
        hitCells.forEach { (cellX, _) ->
            contactXs.add(GameConstants.GRID_LEFT + (cellX + 0.5f) * cellWidth)
        }

        if (contactXs.isEmpty()) return 0f
        val pieceCenterX = centers.map { it.first }.average().toFloat()
        val contactCenterX = contactXs.average().toFloat()
        return if (contactCenterX > pieceCenterX) -GameConstants.SLIDE_IMPULSE else GameConstants.SLIDE_IMPULSE
    }

    private fun beginSnapAnimation(viewWidth: Int, viewHeight: Int) {
        val cellWidth  = (viewWidth  - GameConstants.GRID_LEFT - GameConstants.GRID_RIGHT_MARGIN)  / GameConstants.GRID_COLUMNS
        val cellHeight = (viewHeight - GameConstants.GRID_TOP  - GameConstants.GRID_BOTTOM_MARGIN) / GameConstants.GRID_ROWS

        var normalized = pieceRotation % 360f
        if (normalized < 0f) normalized += 360f
        snapTargetRotation = (Math.round(normalized / 90f).toInt() % 4) * 90f

        val centers = rotatedBlockCenters(GameConstants.PIECES[currentPiece], pieceX, pieceY, snapTargetRotation)
        var totalDx = 0f; var totalDy = 0f; var count = 0
        for ((bx, by) in centers) {
            val gx = ((bx - GameConstants.GRID_LEFT) / cellWidth).toInt().coerceIn(0, GameConstants.GRID_COLUMNS - 1)
            val gy = ((by - GameConstants.GRID_TOP) / cellHeight).toInt().coerceIn(0, GameConstants.GRID_ROWS - 1)
            totalDx += GameConstants.GRID_LEFT + (gx + 0.5f) * cellWidth - bx
            totalDy += GameConstants.GRID_TOP  + (gy + 0.5f) * cellHeight - by
            count++
        }
        if (count > 0) {
            snapTargetX = pieceX + totalDx / count
            snapTargetY = pieceY + totalDy / count
        } else {
            snapTargetX = pieceX
            snapTargetY = pieceY
        }
        bounceCount = 0
        isSnapAnimating = true
    }

    private fun applySnapPull(t: Float) {
        val xStrength   = t * GameConstants.SNAP_PULL_SPEED
        val rotStrength = t * GameConstants.SNAP_ROTATION_SPEED
        pieceX        += (snapTargetX - pieceX) * xStrength
        pieceY        += (snapTargetY - pieceY) * xStrength
        pieceRotation  = lerpAngleDeg(pieceRotation, snapTargetRotation, rotStrength)
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
        for ((bx, by) in rotatedBlockCenters(GameConstants.PIECES[currentPiece], pieceX, testY, pieceRotation)) {
            val gx = ((bx - GameConstants.GRID_LEFT) / cellWidth).toInt()
            val gy = ((by - GameConstants.GRID_TOP) / cellHeight).toInt()
            if (gx in 0 until GameConstants.GRID_COLUMNS && gy in 0 until GameConstants.GRID_ROWS && grid[gy][gx] != null) {
                return true
            }
        }
        return false
    }
}
