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

    // ── Braiding (multiple paths) ───────────────────────────────────────────

    private fun openEdgeCount(grid: Array<Array<MazeCell>>, rows: Int, columns: Int): Int {
        var count = 0
        for (row in 0 until rows) {
            for (col in 0 until columns) {
                val cell = grid[row][col]
                if (!cell.south && row + 1 < rows) count++
                if (!cell.east && col + 1 < columns) count++
            }
        }
        return count
    }

    private fun deadEndCount(grid: Array<Array<MazeCell>>, rows: Int, columns: Int): Int {
        var count = 0
        for (row in 0 until rows) {
            for (col in 0 until columns) {
                val cell = grid[row][col]
                val standingWalls = listOf(cell.north, cell.south, cell.east, cell.west).count { it }
                if (standingWalls == 3) count++
            }
        }
        return count
    }

    @Test
    fun generate_braidPercentZero_isStillAPerfectMaze_spanningTreeEdgeCount() {
        val columns = 8
        val rows = 20
        val grid = MazeGenerator.generate(columns, rows, random = Random(11), braidPercent = 0f).grid
        // A spanning tree over N cells has exactly N-1 edges.
        assertEquals(columns * rows - 1, openEdgeCount(grid, rows, columns))
    }

    @Test
    fun generate_withBraiding_addsLoops_exceedingSpanningTreeEdgeCount() {
        val columns = 8
        val rows = 20
        val grid = MazeGenerator.generate(columns, rows, random = Random(11), braidPercent = 0.35f).grid
        assertTrue("Braiding must add at least one loop edge beyond the spanning tree",
            openEdgeCount(grid, rows, columns) > columns * rows - 1)
    }

    @Test
    fun generate_withBraiding_reducesDeadEndCount() {
        val columns = 8
        val rows = 20
        val perfect = MazeGenerator.generate(columns, rows, random = Random(11), braidPercent = 0f).grid
        val braided = MazeGenerator.generate(columns, rows, random = Random(11), braidPercent = 0.35f).grid
        assertTrue("Braiding must open some dead ends into loops",
            deadEndCount(braided, rows, columns) < deadEndCount(perfect, rows, columns))
    }

    @Test
    fun generate_withBraiding_allCellsStillReachable() {
        val columns = 8
        val rows = 20
        val grid = MazeGenerator.generate(columns, rows, random = Random(11), braidPercent = 0.35f).grid

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

        assertEquals("Braiding must not disconnect any cell", columns * rows, reachable)
    }

    @Test
    fun generate_withBraiding_wallsStayMutuallyConsistent() {
        val columns = 6
        val rows = 6
        val grid = MazeGenerator.generate(columns, rows, random = Random(3), braidPercent = 0.5f).grid

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
    fun generate_withBraiding_sameSeedAndPercent_isDeterministic() {
        val columns = 8
        val rows = 20
        val a = MazeGenerator.generate(columns, rows, random = Random(55), braidPercent = 0.35f)
        val b = MazeGenerator.generate(columns, rows, random = Random(55), braidPercent = 0.35f)

        assertEquals(a.visitOrder, b.visitOrder)
        for (row in 0 until rows) {
            for (col in 0 until columns) {
                assertEquals(a.grid[row][col], b.grid[row][col])
            }
        }
    }
}
