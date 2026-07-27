package com.libuy.fluidtris

// Pure BFS shortest-path search over a maze's wall grid. The grid is small (rows x columns
// cells, e.g. 8x20 = 160), so BFS is sufficient — no need for A*.
internal object MazeSolver {

    // Returns the cell sequence from start to target inclusive, or an empty list if the
    // target is unreachable (cannot happen in a connected maze; braiding only removes walls).
    fun shortestPath(
        grid: Array<Array<MazeCell>>,
        startRow: Int, startCol: Int,
        targetRow: Int, targetCol: Int
    ): List<Pair<Int, Int>> {
        val rows = grid.size
        val columns = if (rows > 0) grid[0].size else 0
        val start = startRow to startCol
        val target = targetRow to targetCol
        if (start == target) return listOf(start)

        val visited = Array(rows) { BooleanArray(columns) }
        val prev = HashMap<Pair<Int, Int>, Pair<Int, Int>>()
        val queue = ArrayDeque<Pair<Int, Int>>()
        queue.addLast(start)
        visited[startRow][startCol] = true

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current == target) break
            val (row, col) = current
            val cell = grid[row][col]
            val neighbors = listOfNotNull(
                if (!cell.north) row - 1 to col else null,
                if (!cell.south) row + 1 to col else null,
                if (!cell.east) row to col + 1 else null,
                if (!cell.west) row to col - 1 else null
            )
            for ((r, c) in neighbors) {
                if (r in 0 until rows && c in 0 until columns && !visited[r][c]) {
                    visited[r][c] = true
                    prev[r to c] = current
                    queue.addLast(r to c)
                }
            }
        }

        if (!visited[targetRow][targetCol]) return emptyList()

        val path = mutableListOf<Pair<Int, Int>>()
        var node = target
        while (node != start) {
            path.add(node)
            node = prev[node] ?: return emptyList()
        }
        path.add(start)
        path.reverse()
        return path
    }
}
