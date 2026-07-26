package com.libuy.fluidtris

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MazeGeneratorTest {

    @Test
    fun generate_producesGridOfRequestedDimensions() {
        val result = MazeGenerator.generate(columns = 8, rows = 20, random = Random(1))
        assertEquals(20, result.grid.size)
        assertEquals(8, result.grid[0].size)
    }

    @Test
    fun generate_visitsEveryCellExactlyOnce() {
        val columns = 8
        val rows = 20
        val result = MazeGenerator.generate(columns, rows, random = Random(42))
        assertEquals(columns * rows, result.visitOrder.size)
        assertEquals(columns * rows, result.visitOrder.toSet().size)
        assertEquals(0 to 0, result.visitOrder.first())
    }

    @Test
    fun generate_isAPerfectMaze_everyCellReachableFromStart() {
        val columns = 8
        val rows = 20
        val result = MazeGenerator.generate(columns, rows, random = Random(7))
        val grid = result.grid

        val visited = Array(rows) { BooleanArray(columns) }
        val queue = ArrayDeque<Pair<Int, Int>>()
        queue.add(0 to 0)
        visited[0][0] = true
        var reachable = 0

        while (queue.isNotEmpty()) {
            val (row, col) = queue.removeFirst()
            reachable++
            val cell = grid[row][col]
            val neighbors = mutableListOf<Pair<Int, Int>>()
            if (!cell.north) neighbors.add(row - 1 to col)
            if (!cell.south) neighbors.add(row + 1 to col)
            if (!cell.east) neighbors.add(row to col + 1)
            if (!cell.west) neighbors.add(row to col - 1)
            for ((r, c) in neighbors) {
                if (r in 0 until rows && c in 0 until columns && !visited[r][c]) {
                    visited[r][c] = true
                    queue.add(r to c)
                }
            }
        }

        assertEquals("Every cell must be reachable from the start cell", columns * rows, reachable)
    }

    @Test
    fun generate_wallsAreMutuallyConsistentBetweenNeighbors() {
        val columns = 6
        val rows = 6
        val grid = MazeGenerator.generate(columns, rows, random = Random(3)).grid

        for (row in 0 until rows) {
            for (col in 0 until columns) {
                val cell = grid[row][col]
                if (col + 1 < columns) {
                    assertEquals(cell.east, grid[row][col + 1].west)
                }
                if (row + 1 < rows) {
                    assertEquals(cell.south, grid[row + 1][col].north)
                }
            }
        }
    }

    @Test
    fun generate_differentSeedsProduceDifferentMazes() {
        val a = MazeGenerator.generate(8, 20, random = Random(1))
        val b = MazeGenerator.generate(8, 20, random = Random(2))
        assertTrue("Different seeds should (almost certainly) produce different carve orders",
            a.visitOrder != b.visitOrder)
    }
}
