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

// Generates a perfect maze (exactly one path between any two cells) sized columns x rows,
// via iterative randomized depth-first backtracking starting at (0, 0).
internal object MazeGenerator {

    fun generate(columns: Int, rows: Int, random: Random = Random.Default): MazeResult {
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

        return MazeResult(grid, visitOrder)
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
