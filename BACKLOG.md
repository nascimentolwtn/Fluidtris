# Backlog

Informal notes on ideas not yet scheduled for implementation.

## Maze feature (see `MazeGenerator.kt`, `GameEngine.kt` maze mode)

- Maze can have multiple valid paths to the exit, not just one — makes the reveal/solve feel less rigid.
- "Show route" button: reveals the shortest path from the player's current cell to the exit.
- While the route is shown, if the player deviates onto a different path, recalculate and redisplay the route from the new position.
