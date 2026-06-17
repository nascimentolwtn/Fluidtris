package com.libuy.fluidtris

import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class ActivePiece(
    var type: Int,
    var color: Int,
    var x: Float,
    var y: Float,
    var rotation: Float,
    var springForceX: Float = 0f,
    var isSlidingOnContact: Boolean = false,
    var slideDirection: Float = 0f,
    var bounceCount: Int = 0,
    var isWaitingToTurnRigidAtBottom: Boolean = false,
    var isWaitingToTurnRigidAtPiece: Boolean = false,
    var bottomCollisionTime: Long = 0L,
    var pieceCollisionTime: Long = 0L,
    var isSnapAnimating: Boolean = false,
    var snapTargetX: Float = 0f,
    var snapTargetY: Float = 0f,
    var snapTargetRotation: Float = 0f
)

internal class GameEngine(
    private val onPieceLocked: () -> Unit,
    private val onLineCleared: () -> Unit,
    private val onHighScoreBeat: (newScore: Int) -> Unit = {},
    private val onLevelUp: () -> Unit = {}
) {
    val grid = Array(GameConstants.GRID_ROWS) { Array<Int?>(GameConstants.GRID_COLUMNS) { null } }
    var score = 0
    var highScore = 0

    val fallingPieces = mutableListOf<ActivePiece>()
    var draggedPiece: ActivePiece? = null

    // Touch state for the actively dragged piece
    private var isDraggingCenter = false
    private var touchedBlockRow = 0
    private var touchedBlockCol = 0
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    var isPaused = false
    var isGameOver = true
    private var wasManuallyPausedBeforeSystemPause = false
    var justBeatHighScore = false
    var isBeatingHighScore = false

    var nextPiece = 1
    var nextPieceColor = GameConstants.PIECE_COLORS[1]
    var nextPieceRotation = 0f

    internal var currentTimeMs: () -> Long = System::currentTimeMillis

    // ── Backward-compat properties (tests and FluidTetrisView use these) ──────

    var pieceX: Float
        get() = fallingPieces.firstOrNull()?.x ?: 0f
        set(v) { fallingPieces.firstOrNull()?.x = v }

    var pieceY: Float
        get() = fallingPieces.firstOrNull()?.y ?: 0f
        set(v) { fallingPieces.firstOrNull()?.y = v }

    var pieceRotation: Float
        get() = fallingPieces.firstOrNull()?.rotation ?: 0f
        set(v) { fallingPieces.firstOrNull()?.rotation = v }

    var currentPiece: Int
        get() = fallingPieces.firstOrNull()?.type ?: 0
        set(v) {
            fallingPieces.firstOrNull()?.let {
                it.type = v
                it.color = GameConstants.PIECE_COLORS[v]
            }
        }

    val currentPieceColor: Int get() = fallingPieces.firstOrNull()?.color ?: 0

    var isDragging: Boolean
        get() = draggedPiece != null
        set(v) { draggedPiece = if (v) fallingPieces.firstOrNull() else null }

    val otherFallingPieces: List<ActivePiece>
        get() = if (fallingPieces.size > 1) fallingPieces.subList(1, fallingPieces.size) else emptyList()

    // ── Update ────────────────────────────────────────────────────────────────

    fun update(viewWidth: Int, viewHeight: Int) {
        if (viewWidth == 0 || viewHeight == 0) return
        if (isPaused || isGameOver) return

        if (fallingPieces.isEmpty()) spawnNextPiece(viewWidth, viewHeight)

        val scaledGravity = GameConstants.GRAVITY * getLevelMultiplier()

        for (piece in fallingPieces.toList()) {
            if (isGameOver) break
            if (!fallingPieces.contains(piece)) continue  // already locked this frame

            val pieceIsDragging = draggedPiece === piece

            if (!piece.isWaitingToTurnRigidAtBottom && !piece.isWaitingToTurnRigidAtPiece && !pieceIsDragging) {
                piece.y += scaledGravity
            }

            if (!pieceIsDragging && piece.springForceX != 0f) {
                piece.x += piece.springForceX
                piece.springForceX *= GameConstants.SPRING_DAMPING
                if (kotlin.math.abs(piece.springForceX) < 0.1f) piece.springForceX = 0f
            }

            keepPieceInsideWalls(piece, viewWidth)
            checkPieceCollisions(piece, pieceIsDragging, viewWidth, viewHeight)

            if (!pieceIsDragging && piece.isSnapAnimating &&
                (piece.isWaitingToTurnRigidAtBottom || piece.isWaitingToTurnRigidAtPiece)) {
                val elapsed = when {
                    piece.isWaitingToTurnRigidAtBottom -> currentTimeMs() - piece.bottomCollisionTime
                    else -> currentTimeMs() - piece.pieceCollisionTime
                }
                val t = (elapsed / GameConstants.LOCK_DELAY_MS.toFloat()).coerceIn(0f, 1f)
                applySnapPull(piece, t)
            }

            var didSolidify = false
            if (piece.isWaitingToTurnRigidAtBottom) {
                if (currentTimeMs() - piece.bottomCollisionTime >= GameConstants.LOCK_DELAY_MS) {
                    if (draggedPiece === piece) draggedPiece = null
                    turnPieceRigidInternal(piece, viewWidth, viewHeight)
                    didSolidify = true
                }
            }
            if (!didSolidify && piece.isWaitingToTurnRigidAtPiece) {
                if (currentTimeMs() - piece.pieceCollisionTime >= GameConstants.LOCK_DELAY_MS) {
                    if (draggedPiece === piece) draggedPiece = null
                    moveUpUntilClear(piece, viewWidth, viewHeight)
                    turnPieceRigidInternal(piece, viewWidth, viewHeight)
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
        draggedPiece = null
        isDraggingCenter = false
        touchedBlockRow = 0
        touchedBlockCol = 0
        lastTouchX = 0f
        lastTouchY = 0f
        justBeatHighScore = false
        isBeatingHighScore = false
        fallingPieces.clear()

        val pieceType = Random.nextInt(GameConstants.PIECES.size)
        fallingPieces.add(ActivePiece(
            type = pieceType,
            color = GameConstants.PIECE_COLORS[pieceType],
            x = (viewWidth / 2) - 50f,
            y = GameConstants.GRID_TOP,
            rotation = 0f
        ))

        nextPiece = Random.nextInt(GameConstants.PIECES.size)
        nextPieceColor = GameConstants.PIECE_COLORS[nextPiece]
        nextPieceRotation = Random.nextInt(4) * 90f
    }

    fun onTouchDown(x: Float, y: Float): Boolean {
        for (piece in fallingPieces) {
            val hitCell = hitCellFromTouch(x, y, piece.x, piece.y, piece.rotation, GameConstants.PIECES[piece.type])
            if (hitCell != null) {
                draggedPiece = piece
                piece.springForceX = 0f
                isDraggingCenter = hitCell in GameConstants.PIECE_CENTER_CELLS[piece.type]
                touchedBlockRow = hitCell.first
                touchedBlockCol = hitCell.second
                lastTouchX = x
                lastTouchY = y
                return true
            }
        }
        return false
    }

    fun onTouchMove(x: Float, y: Float, viewWidth: Int, viewHeight: Int) {
        val piece = draggedPiece ?: return
        val dx = x - lastTouchX
        val dy = y - lastTouchY
        piece.springForceX = dx * GameConstants.SPRING_CARRY
        var appliedDx = 0f
        var appliedDy = 0f
        val prevRotation = piece.rotation

        if (isDraggingCenter) {
            appliedDx = dx
            appliedDy = effectiveVerticalDrag(dy, GameConstants.UPWARD_DRAG_FACTOR)
            piece.x += appliedDx
            piece.y += appliedDy
            keepPieceInsideWalls(piece, viewWidth)
        } else {
            val shape = GameConstants.PIECES[piece.type]
            val rcx = piece.x + shape[0].size * GameConstants.PIECE_SIZE / 2f
            val rcy = piece.y + shape.size * GameConstants.PIECE_SIZE / 2f
            val angle = piece.rotation * Math.PI / 180.0
            val cosA = cos(angle)
            val sinA = sin(angle)
            val bux = piece.x + (touchedBlockCol + 0.5f) * GameConstants.PIECE_SIZE
            val buy = piece.y + (touchedBlockRow + 0.5f) * GameConstants.PIECE_SIZE
            val bdx = (bux - rcx).toDouble()
            val bdy = (buy - rcy).toDouble()
            val bx = (rcx + bdx * cosA - bdy * sinA).toFloat()
            val by = (rcy + bdx * sinA + bdy * cosA).toFloat()
            val vx = rcx - bx
            val vy = rcy - by
            piece.rotation -= rotationDeltaFromDrag(vx, vy, dx, dy, GameConstants.ROTATION_SENSITIVITY)
            appliedDx = dx
            appliedDy = effectiveVerticalDrag(dy, GameConstants.UPWARD_DRAG_FACTOR)
            piece.x += appliedDx
            piece.y += appliedDy
            keepPieceInsideWalls(piece, viewWidth)
        }

        lastTouchX = x
        lastTouchY = y

        val cellWidth = (viewWidth - GameConstants.GRID_LEFT - GameConstants.GRID_RIGHT_MARGIN) / GameConstants.GRID_COLUMNS
        val cellHeight = (viewHeight - GameConstants.GRID_TOP - GameConstants.GRID_BOTTOM_MARGIN) / GameConstants.GRID_ROWS
        val cellX = ((piece.x - GameConstants.GRID_LEFT) / cellWidth).toInt()
        val cellY = ((piece.y - GameConstants.GRID_TOP) / cellHeight).toInt()
        if (cellX in 0 until GameConstants.GRID_COLUMNS && cellY in 0 until GameConstants.GRID_ROWS) {
            if (checkPieceAtBottom(piece, viewHeight)) {
                draggedPiece = null
                turnPieceRigidInternal(piece, viewWidth, viewHeight)
                return
            }
            if (checkPieceCollidingWithGrid(piece, cellWidth, cellHeight)) {
                piece.x -= appliedDx
                piece.y -= appliedDy
                piece.rotation = prevRotation
                draggedPiece = null
            }
        }
    }

    fun onTouchUp() {
        draggedPiece = null
    }

    fun getCollisionSolidity(piece: ActivePiece): Float {
        val elapsed = when {
            piece.isWaitingToTurnRigidAtBottom -> currentTimeMs() - piece.bottomCollisionTime
            piece.isWaitingToTurnRigidAtPiece  -> currentTimeMs() - piece.pieceCollisionTime
            else -> return 0f
        }
        val t = (elapsed / GameConstants.LOCK_DELAY_MS.toFloat()).coerceIn(0f, 1f)
        val oscillation = sin(t * Math.PI * 6.0).toFloat()
        return (t + oscillation * (1f - t) * 0.6f).coerceIn(0f, 1f)
    }

    fun getCollisionSolidity(): Float = fallingPieces.firstOrNull()?.let { getCollisionSolidity(it) } ?: 0f

    private fun computeLevel(): Int = score / GameConstants.NEXT_LEVEL_SCORE + 1

    fun getLevel(): Int = computeLevel()

    private fun getLevelMultiplier(): Float {
        val level = computeLevel()
        return (1 + (level - 1) * GameConstants.LEVEL_DIFFICULTY_FACTOR).coerceAtMost(GameConstants.MAX_LEVEL_MULTIPLIER)
    }

    fun pause() { isPaused = true }
    fun resume() { isPaused = false }
    fun togglePause() { isPaused = !isPaused }

    fun onFocusLost() {
        wasManuallyPausedBeforeSystemPause = isPaused
        isPaused = true
    }

    fun onFocusGained() {
        isPaused = wasManuallyPausedBeforeSystemPause
    }

    fun onNextPieceButton(viewWidth: Int, viewHeight: Int) {
        if (isGameOver || isPaused) return
        draggedPiece = null
        val newPiece = ActivePiece(
            type = nextPiece,
            color = nextPieceColor,
            x = (viewWidth / 2) - 50f,
            y = GameConstants.GRID_TOP,
            rotation = nextPieceRotation
        )
        fallingPieces.add(0, newPiece)
        nextPiece = Random.nextInt(GameConstants.PIECES.size)
        nextPieceColor = GameConstants.PIECE_COLORS[nextPiece]
        nextPieceRotation = Random.nextInt(4) * 90f
    }

    // ── Internal lock / spawn ─────────────────────────────────────────────────

    private fun turnPieceRigidInternal(piece: ActivePiece, viewWidth: Int, viewHeight: Int) {
        onPieceLocked()

        var normalized = piece.rotation % 360f
        if (normalized < 0f) normalized += 360f
        piece.rotation = (Math.round(normalized / 90f).toInt() % 4) * 90f

        val cellWidth = (viewWidth - GameConstants.GRID_LEFT - GameConstants.GRID_RIGHT_MARGIN) / GameConstants.GRID_COLUMNS
        val cellHeight = (viewHeight - GameConstants.GRID_TOP - GameConstants.GRID_BOTTOM_MARGIN) / GameConstants.GRID_ROWS
        val shape = GameConstants.PIECES[piece.type]

        val step = 5f
        while (true) {
            var blocked = false
            for ((bx, by) in rotatedBlockCenters(shape, piece.x, piece.y, piece.rotation)) {
                val gx = ((bx - GameConstants.GRID_LEFT) / cellWidth).toInt()
                val gy = ((by - GameConstants.GRID_TOP) / cellHeight).toInt()
                if (gx in 0 until GameConstants.GRID_COLUMNS && gy in 0 until GameConstants.GRID_ROWS && grid[gy][gx] != null) {
                    blocked = true; break
                }
            }
            if (!blocked) break
            piece.y -= step
            if (piece.y < GameConstants.GRID_TOP) break
        }

        for ((bx, by) in rotatedBlockCenters(shape, piece.x, piece.y, piece.rotation)) {
            val gx = ((bx - GameConstants.GRID_LEFT) / cellWidth).toInt()
            val gy = ((by - GameConstants.GRID_TOP) / cellHeight).toInt()
            if (gx in 0 until GameConstants.GRID_COLUMNS && gy in 0 until GameConstants.GRID_ROWS) {
                grid[gy][gx] = piece.color
            }
        }

        val linesCleared = checkLines()
        if (linesCleared) {
            for (fallingPiece in fallingPieces) {
                if (fallingPiece.isSnapAnimating) {
                    fallingPiece.isSnapAnimating = false
                    fallingPiece.isWaitingToTurnRigidAtBottom = false
                    fallingPiece.isWaitingToTurnRigidAtPiece = false
                }
            }
        }

        if (draggedPiece === piece) draggedPiece = null
        fallingPieces.remove(piece)

        if (fallingPieces.isEmpty()) {
            spawnNextPiece(viewWidth, viewHeight)
        }

        if (grid[0].any { it != null }) {
            isGameOver = true
        }
    }

    private fun spawnNextPiece(viewWidth: Int, viewHeight: Int) {
        fallingPieces.add(0, ActivePiece(
            type = nextPiece,
            color = nextPieceColor,
            x = (viewWidth / 2) - 50f,
            y = GameConstants.GRID_TOP,
            rotation = nextPieceRotation
        ))
        nextPiece = Random.nextInt(GameConstants.PIECES.size)
        nextPieceColor = GameConstants.PIECE_COLORS[nextPiece]
        nextPieceRotation = Random.nextInt(4) * 90f
    }

    internal fun checkLines(): Boolean {
        var linesCleared = 0
        var i = GameConstants.GRID_ROWS - 1
        while (i >= 0) {
            if (grid[i].all { it != null }) {
                for (j in i downTo 1) {
                    grid[j] = grid[j - 1].clone()
                }
                grid[0] = Array(GameConstants.GRID_COLUMNS) { null }
                linesCleared++
            } else {
                i--
            }
        }
        if (linesCleared > 0) {
            val prevLevel = getLevel()
            val points = when (linesCleared) {
                1 -> 100
                2 -> 300
                3 -> 500
                4 -> 800
                else -> linesCleared * 100
            }
            score += points
            val newLevel = getLevel()
            isBeatingHighScore = score > highScore
            if (score > highScore) {
                highScore = score
                justBeatHighScore = true
                onHighScoreBeat(score)
            }
            onLineCleared()
            if (newLevel > prevLevel) onLevelUp()
            return true
        }
        return false
    }

    // ── Backward-compat wrappers (called by tests) ────────────────────────────

    internal fun turnPieceRigid(viewWidth: Int, viewHeight: Int) {
        fallingPieces.firstOrNull()?.let { turnPieceRigidInternal(it, viewWidth, viewHeight) }
    }

    internal fun lockPieceAtBottom(viewWidth: Int, viewHeight: Int) {
        fallingPieces.firstOrNull()?.let { turnPieceRigidInternal(it, viewWidth, viewHeight) }
    }

    internal fun isPieceAtBottom(viewHeight: Int): Boolean =
        fallingPieces.firstOrNull()?.let { checkPieceAtBottom(it, viewHeight) } ?: false

    internal fun isPieceCollidingWithAnotherPiece(cellWidth: Float, cellHeight: Float): Boolean =
        fallingPieces.firstOrNull()?.let { checkPieceCollidingWithGrid(it, cellWidth, cellHeight) } ?: false

    // ── Per-piece physics helpers ─────────────────────────────────────────────

    private fun keepPieceInsideWalls(piece: ActivePiece, viewWidth: Int) {
        val centers = rotatedBlockCenters(GameConstants.PIECES[piece.type], piece.x, piece.y, piece.rotation)
        piece.x = clampPieceXByCenters(
            centers, piece.x,
            GameConstants.GRID_LEFT,
            viewWidth - GameConstants.GRID_RIGHT_MARGIN,
            GameConstants.PIECE_SIZE / 2
        )
    }

    private fun checkPieceCollisions(piece: ActivePiece, pieceIsDragging: Boolean, viewWidth: Int, viewHeight: Int) {
        val cellWidth  = (viewWidth  - GameConstants.GRID_LEFT - GameConstants.GRID_RIGHT_MARGIN)  / GameConstants.GRID_COLUMNS
        val cellHeight = (viewHeight - GameConstants.GRID_TOP  - GameConstants.GRID_BOTTOM_MARGIN) / GameConstants.GRID_ROWS

        val bottomContacts    = countContactBlocksAtBottom(piece, viewHeight)
        val pieceContacts     = countContactBlocksWithPiece(piece, cellWidth, cellHeight)
        val livePieceContacts = if (!pieceIsDragging) countContactBlocksWithAnimatingPieces(piece) else 0
        val totalContacts     = bottomContacts + pieceContacts

        when {
            piece.bounceCount >= 4 && totalContacts >= 1 -> {
                piece.isSnapAnimating = false
                piece.isSlidingOnContact = false
                piece.slideDirection = 0f
                if (bottomContacts > 0) {
                    if (!piece.isWaitingToTurnRigidAtBottom) {
                        piece.isWaitingToTurnRigidAtBottom = true
                        piece.bottomCollisionTime = currentTimeMs()
                        piece.springForceX = 0f
                    }
                } else { piece.isWaitingToTurnRigidAtBottom = false }
                if (pieceContacts > 0) {
                    if (!piece.isWaitingToTurnRigidAtPiece) {
                        piece.isWaitingToTurnRigidAtPiece = true
                        piece.pieceCollisionTime = currentTimeMs()
                        piece.springForceX = 0f
                    }
                } else { piece.isWaitingToTurnRigidAtPiece = false }
            }
            totalContacts >= 1 && canPieceLockCleanly(piece, viewWidth, viewHeight) -> {
                piece.isSlidingOnContact = false
                piece.slideDirection = 0f
                if (bottomContacts > 0) {
                    if (!piece.isWaitingToTurnRigidAtBottom) {
                        piece.isWaitingToTurnRigidAtBottom = true
                        piece.bottomCollisionTime = currentTimeMs()
                        piece.springForceX = 0f
                        if (!piece.isSnapAnimating) beginSnapAnimation(piece, viewWidth, viewHeight)
                    }
                } else { piece.isWaitingToTurnRigidAtBottom = false }
                if (pieceContacts > 0) {
                    if (!piece.isWaitingToTurnRigidAtPiece) {
                        piece.isWaitingToTurnRigidAtPiece = true
                        piece.pieceCollisionTime = currentTimeMs()
                        piece.springForceX = 0f
                        if (!piece.isSnapAnimating) beginSnapAnimation(piece, viewWidth, viewHeight)
                    }
                } else { piece.isWaitingToTurnRigidAtPiece = false }
            }
            totalContacts >= 1 -> {
                if (!piece.isSnapAnimating) {
                    piece.isWaitingToTurnRigidAtBottom = false
                    piece.isWaitingToTurnRigidAtPiece = false
                    if (!piece.isSlidingOnContact) {
                        piece.bounceCount++
                        val force = computeSlideForceX(piece, viewHeight, cellWidth, cellHeight)
                        piece.slideDirection = if (force >= 0f) 1f else -1f
                        piece.springForceX = piece.slideDirection * GameConstants.SLIDE_IMPULSE
                        piece.y -= GameConstants.SLIDE_IMPULSE
                        piece.rotation += piece.slideDirection * GameConstants.BOUNCE_ROTATION_DEG
                        piece.isSlidingOnContact = true
                    } else {
                        piece.springForceX = 0f
                    }
                }
            }
            livePieceContacts >= 1 -> {
                if (!piece.isSnapAnimating) {
                    if (canPieceLockCleanly(piece, viewWidth, viewHeight)) {
                        piece.isSlidingOnContact = false
                        piece.slideDirection = 0f
                        if (!piece.isWaitingToTurnRigidAtPiece) {
                            piece.isWaitingToTurnRigidAtPiece = true
                            piece.pieceCollisionTime = currentTimeMs()
                            piece.springForceX = 0f
                            beginSnapAnimation(piece, viewWidth, viewHeight)
                        }
                    } else {
                        piece.isWaitingToTurnRigidAtBottom = false
                        piece.isWaitingToTurnRigidAtPiece = false
                        if (!piece.isSlidingOnContact) {
                            val force = computeSlideForceXFromAnimatingPieces(piece)
                            piece.slideDirection = if (force >= 0f) 1f else -1f
                            piece.springForceX = piece.slideDirection * GameConstants.SLIDE_IMPULSE
                            piece.y -= GameConstants.SLIDE_IMPULSE
                            piece.rotation += piece.slideDirection * GameConstants.BOUNCE_ROTATION_DEG
                            piece.isSlidingOnContact = true
                        } else {
                            piece.springForceX = 0f
                        }
                    }
                }
            }
            else -> {
                piece.isSlidingOnContact = false
                piece.slideDirection = 0f
                if (!piece.isSnapAnimating || pieceIsDragging) {
                    piece.bounceCount = 0
                    piece.isWaitingToTurnRigidAtBottom = false
                    piece.isWaitingToTurnRigidAtPiece = false
                    piece.isSnapAnimating = false
                }
            }
        }
    }

    private fun animatingPeerOccupiesCell(gx: Int, gy: Int, exclude: ActivePiece, cellWidth: Float, cellHeight: Float): Boolean {
        return fallingPieces.any { other ->
            other !== exclude && other.isSnapAnimating &&
            rotatedBlockCenters(GameConstants.PIECES[other.type], other.x, other.y, other.rotation)
                .any { (bx, by) ->
                    ((bx - GameConstants.GRID_LEFT) / cellWidth).toInt() == gx &&
                    ((by - GameConstants.GRID_TOP) / cellHeight).toInt() == gy
                }
        }
    }

    private fun canPieceLockCleanly(piece: ActivePiece, viewWidth: Int, viewHeight: Int): Boolean {
        val cellWidth  = (viewWidth  - GameConstants.GRID_LEFT - GameConstants.GRID_RIGHT_MARGIN)  / GameConstants.GRID_COLUMNS
        val cellHeight = (viewHeight - GameConstants.GRID_TOP  - GameConstants.GRID_BOTTOM_MARGIN) / GameConstants.GRID_ROWS
        val gridBottom = viewHeight - GameConstants.GRID_BOTTOM_MARGIN

        var normalized = piece.rotation % 360f
        if (normalized < 0f) normalized += 360f
        val snappedRotation = (Math.round(normalized / 90f).toInt() % 4) * 90f

        val centers = rotatedBlockCenters(GameConstants.PIECES[piece.type], piece.x, piece.y, snappedRotation)
        val ownCells = centers.mapTo(HashSet()) { (bx, by) ->
            ((bx - GameConstants.GRID_LEFT) / cellWidth).toInt() to
            ((by - GameConstants.GRID_TOP) / cellHeight).toInt()
        }

        var resting = false
        for ((bx, by) in centers) {
            val gx = ((bx - GameConstants.GRID_LEFT) / cellWidth).toInt()
            val gy = ((by - GameConstants.GRID_TOP) / cellHeight).toInt()
            if (gx !in 0 until GameConstants.GRID_COLUMNS || gy !in 0 until GameConstants.GRID_ROWS) return false
            if (grid[gy][gx] != null) return false
            if (by + GameConstants.PIECE_SIZE / 2f >= gridBottom) resting = true
            else if (gy + 1 >= GameConstants.GRID_ROWS) resting = true
            else if (grid[gy + 1][gx] != null) resting = true
            else if ((gx to gy + 1) !in ownCells && animatingPeerOccupiesCell(gx, gy + 1, piece, cellWidth, cellHeight)) resting = true
        }
        return resting
    }

    private fun checkPieceAtBottom(piece: ActivePiece, viewHeight: Int): Boolean {
        val gridBottom = viewHeight - GameConstants.GRID_BOTTOM_MARGIN
        return rotatedBlockCenters(GameConstants.PIECES[piece.type], piece.x, piece.y, piece.rotation)
            .any { (_, by) -> by + GameConstants.PIECE_SIZE / 2 > gridBottom }
    }

    private fun checkPieceCollidingWithGrid(piece: ActivePiece, cellWidth: Float, cellHeight: Float): Boolean {
        val blockSize = GameConstants.PIECE_SIZE
        for ((bx, by) in rotatedBlockCenters(GameConstants.PIECES[piece.type], piece.x, piece.y, piece.rotation)) {
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

    private fun countContactBlocksAtBottom(piece: ActivePiece, viewHeight: Int): Int {
        val gridBottom = viewHeight - GameConstants.GRID_BOTTOM_MARGIN
        val halfBlock = GameConstants.PIECE_SIZE / 2 - GameConstants.BLOCK_INSET
        return rotatedBlockCenters(GameConstants.PIECES[piece.type], piece.x, piece.y, piece.rotation)
            .count { (_, by) -> by + halfBlock > gridBottom }
    }

    private fun countContactBlocksWithPiece(piece: ActivePiece, cellWidth: Float, cellHeight: Float): Int {
        val blockSize = GameConstants.PIECE_SIZE
        val h = blockSize / 2f - GameConstants.BLOCK_INSET
        return rotatedBlockCenters(GameConstants.PIECES[piece.type], piece.x, piece.y, piece.rotation)
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

    private fun countContactBlocksWithAnimatingPieces(piece: ActivePiece): Int {
        if (piece.isSnapAnimating) return 0
        val blockSize = GameConstants.PIECE_SIZE
        val h = blockSize / 2f - GameConstants.BLOCK_INSET
        val halfSize = blockSize / 2f
        val animatingOthers = fallingPieces.filter { it !== piece && it.isSnapAnimating }
        if (animatingOthers.isEmpty()) return 0
        return rotatedBlockCenters(GameConstants.PIECES[piece.type], piece.x, piece.y, piece.rotation)
            .count { (bx, by) ->
                listOf(bx - h to by - h, bx + h to by - h, bx - h to by + h, bx + h to by + h)
                    .any { (cx, cy) ->
                        animatingOthers.any { other ->
                            rotatedBlockCenters(GameConstants.PIECES[other.type], other.x, other.y, other.rotation)
                                .any { (obx, oby) ->
                                    kotlin.math.abs(cx - obx) < halfSize && kotlin.math.abs(cy - oby) < halfSize
                                }
                        }
                    }
            }
    }

    private fun computeSlideForceXFromAnimatingPieces(piece: ActivePiece): Float {
        val blockSize = GameConstants.PIECE_SIZE
        val h = blockSize / 2f - GameConstants.BLOCK_INSET
        val halfSize = blockSize / 2f
        val centers = rotatedBlockCenters(GameConstants.PIECES[piece.type], piece.x, piece.y, piece.rotation)
        val pieceCenterX = centers.map { it.first }.average().toFloat()
        val animatingOthers = fallingPieces.filter { it !== piece && it.isSnapAnimating }
        val contactXs = mutableListOf<Float>()
        centers.forEach { (bx, by) ->
            listOf(bx - h to by - h, bx + h to by - h, bx - h to by + h, bx + h to by + h)
                .forEach { (cx, cy) ->
                    animatingOthers.forEach { other ->
                        rotatedBlockCenters(GameConstants.PIECES[other.type], other.x, other.y, other.rotation)
                            .forEach { (obx, oby) ->
                                if (kotlin.math.abs(cx - obx) < halfSize && kotlin.math.abs(cy - oby) < halfSize) {
                                    contactXs.add(obx)
                                }
                            }
                    }
                }
        }
        if (contactXs.isEmpty()) return 0f
        val contactCenterX = contactXs.average().toFloat()
        return if (contactCenterX > pieceCenterX) -GameConstants.SLIDE_IMPULSE else GameConstants.SLIDE_IMPULSE
    }

    private fun computeSlideForceX(piece: ActivePiece, viewHeight: Int, cellWidth: Float, cellHeight: Float): Float {
        val blockSize = GameConstants.PIECE_SIZE
        val gridBottom = viewHeight - GameConstants.GRID_BOTTOM_MARGIN
        val centers = rotatedBlockCenters(GameConstants.PIECES[piece.type], piece.x, piece.y, piece.rotation)

        val h = blockSize / 2f - GameConstants.BLOCK_INSET
        val contactXs = mutableListOf<Float>()
        centers.forEach { (bx, by) ->
            if (by + h > gridBottom) contactXs.add(bx)
        }
        val hitCells = mutableSetOf<Pair<Int, Int>>()
        centers.forEach { (bx, by) ->
            listOf(bx - h to by - h, bx + h to by - h, bx - h to by + h, bx + h to by + h)
                .forEach { (cx, cy) ->
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

    private fun beginSnapAnimation(piece: ActivePiece, viewWidth: Int, viewHeight: Int) {
        val cellWidth  = (viewWidth  - GameConstants.GRID_LEFT - GameConstants.GRID_RIGHT_MARGIN)  / GameConstants.GRID_COLUMNS
        val cellHeight = (viewHeight - GameConstants.GRID_TOP  - GameConstants.GRID_BOTTOM_MARGIN) / GameConstants.GRID_ROWS

        var normalized = piece.rotation % 360f
        if (normalized < 0f) normalized += 360f
        piece.snapTargetRotation = (Math.round(normalized / 90f).toInt() % 4) * 90f

        val centers = rotatedBlockCenters(GameConstants.PIECES[piece.type], piece.x, piece.y, piece.snapTargetRotation)
        var totalDx = 0f; var totalDy = 0f; var count = 0
        for ((bx, by) in centers) {
            val gx = ((bx - GameConstants.GRID_LEFT) / cellWidth).toInt().coerceIn(0, GameConstants.GRID_COLUMNS - 1)
            val gy = ((by - GameConstants.GRID_TOP) / cellHeight).toInt().coerceIn(0, GameConstants.GRID_ROWS - 1)
            totalDx += GameConstants.GRID_LEFT + (gx + 0.5f) * cellWidth - bx
            totalDy += GameConstants.GRID_TOP  + (gy + 0.5f) * cellHeight - by
            count++
        }
        if (count > 0) {
            piece.snapTargetX = piece.x + totalDx / count
            piece.snapTargetY = piece.y + totalDy / count
        } else {
            piece.snapTargetX = piece.x
            piece.snapTargetY = piece.y
        }
        piece.bounceCount = 0
        piece.isSnapAnimating = true
    }

    private fun applySnapPull(piece: ActivePiece, t: Float) {
        val xStrength   = t * GameConstants.SNAP_PULL_SPEED
        val rotStrength = t * GameConstants.SNAP_ROTATION_SPEED
        piece.x        += (piece.snapTargetX - piece.x) * xStrength
        piece.y        += (piece.snapTargetY - piece.y) * xStrength
        piece.rotation  = lerpAngleDeg(piece.rotation, piece.snapTargetRotation, rotStrength)
    }

    private fun moveUpUntilClear(piece: ActivePiece, viewWidth: Int, viewHeight: Int) {
        val cellWidth  = (viewWidth  - GameConstants.GRID_LEFT - GameConstants.GRID_RIGHT_MARGIN)  / GameConstants.GRID_COLUMNS
        val cellHeight = (viewHeight - GameConstants.GRID_TOP  - GameConstants.GRID_BOTTOM_MARGIN) / GameConstants.GRID_ROWS
        val step = 5f
        var moved = false

        while (doesPieceCollideWithGridAtY(piece, piece.y, cellWidth, cellHeight)) {
            piece.y -= step
            moved = true
            if (piece.y < GameConstants.GRID_TOP) break
        }
        if (moved) keepPieceInsideWalls(piece, viewWidth)
    }

    private fun doesPieceCollideWithGridAtY(piece: ActivePiece, testY: Float, cellWidth: Float, cellHeight: Float): Boolean {
        for ((bx, by) in rotatedBlockCenters(GameConstants.PIECES[piece.type], piece.x, testY, piece.rotation)) {
            val gx = ((bx - GameConstants.GRID_LEFT) / cellWidth).toInt()
            val gy = ((by - GameConstants.GRID_TOP) / cellHeight).toInt()
            if (gx in 0 until GameConstants.GRID_COLUMNS && gy in 0 until GameConstants.GRID_ROWS && grid[gy][gx] != null) {
                return true
            }
        }
        return false
    }
}
