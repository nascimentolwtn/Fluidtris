package com.libuy.fluidtris

internal object GameConstants {
    const val GRID_COLUMNS = 7
    const val GRID_ROWS = 20
    const val PIECE_SIZE = 100f
    const val GRAVITY = 2.0f
    const val UPWARD_DRAG_FACTOR = 0.4f
    const val ROTATION_SENSITIVITY = 30f
    const val LOCK_DELAY_MS = 3000L
    const val SPRING_CARRY = 0.5f   // fraction of horizontal drag velocity carried on release
    const val SPRING_DAMPING = 0.80f // per-frame velocity retention after release
    const val SLIDE_IMPULSE = 3.0f
    const val BOUNCE_ROTATION_DEG = 3f  // degrees of tilt added per bounce event
    const val BLOCK_INSET = 1.5f        // per-side visual and collision shrink (3px total)
    const val SNAP_PULL_SPEED = 0.72f       // X drift: per-frame lerp strength at t=1 (linear ramp)
    const val SNAP_ROTATION_SPEED = 0.96f  // rotation: per-frame lerp strength at t=1 (linear ramp)
    const val LEVEL_DIFFICULTY_FACTOR = 1f // multiplier increase per level
    const val MAX_LEVEL_MULTIPLIER = 3f      // cap on level multiplier
    const val NEXT_LEVEL_SCORE = 300 // score points needed per level (default: 500)
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
        0xFF00FFFF.toInt(), // I – cyan
        0xFFFFFF00.toInt(), // O – yellow
        0xFFFF00FF.toInt(), // T – magenta
        0xBBBBBBBB.toInt(), // L – light-gray
        0xFF0000FF.toInt(), // J – blue
        0xFF00FF00.toInt(), // S – green
        0xFFFF0000.toInt()  // Z – red
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
