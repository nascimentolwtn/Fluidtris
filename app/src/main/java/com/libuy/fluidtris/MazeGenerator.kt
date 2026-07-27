package com.libuy.fluidtris

import kotlin.random.Random

// Wall flags: true means the wall is standing (movement blocked in that direction).
internal data class MazeCell(
    var north: Boolean = true,
    var south: Boolean = true,
    var east: Boolean = true,
    var west: Boolean = true
)

internal data class MazeResult(
    val grid: Array<Array<MazeCell>>,
    // Cells in carve order; used to animate the maze materializing wall-by-wall.
    val visitOrder: List<Pair<Int, Int>>
)

// Generates a maze sized columns x rows via iterative randomized depth-first backtracking
// starting at (0, 0). With braidPercent = 0 (default) the result is a perfect maze (exactly
// one path between any two cells); a positive braidPercent opens a fraction of dead ends
// into loops, so solvers may have more than one valid route to the exit.
internal object MazeGenerator {

    fun generate(columns: Int, rows: Int, random: Random = Random.Default, braidPercent: Float = 0f): MazeResult {
        val grid = Array(rows) { Array(columns) { MazeCell() } }
        val visited = Array(rows) { BooleanArray(columns) }
        val visitOrder = mutableListOf<Pair<Int, Int>>()
        val stack = ArrayDeque<Pair<Int, Int>>()

        val start = 0 to 0
        visited[0][0] = true
        visitOrder.add(start)
        stack.addLast(start)

        while (stack.isNotEmpty()) {
            val (row, col) = stack.last()
            val neighbors = unvisitedNeighbors(row, col, columns, rows, visited)
            if (neighbors.isEmpty()) {
                stack.removeLast()
                continue
            }
            val (nRow, nCol, dir) = neighbors[random.nextInt(neighbors.size)]
            carveWall(grid, row, col, nRow, nCol, dir)
            visited[nRow][nCol] = true
            visitOrder.add(nRow to nCol)
            stack.addLast(nRow to nCol)
        }

        braid(grid, columns, rows, braidPercent, random)

        return MazeResult(grid, visitOrder)
    }

    // Opens a fraction of dead-end cells (exactly one carved opening) into an adjacent
    // standing wall, creating a loop and therefore an alternate route. Dead ends and their
    // fate are decided in fixed row/col order, consuming `random` deterministically.
    private fun braid(grid: Array<Array<MazeCell>>, columns: Int, rows: Int, braidPercent: Float, random: Random) {
        if (braidPercent <= 0f) return
        for (row in 0 until rows) {
            for (col in 0 until columns) {
                val cell = grid[row][col]
                if (standingWallCount(cell) != 3) continue
                if (random.nextFloat() >= braidPercent) continue
                val candidates = standingWallNeighbors(row, col, cell, columns, rows)
                if (candidates.isEmpty()) continue
                val (nRow, nCol, dir) = candidates[random.nextInt(candidates.size)]
                carveWall(grid, row, col, nRow, nCol, dir)
            }
        }
    }

    private fun standingWallCount(cell: MazeCell): Int =
        listOf(cell.north, cell.south, cell.east, cell.west).count { it }

    // Standing walls whose neighbor cell is in bounds (excludes boundary walls, which have
    // no neighbor to carve into).
    private fun standingWallNeighbors(
        row: Int, col: Int, cell: MazeCell, columns: Int, rows: Int
    ): List<Triple<Int, Int, Char>> {
        val candidates = listOf(
            Triple(row - 1, col, 'N') to cell.north,
            Triple(row + 1, col, 'S') to cell.south,
            Triple(row, col + 1, 'E') to cell.east,
            Triple(row, col - 1, 'W') to cell.west
        )
        return candidates.filter { (pos, standing) ->
            standing && pos.first in 0 until rows && pos.second in 0 until columns
        }.map { it.first }
    }

    private fun unvisitedNeighbors(
        row: Int, col: Int, columns: Int, rows: Int, visited: Array<BooleanArray>
    ): List<Triple<Int, Int, Char>> {
        val candidates = listOf(
            Triple(row - 1, col, 'N'),
            Triple(row + 1, col, 'S'),
            Triple(row, col + 1, 'E'),
            Triple(row, col - 1, 'W')
        )
        return candidates.filter { (r, c, _) -> r in 0 until rows && c in 0 until columns && !visited[r][c] }
    }

    private fun carveWall(grid: Array<Array<MazeCell>>, row: Int, col: Int, nRow: Int, nCol: Int, dir: Char) {
        when (dir) {
            'N' -> { grid[row][col].north = false; grid[nRow][nCol].south = false }
            'S' -> { grid[row][col].south = false; grid[nRow][nCol].north = false }
            'E' -> { grid[row][col].east = false; grid[nRow][nCol].west = false }
            else -> { grid[row][col].west = false; grid[nRow][nCol].east = false }
        }
    }
}
