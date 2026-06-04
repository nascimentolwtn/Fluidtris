package com.libuy.fluidtris

import android.graphics.Color

internal object GameConstants {
    const val GRID_COLUMNS = 7
    const val GRID_ROWS = 20
    const val PIECE_SIZE = 100f
    const val GRAVITY = 2.0f
    const val UPWARD_DRAG_FACTOR = 0.4f
    const val ROTATION_SENSITIVITY = 30f
    const val LOCK_DELAY_MS = 3000L
    const val GAME_LOOP_INTERVAL_MS = 16L
    const val GRID_LEFT = 150f
    const val GRID_TOP = 100f
    const val GRID_RIGHT_MARGIN = 150f
    const val GRID_BOTTOM_MARGIN = 180f

    val PIECES: List<List<List<Int>>> = listOf(
        listOf(listOf(1, 1, 1, 1)),              // I
        listOf(listOf(1, 1), listOf(1, 1)),       // O
        listOf(listOf(1, 1, 1), listOf(0, 1, 0)), // T
        listOf(listOf(1, 1, 1), listOf(1, 0, 0)), // L
        listOf(listOf(1, 1, 1), listOf(0, 0, 1)), // J
        listOf(listOf(1, 1, 0), listOf(0, 1, 1)), // S
        listOf(listOf(0, 1, 1), listOf(1, 1, 0))  // Z
    )

    val PIECE_COLORS: List<Int> = listOf(
        Color.CYAN,    // I
        Color.YELLOW,  // O
        Color.MAGENTA, // T
        Color.RED,     // L
        Color.BLUE,    // J
        Color.GREEN,   // S
        Color.RED      // Z
    )

    val PIECE_CENTER_CELLS: List<Set<Pair<Int, Int>>> = listOf(
        setOf(0 to 1, 0 to 2),                   // I – both middle blocks
        setOf(0 to 0, 0 to 1, 1 to 0, 1 to 1),  // O – whole piece is movement zone
        setOf(0 to 1),                            // T – middle of top bar
        setOf(0 to 1),                            // L – middle of long bar
        setOf(0 to 1),                            // J – middle of long bar
        setOf(0 to 1),                            // S – top middle block
        setOf(0 to 1),                            // Z – top middle block
    )
}
