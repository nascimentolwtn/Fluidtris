# Plan: Maze Route-Assist (Multiple Paths + Show-Route + Recalculate)

## Problem

Maze mode (backlog item 10) currently generates a *perfect* maze: `MazeGenerator.generate()`
is a DFS backtracker producing a spanning tree — exactly one path between any two cells.
Solving feels rigid: there is never a choice that matters, and a wrong turn is always a
full dead end. We want (1) mazes with multiple valid routes to the exit, (2) a "show route"
control that displays the shortest path from the player's current cell to the exit, and
(3) live recalculation of that route when the player's moves leave the displayed path
(which only becomes possible once (1) exists).

Constraints: `GameEngine.kt` / `GameMath.kt` / `MazeGenerator.kt` stay pure Kotlin (zero
Android imports); `FluidTetrisView.kt` stays rendering/input only; every magic number goes
in `GameConstants.kt`; randomness flows through the injectable `mazeRandom` seam and time
through `currentTimeMs()`.

---

# Part 1 — Multiple paths to the exit (braiding)

## Option 1A — Dead-end braiding post-pass (recommended)

**What changes:** After the DFS carve loop in `MazeGenerator.generate()`, run a braid pass:
scan the grid for dead ends (cells with exactly 3 standing walls — boundary edges count as
standing because `MazeCell` initializes all walls `true` and boundary walls are never
carved). For a fraction `braidPercent` of them (chosen via the same `random` instance, so
seed determinism is preserved), knock down one additional standing wall toward an in-bounds
neighbor, reusing the existing `carveWall()` so both cells' flags stay mutually consistent.
In a spanning tree, every such removal creates exactly one loop, so each braided dead end
adds one alternative route. Signature becomes
`generate(columns, rows, random, braidPercent: Float = 0f)` — default `0f` keeps the
existing perfect-maze contract (and `MazeGeneratorTest`) intact; `GameEngine.beginMaze()`
passes the new constant `GameConstants.MAZE_BRAID_PERCENT` (suggest `0.35f`, tune by feel).
Iterate dead ends in fixed row/col order and pick the wall via `random` so the result is
fully determined by the seed.

**Feel:** Wrong turns often loop back around instead of dead-ending; the maze stays
visually dense and corridor-like because only dead-end stubs open up — no big rooms.

**Risk:** Low. Braiding is a well-understood standard technique. Only tuning risk: too
high a `braidPercent` makes the maze feel open/trivial. `0.35f` on 8x20 leaves plenty of
dead ends while guaranteeing several loops. One test-compat trap: any test that
regenerates the engine's maze from the same seed (see Part 4) must pass the same
`braidPercent` the engine uses.

## Option 1B — Random interior wall removal

**What changes:** After DFS, remove N random interior walls anywhere in the grid
(`N = braidCount` constant).

**Feel:** Also produces loops, but removals can cluster and merge adjacent corridors into
2-cell-wide open areas — reads as "broken maze" rather than "maze with choices".

**Risk:** Degenerate/open-looking output at exactly the density needed to make multiple
routes likely. Harder to tune than 1A.

## Option 1C — Carve-time loops (probability of carving into a visited neighbor)

**What changes:** During DFS, when a cell has no unvisited neighbors, with probability p
carve into a random *visited* neighbor before backtracking.

**Feel:** Similar to 1A in output, but loops bias toward the backtrack frontier.

**Risk:** Entangles loop creation with the carve loop, complicating the invariant that
`visitOrder` contains each cell exactly once (`generate_visitsEveryCellExactlyOnce`),
and makes the loop density harder to reason about. No payoff over 1A.

### Interaction with the progressive reveal (applies to whichever option wins)

No change needed to the reveal pipeline. `visitOrder` is a list of *cells*, not walls;
braiding removes walls but adds no cells, so `mazeRevealedCells()` /
`mazeRevealFraction()` / the wall-drawing loop in `FluidTetrisView.drawMaze()` work
untouched — a braided opening simply means that wall is never drawn once its cell is
revealed. `MazeResult` keeps its shape. (If we ever want a distinct "wall crumbles"
animation for braided openings, `MazeResult` could gain a `removedWalls` list — out of
scope here.)

---

# Part 2 — "Show route" button (shortest path)

## Pathfinding: where the BFS lives

### Option 2A — New pure object `MazeSolver` (recommended)

**What changes:** New file `MazeSolver.kt` (pure Kotlin, `internal object`, sibling of
`MazeGenerator`) with
`fun shortestPath(grid: Array<Array<MazeCell>>, startRow, startCol, targetRow, targetCol): List<Pair<Int, Int>>`
— plain BFS with a predecessor map, returning the cell sequence inclusive of start and
target (empty list only if unreachable, which cannot happen in a connected maze; braiding
preserves connectivity since it only removes walls). Grid is 8x20 = 160 cells; BFS is
overkill-proof, no A* needed. Note `GameEngineMazeTest.solutionPath()` already implements
this exact BFS in test code — the test copy deliberately stays as an independent oracle.

**Feel:** n/a (internal structure). Directly unit-testable without touching GameEngine.

**Risk:** None meaningful.

### Option 2B — Private method inside GameEngine

**What changes:** Same BFS as a private `GameEngine` function.

**Feel:** n/a.

**Risk:** Not independently testable; GameEngine is already ~950 lines. The repo's pattern
(`MazeGenerator`, `GameMath`) is to keep pure algorithms out of the engine.

## Route state: GameEngine owns it (no options — architecture dictates this)

New maze-mode state in `GameEngine` next to the existing maze fields:

- `var isMazeRouteVisible = false; private set`
- `private var mazeRoute: List<Pair<Int, Int>> = emptyList()`
- `fun mazeRouteCells(): List<Pair<Int, Int>>` — read accessor for the view (mirrors
  `mazeRevealedCells()`).
- `fun toggleMazeRoute()` — gated exactly like `attemptMazeMove`:
  `if (!isMazeActive || !isMazeRevealComplete()) return`. On show, computes
  `MazeSolver.shortestPath(maze!!, mazePlayerRow, mazePlayerCol, mazeExitRow, mazeExitCol)`;
  on hide, clears the list.
- Reset `isMazeRouteVisible = false` / `mazeRoute = emptyList()` in `beginMaze()`,
  `completeMaze()`, and `resetGame()` (alongside the existing `maze = null` cleanup).

## Button placement

### Option 2C — Reuse the side "Next" strips during maze mode (recommended)

**What changes:** The elongated left/right side buttons (x `0..GRID_LEFT` and
`(width - GRID_RIGHT_MARGIN)..width`, y `400f..height - 170f`) are currently drawn and
hit-tested only when `!engine.isMazeActive`. Add the maze-mode counterpart: when
`engine.isMazeActive && engine.isMazeRevealComplete()`, draw the same strips with a
different tint and rotated label "route" (or "hide" when `isMazeRouteVisible`), and in
`onTouchEvent` ACTION_DOWN add a hit-test on the same bounds that calls
`engine.toggleMazeRoute()` — placed *before* the maze-swipe fallback block (the fallback
currently swallows every remaining touch-down while `isMazeActive`). While extracting,
promote the shared layout numbers (`400f`, `170f`) to `GameConstants.SIDE_BUTTON_TOP` /
`SIDE_BUTTON_BOTTOM_MARGIN` per the magic-number rule, since two features now share them.

**Feel:** Consistent with established side-button convention (napkin "Done" entry
2026-06-18); big touch targets on both thumbs; zero overlap with the play area, the HUD
(score box 10..400 x 5..155, toggles at y 170..380), or the bottom New Game/Pause/Exit row.

**Risk:** Low. Only ordering bug to watch: the route hit-test must precede the swipe
fallback, and the swipe fallback must keep working when the touch misses the strips.

### Option 2D — Button in the (idle) next-piece preview corner

**What changes:** During maze mode the preview box (top-right, 160f) isn't drawn; place a
"Route" button there.

**Feel:** Small target, top corner is far from thumbs in portrait; visually orphaned.

**Risk:** Collides conceptually with preview rendering if maze mode ever shows the
upcoming piece; smaller touch target.

## Route rendering (in `drawMaze`, no options — one obvious slot)

In `FluidTetrisView.drawMaze()`, after the exit tile and wall pass, before the player
marker: if `engine.isMazeRouteVisible`, draw a translucent dot
(`canvas.drawCircle`) at each cell center of `engine.mazeRouteCells()`, radius
`minOf(cellWidth, cellHeight) * GameConstants.MAZE_ROUTE_DOT_RADIUS_FRACTION` (suggest
`0.14f` — clearly smaller than the 0.32f player marker). Dots skip the player's own cell
(index 0) so the marker stays readable, and the exit cell already has its green tile.
Dots-not-lines avoids fighting the 6f-wide wall strokes visually. ARGB colors stay inline
in the view like every other color there; only the radius fraction is a constant.

---

# Part 3 — Live recalculation on deviation

## Option 3A — Recompute on every successful move while route is visible (recommended)

**What changes:** At the end of `attemptMazeMove()`, in the success path after
`mazePlayerRow/Col` are updated: if the exit was reached, `completeMaze()` already runs
(and now also clears route state); otherwise, if `isMazeRouteVisible`, recompute
`mazeRoute = MazeSolver.shortestPath(maze!!, mazePlayerRow, mazePlayerCol, mazeExitRow, mazeExitCol)`.
No deviation detection at all.

**Feel:** Exactly the backlog behavior falls out for free. Moving *along* the route makes
the recomputed route equal the old route minus its head — visually the trail shrinks
behind the player. Moving *off* the route redraws the new shortest path from the new cell
in the same frame. Also handles the subtle case where a deviation move makes a
*different* route become shortest even though the player is technically still adjacent to
the old one.

**Risk:** Essentially none. BFS over 160 cells runs per user-paced move (not per frame —
`update()` early-returns on `isMazeActive`, so there's no per-frame path). No new state
beyond Part 2's two fields.

## Option 3B — Recompute only on deviation (route-membership check)

**What changes:** Keep a `HashSet` of route cells; on each move, if the new cell is on the
route, trim the list head; else recompute.

**Feel:** Identical to 3A on screen.

**Risk:** More state (set + list must stay in sync), an edge case around re-entering the
route mid-way (trim must find the *matching index*, not just membership — stepping onto a
later route cell via a loop shortcut must trim multiple entries), and it saves microseconds
that don't matter. Complexity with no observable payoff.

---

## Decision

- **Part 1: Option 1A** (dead-end braiding, `braidPercent` param defaulting to `0f`,
  engine passes `GameConstants.MAZE_BRAID_PERCENT = 0.35f`). Standard, tunable, keeps
  `visitOrder` and the reveal animation untouched.
- **Part 2: Option 2A** (new `MazeSolver.kt`) + **Option 2C** (maze-mode side strips
  toggle `engine.toggleMazeRoute()`), dot-overlay rendering in `drawMaze`.
- **Part 3: Option 3A** (unconditional recompute per successful move while visible).

Implementation order: 1A first (pure, self-contained, testable), then `MazeSolver` + engine
state (pure, testable), then the view wiring (button + overlay) last.

### New constants (GameConstants.kt)

- `MAZE_BRAID_PERCENT = 0.35f` — fraction of dead ends opened into loops (0 = perfect maze)
- `MAZE_ROUTE_DOT_RADIUS_FRACTION = 0.14f` — route dot radius as fraction of min cell dimension
- `SIDE_BUTTON_TOP = 400f`, `SIDE_BUTTON_BOTTOM_MARGIN = 170f` — extracted from the
  existing hardcoded side-button layout in FluidTetrisView, now shared by "next" and "route"

### New unit tests (no device; `./gradlew testDebugUnitTest`)

- `MazeGeneratorTest` additions: with `braidPercent > 0` — walls still mutually consistent;
  all cells still reachable; open-edge count exceeds `cells - 1` (spanning tree edge count),
  proving loops exist; dead-end count strictly decreases vs. `braidPercent = 0` on the same
  seed; same seed + same braidPercent is deterministic; `braidPercent = 0f` still yields
  exactly `cells - 1` open edges (perfect-maze regression).
- New `MazeSolverTest`: path validity (consecutive cells adjacent, no standing wall
  between them — reuse the wall-flag checks from `GameEngineMazeTest.solutionPath`);
  endpoints correct; start == target returns single-cell path; on a hand-carved 3x3 with
  one loop, returns the strictly shorter branch.
- `GameEngineMazeTest` additions: `toggleMazeRoute()` is a no-op before
  `isMazeRevealComplete()`; shown route starts at `(mazePlayerRow, mazePlayerCol)` and ends
  at `(mazeExitRow, mazeExitCol)`; moving along the route shrinks it by one from the head;
  a deviating move (pick any legal move != route[1], findable by seed hunting with a fixed
  `mazeRandom = Random(seed)`) triggers recompute so the new route again starts at the
  player; reaching the exit clears `isMazeRouteVisible` and `mazeRouteCells()`;
  `resetGame()` clears both.
- **Required update to an existing test:** `solvingTheMaze_resumesFluidtris` regenerates
  the engine's maze via `MazeGenerator.generate(GRID_COLUMNS, GRID_ROWS, Random(seed))` —
  once `beginMaze()` passes `MAZE_BRAID_PERCENT`, the test must pass the same value or the
  regenerated maze won't match the engine's. Its BFS oracle itself works fine on braided
  mazes.

## Files to change

- `MazeGenerator.kt` — braid pass in `generate()` (new `braidPercent` param, dead-end scan, reuse `carveWall`)
- `MazeSolver.kt` — **new file**, pure BFS `shortestPath` over `Array<Array<MazeCell>>`
- `GameEngine.kt` — `beginMaze()` (pass braid constant, reset route state), new `isMazeRouteVisible` / `mazeRoute` / `mazeRouteCells()` / `toggleMazeRoute()`, recompute hook at end of `attemptMazeMove()`, route cleanup in `completeMaze()` and `resetGame()`
- `GameConstants.kt` — `MAZE_BRAID_PERCENT`, `MAZE_ROUTE_DOT_RADIUS_FRACTION`, `SIDE_BUTTON_TOP`, `SIDE_BUTTON_BOTTOM_MARGIN`
- `FluidTetrisView.kt` — maze-mode side "route" buttons in `onDraw` + hit-test in `onTouchEvent` (before the maze-swipe fallback), route dot overlay in `drawMaze()`
- Tests — `MazeGeneratorTest.kt` (braid cases), `MazeSolverTest.kt` (new), `GameEngineMazeTest.kt` (route toggle/recalc cases + braidPercent fix in `solvingTheMaze_resumesFluidtris`)

---

### Critical Files for Implementation

- `app/src/main/java/com/libuy/fluidtris/MazeGenerator.kt`
- `app/src/main/java/com/libuy/fluidtris/GameEngine.kt`
- `app/src/main/java/com/libuy/fluidtris/FluidTetrisView.kt`
- `app/src/main/java/com/libuy/fluidtris/GameConstants.kt`
- `app/src/test/java/com/libuy/fluidtris/GameEngineMazeTest.kt`
