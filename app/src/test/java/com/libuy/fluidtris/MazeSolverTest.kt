package com.libuy.fluidtris

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MazeSolverTest {

    private fun assertValidPath(grid: Array<Array<MazeCell>>, path: List<Pair<Int, Int>>) {
        for (i in 0 until path.size - 1) {
            val (row, col) = path[i]
            val (nextRow, nextCol) = path[i + 1]
            val cell = grid[row][col]
            val stepIsOpen = when {
                nextRow == row - 1 && nextCol == col -> !cell.north
                nextRow == row + 1 && nextCol == col -> !cell.south
                nextRow == row && nextCol == col + 1 -> !cell.east
                nextRow == row && nextCol == col - 1 -> !cell.west
                else -> false // not an orthogonal step to an adjacent cell
            }
            assertTrue("Step from ${path[i]} to ${path[i + 1]} must cross an open wall", stepIsOpen)
        }
    }

    @Test
    fun shortestPath_startEqualsTarget_returnsSingleCell() {
        val grid = MazeGenerator.generate(6, 6, Random(1)).grid
        val path = MazeSolver.shortestPath(grid, 2, 3, 2, 3)
        assertEquals(listOf(2 to 3), path)
    }

    @Test
    fun shortestPath_endpointsAreStartAndTarget() {
        val grid = MazeGenerator.generate(8, 20, Random(4)).grid
        val path = MazeSolver.shortestPath(grid, 0, 0, 19, 7)
        assertTrue(path.isNotEmpty())
        assertEquals(0 to 0, path.first())
        assertEquals(19 to 7, path.last())
    }

    @Test
    fun shortestPath_everyStepCrossesAnOpenWall() {
        val grid = MazeGenerator.generate(8, 20, Random(9), braidPercent = 0.35f).grid
        val path = MazeSolver.shortestPath(grid, 0, 0, 19, 7)
        assertValidPath(grid, path)
    }

    @Test
    fun shortestPath_onPerfectMaze_isUnique_andValid() {
        // A perfect maze (braidPercent = 0) has exactly one path between any two cells.
        val grid = MazeGenerator.generate(8, 20, Random(2)).grid
        val path = MazeSolver.shortestPath(grid, 0, 0, 19, 7)
        assertValidPath(grid, path)
        assertEquals(19 to 7, path.last())
    }

    @Test
    fun shortestPath_picksShorterBranch_onHandCarvedLoop() {
        // 4x4 grid with two disjoint routes from (0,0) to (3,3) sharing only their endpoints:
        // branch A (short, 6 steps): down the left column then across the bottom row.
        // branch B (long, 8 steps): across the top row, down, back left, down, across.
        // No other walls are open, so these are the only two possible routes.
        val grid = Array(4) { Array(4) { MazeCell() } }
        fun open(r1: Int, c1: Int, r2: Int, c2: Int) {
            when {
                r2 == r1 + 1 -> { grid[r1][c1].south = false; grid[r2][c2].north = false }
                else -> { grid[r1][c1].east = false; grid[r2][c2].west = false }
            }
        }
        // Branch A: (0,0)-(1,0)-(2,0)-(3,0)-(3,1)-(3,2)-(3,3) [6 steps]
        open(0, 0, 1, 0); open(1, 0, 2, 0); open(2, 0, 3, 0)
        open(3, 0, 3, 1); open(3, 1, 3, 2); open(3, 2, 3, 3)
        // Branch B: (0,0)-(0,1)-(0,2)-(0,3)-(1,3)-(1,2)-(2,2)-(2,3)-(3,3) [8 steps]
        open(0, 0, 0, 1); open(0, 1, 0, 2); open(0, 2, 0, 3)
        open(0, 3, 1, 3); open(1, 2, 1, 3); open(1, 2, 2, 2)
        open(2, 2, 2, 3); open(2, 3, 3, 3)

        val path = MazeSolver.shortestPath(grid, 0, 0, 3, 3)
        assertValidPath(grid, path)
        assertEquals(
            listOf(0 to 0, 1 to 0, 2 to 0, 3 to 0, 3 to 1, 3 to 2, 3 to 3),
            path
        )
    }
}
