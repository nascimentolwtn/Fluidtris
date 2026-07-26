package com.libuy.fluidtris

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameEngineMazeTest {

    private val VW = 1080
    private val VH = 1920

    private fun solutionPath(grid: Array<Array<MazeCell>>, rows: Int, cols: Int): List<MazeDirection> {
        val start = 0 to 0
        val target = (rows - 1) to (cols - 1)
        val prev = HashMap<Pair<Int, Int>, Pair<Pair<Int, Int>, MazeDirection>>()
        val visited = HashSet<Pair<Int, Int>>()
        val queue = ArrayDeque<Pair<Int, Int>>()
        queue.add(start)
        visited.add(start)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current == target) break
            val (row, col) = current
            val cell = grid[row][col]
            val moves = listOfNotNull(
                if (!cell.north) Triple(row - 1, col, MazeDirection.UP) else null,
                if (!cell.south) Triple(row + 1, col, MazeDirection.DOWN) else null,
                if (!cell.west) Triple(row, col - 1, MazeDirection.LEFT) else null,
                if (!cell.east) Triple(row, col + 1, MazeDirection.RIGHT) else null
            )
            for ((r, c, dir) in moves) {
                val next = r to c
                if (next !in visited) {
                    visited.add(next)
                    prev[next] = current to dir
                    queue.add(next)
                }
            }
        }

        val path = mutableListOf<MazeDirection>()
        var node = target
        while (node != start) {
            val (p, dir) = prev[node] ?: error("no path found from start to target")
            path.add(0, dir)
            node = p
        }
        return path
    }

    // Drives a real line clear so score/level advance through the engine's normal path,
    // matching the pattern used by GameEngineLevelTest.
    private fun clearOneLine(e: GameEngine, advanceTime: () -> Unit) {
        val cellWidth = (VW - GameConstants.GRID_LEFT - GameConstants.GRID_RIGHT_MARGIN) / GameConstants.GRID_COLUMNS
        val cellHeight = (VH - GameConstants.GRID_TOP - GameConstants.GRID_BOTTOM_MARGIN) / GameConstants.GRID_ROWS

        for (col in 0 until GameConstants.GRID_COLUMNS - 1) {
            e.grid[GameConstants.GRID_ROWS - 1][col] = 0xFF0000
        }
        e.currentPiece = 0  // I-piece
        e.pieceY = GameConstants.GRID_TOP + cellHeight * 2
        e.pieceX = GameConstants.GRID_LEFT + cellWidth * (GameConstants.GRID_COLUMNS - 2)
        e.pieceRotation = 90f

        val scoreBefore = e.score
        var iterations = 0
        while (e.score == scoreBefore && iterations < 10_000) {
            advanceTime()
            e.update(VW, VH)
            iterations++
        }
    }

    @Test
    fun crossingMazeMilestoneLevel_activatesMaze_andClearsPlayArea() {
        var fakeTimeMs = 0L
        val e = GameEngine(onPieceLocked = {}, onLineCleared = {})
        e.currentTimeMs = { fakeTimeMs }
        e.resetGame(VW, VH)

        // One line-clear away from level 5 (score 1200), a multiple of the default MAZE_LEVEL_INTERVAL (5)
        e.score = GameConstants.NEXT_LEVEL_SCORE * 4 - 100
        assertEquals(4, e.getLevel())

        clearOneLine(e) { fakeTimeMs += 16L }

        assertEquals(5, e.getLevel())
        assertTrue("Maze should activate on crossing a maze-interval level", e.isMazeActive)
        assertTrue("Grid must be cleared when the maze starts", e.grid.all { row -> row.all { it == null } })
        assertTrue("Falling pieces must be cleared when the maze starts", e.fallingPieces.isEmpty())
    }

    @Test
    fun mazeIntervalDisabled_zero_neverActivatesMaze() {
        // GameConstants.MAZE_LEVEL_INTERVAL is fixed at build time; this test documents the
        // pure-function contract that backs the "0 disables" behavior (see MazeMilestoneTest).
        assertFalse(crossedMazeMilestone(4, 10, 0))
    }

    @Test
    fun mazeMovement_blockedUntilRevealAnimationCompletes() {
        var fakeTimeMs = 0L
        val e = GameEngine(onPieceLocked = {}, onLineCleared = {})
        e.currentTimeMs = { fakeTimeMs }
        e.resetGame(VW, VH)
        e.mazeRandom = Random(11)
        e.score = GameConstants.NEXT_LEVEL_SCORE * 4 - 100
        clearOneLine(e) { fakeTimeMs += 16L }
        assertTrue(e.isMazeActive)

        // Reveal animation just started; movement must not be possible yet.
        assertFalse(e.attemptMazeMove(MazeDirection.DOWN, VW, VH))
        assertFalse(e.attemptMazeMove(MazeDirection.RIGHT, VW, VH))
        assertEquals(0, e.mazePlayerRow)
        assertEquals(0, e.mazePlayerCol)
    }

    @Test
    fun mazeMovement_boundaryWallsAtStartAlwaysBlockUpAndLeft() {
        var fakeTimeMs = 0L
        val e = GameEngine(onPieceLocked = {}, onLineCleared = {})
        e.currentTimeMs = { fakeTimeMs }
        e.resetGame(VW, VH)
        e.mazeRandom = Random(99)
        e.score = GameConstants.NEXT_LEVEL_SCORE * 4 - 100
        clearOneLine(e) { fakeTimeMs += 16L }
        fakeTimeMs += GameConstants.MAZE_REVEAL_DURATION_MS + 100L

        // The start cell (0, 0) is a grid corner: north/west are always out-of-bounds walls,
        // regardless of how the maze was carved.
        assertFalse(e.attemptMazeMove(MazeDirection.UP, VW, VH))
        assertFalse(e.attemptMazeMove(MazeDirection.LEFT, VW, VH))
        assertEquals(0, e.mazePlayerRow)
        assertEquals(0, e.mazePlayerCol)

        // The start cell must have at least one carved exit (down or right).
        val movedDown = e.attemptMazeMove(MazeDirection.DOWN, VW, VH)
        val movedRight = if (!movedDown) e.attemptMazeMove(MazeDirection.RIGHT, VW, VH) else false
        assertTrue("Start cell must have at least one open direction", movedDown || movedRight)
    }

    @Test
    fun solvingTheMaze_resumesFluidtris() {
        var fakeTimeMs = 0L
        var mazeSolvedCount = 0
        val e = GameEngine(
            onPieceLocked = {},
            onLineCleared = {},
            onMazeSolved = { mazeSolvedCount++ }
        )
        e.currentTimeMs = { fakeTimeMs }
        e.resetGame(VW, VH)

        val seed = 2024
        e.mazeRandom = Random(seed)
        e.score = GameConstants.NEXT_LEVEL_SCORE * 4 - 100
        clearOneLine(e) { fakeTimeMs += 16L }
        assertTrue(e.isMazeActive)

        // Independently regenerate the same maze (same seed) to compute the solving path.
        val expectedMaze = MazeGenerator.generate(GameConstants.GRID_COLUMNS, GameConstants.GRID_ROWS, Random(seed))
        val path = solutionPath(expectedMaze.grid, GameConstants.GRID_ROWS, GameConstants.GRID_COLUMNS)
        assertTrue(path.isNotEmpty())

        fakeTimeMs += GameConstants.MAZE_REVEAL_DURATION_MS + 100L

        for (direction in path) {
            val moved = e.attemptMazeMove(direction, VW, VH)
            assertTrue("Move $direction along the known solution path must succeed", moved)
        }

        assertEquals(GameConstants.GRID_ROWS - 1, e.mazePlayerRow)
        assertEquals(GameConstants.GRID_COLUMNS - 1, e.mazePlayerCol)
        assertFalse("Maze should end once the exit is reached", e.isMazeActive)
        assertEquals(1, mazeSolvedCount)
        assertEquals("Fluidtris should resume with a freshly spawned piece", 1, e.fallingPieces.size)
    }
}
