package com.libuy.fluidtris

import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

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
    val leftEdge = x + minCol * pieceSize
    if (leftEdge < leftWall) x = leftWall - minCol * pieceSize
    val rightEdge = x + (maxCol + 1) * pieceSize
    if (rightEdge > rightWall) x = rightWall - (maxCol + 1) * pieceSize
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

internal fun clampPieceXByCenters(
    centers: List<Pair<Float, Float>>,
    pieceX: Float,
    leftWall: Float,
    rightWall: Float,
    halfBlock: Float
): Float {
    if (centers.isEmpty()) return pieceX
    val minBx = centers.minOf { it.first } - halfBlock
    val maxBx = centers.maxOf { it.first } + halfBlock
    var x = pieceX
    if (minBx < leftWall) x += leftWall - minBx
    if (maxBx > rightWall) x -= maxBx - rightWall
    return x
}

// Returns the screen-space center of each filled block after applying the canvas rotation transform.
// Mirrors the canvas.rotate(pieceRotation, cx, cy) call in onDraw() so collision checks use actual positions.
internal fun rotatedBlockCenters(
    shape: List<List<Int>>,
    pieceX: Float,
    pieceY: Float,
    rotation: Float,
    pieceSize: Float = 100f
): List<Pair<Float, Float>> {
    val rows = shape.size
    val cols = if (rows > 0) shape[0].size else 0
    val cx = pieceX + cols * pieceSize / 2f
    val cy = pieceY + rows * pieceSize / 2f
    val angle = rotation * Math.PI / 180.0
    val cosA = cos(angle)
    val sinA = sin(angle)
    val result = mutableListOf<Pair<Float, Float>>()
    for (row in shape.indices) {
        for (col in shape[row].indices) {
            if (shape[row][col] != 1) continue
            val dx = pieceX + (col + 0.5f) * pieceSize - cx
            val dy = pieceY + (row + 0.5f) * pieceSize - cy
            result.add(
                (cx + dx * cosA - dy * sinA).toFloat() to
                (cy + dx * sinA + dy * cosA).toFloat()
            )
        }
    }
    return result
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
