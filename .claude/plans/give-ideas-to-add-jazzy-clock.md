# Gamification Specials: Bombs, Diamonds & Perks

## Context
The base game (score, levels, multi-piece physics) is solid. The next growth layer is player excitement through specials: bombs that shatter the grid, a diamond economy that rewards skilled play, and a perk framework that can eventually generate revenue. This plan covers the full design — architectural hooks, UI placement, and future IAP scaffolding.

---

## Feature 1: Bomb Pieces

### Design
A bomb is a falling piece that, on lock, detonates: it clears all solid grid cells within a configurable radius and awards bonus score per cell destroyed.

**Engine changes (`GameEngine.kt`)**
- Add `specialType: SpecialType = SpecialType.NORMAL` to `ActivePiece` data class (line ~7). Enum: `NORMAL`, `BOMB`.
- In `spawnNextPiece()` (line ~430): after choosing `nextPiece` via `Random.nextInt(...)`, roll a second `Random.nextFloat()` against `BOMB_SPAWN_CHANCE` (suggest 8%). If hit, set `specialType = BOMB` on the spawned piece. Apply a distinct color from `GameConstants` (e.g. deep orange `0xFFFF6B00`).
- Add `explodeBomb(piece: ActivePiece)` method: iterate all block centers of the locked piece via `rotatedBlockCenters()`; for each center convert to grid coords; null out every `grid[r][c]` where Manhattan distance ≤ `BOMB_EXPLOSION_RADIUS` (suggest 2 cells); count destroyed cells; `score += cells * BOMB_SCORE_PER_CELL` (suggest 50 pts each).
- In `turnPieceRigidInternal()` (line ~372): after writing cells into `grid[][]`, check `if (piece.specialType == BOMB) explodeBomb(piece)`. Then call existing `checkLines()` so cleared rows still score.
- Add `val onBombExploded: () -> Unit` callback parameter (same pattern as existing `onPieceLocked`).

**Constants to add (`GameConstants.kt`)**
```
BOMB_SPAWN_CHANCE = 0.08f
BOMB_EXPLOSION_RADIUS = 2          // Manhattan distance in cells
BOMB_SCORE_PER_CELL = 50
BOMB_COLOR = 0xFFFF6B00.toInt()    // deep orange
```

**Rendering (`FluidTetrisView.kt`)**
- In the falling-pieces draw loop, after `drawJellyPiece(...)`, check if `piece.specialType == BOMB`. If so, draw a 💣 emoji (or a glowing red circle + "B" text) at the piece's center pixel: `canvas.drawText("💣", centerX, centerY, bombPaint)`, textSize ~60f. This keeps `drawJellyPiece` unchanged.
- On `onBombExploded` callback: trigger a brief shake animation (offset `canvas.translate` by ±4px for 3 frames via a countdown field).

---

## Feature 2: Special Bar + Diamonds

### Engine design (`GameEngine.kt`)
New fields:
```kotlin
var specialBarProgress: Float = 0f   // 0..100
var diamonds: Int = 0
```

Bar-fill events (increment in existing methods):
- `turnPieceRigidInternal()` → `+10f` per piece locked
- `onLineCleared` path inside `checkLines()` → `+25f` per line cleared
- `onNextPieceButton()` (player taps side "next" button) → `+5f`

When `specialBarProgress >= 100f`:
```kotlin
specialBarProgress -= 100f
diamonds++
onDiamondEarned()   // new callback, plays a chime
```

Diamond spend — add `fun spendDiamond(perk: DiamondPerk): Boolean`:
```kotlin
enum class DiamondPerk(val cost: Int) {
    REROLL_NEXT(1),
    CLEAR_BOTTOM_ROW(2),
}
```
- `REROLL_NEXT`: pick a new `nextPiece` via `Random.nextInt(...)`, resetting preview. Cost: 1 diamond.
- `CLEAR_BOTTOM_ROW`: null out the bottom-most non-empty row in `grid[][]`. Cost: 2 diamonds.

**Persistence (`HighScoreManager.kt`)**
Add `saveDiamonds(n)` / `loadDiamonds()` via a `"diamonds"` SharedPreferences key, same pattern as `highScore`. Load in `FluidTetrisView.init` alongside `highScore`.

### UI placement (`FluidTetrisView.kt`)
The score HUD panel occupies `x:10–400, y:5–155`. The top-right preview occupies `x:width-180, y:5–185`. The horizontal strip `x:410–(width-190), y:5–60` is free on all screen sizes.

**Special bar** — draw a horizontal meter in that strip:
- Background: `drawRect(410f, 10f, width-190f, 50f)` with `Color.argb(180, 80, 60, 120)` (dark purple)
- Fill: `drawRect(410f, 10f, 410f + (width-600f) * (specialBarProgress/100f), 50f)` with `Color.argb(255, 180, 100, 255)` (purple)
- Label: `"⬦ ${engine.diamonds}"` to the right of the bar, textSize 36f

**"Reroll" button** — small rect directly below the next-piece preview box:
- `drawRect(previewX, 195f, previewX+160f, 255f)` with `Color.argb(200, 100, 80, 160)` (purple tint)
- Text: `"↻ 1⬦"`, textSize 28f; grayed out if `diamonds < 1`
- Touch: `event.x in previewX..(previewX+160f) && event.y in 195f..255f` → `engine.spendDiamond(REROLL_NEXT)`

**"Clear Row" button** — below the reroll button, or accessible from pause overlay:
- `drawRect(previewX, 265f, previewX+160f, 325f)` same style, text `"⌦ 2⬦"`

---

## Feature 3: Future Gamification Framework

These are design decisions for later — no code needed now, but worth anchoring architecturally.

### 3a. Streak Multiplier
Add `streakMultiplier: Int = 1` and `lastLineClearTimeMs: Long` to `GameEngine`. In `checkLines()`: if `currentTimeMs() - lastLineClearTimeMs <= STREAK_WINDOW_MS (5000L)`, increment `streakMultiplier` (cap at 4); else reset to 1. Apply: `score += baseLineScore * streakMultiplier`. Show "×2" text near score HUD when active. Zero touch required to existing flow.

### 3b. Daily Challenge Mode
Seed `Random` from `LocalDate.now().toEpochDay()` for the piece sequence. All players get identical pieces that day. Show a daily-mode banner in the score HUD. Separate leaderboard (score only, no persistence needed locally — future server).

### 3c. Achievement Toasts
Add `AchievementManager` (pure Kotlin, no Android imports). Events dispatched from `GameEngine` callbacks → check milestone conditions → fire `onAchievementUnlocked(title, icon)` callback → `FluidTetrisView` draws a 2-second toast overlay (slide-in rect with title text). Sample milestones: first bomb detonation, 5 diamonds earned, level 10 reached, 4-line Tetris clear.

### 3d. IAP Perk Framework (stub now, fill later)
Create `PerkManager.kt` with:
```kotlin
object PerkManager {
    fun isPerkUnlocked(perk: Perk): Boolean = false  // always false until IAP live
    enum class Perk { SLOW_MODE, BOMB_PACK, EXTRA_DIAMONDS, UNDO_LOCK }
}
```
In the pause overlay, add a grayed-out "Perks [soon]" button. When `PerkManager` goes live, swap the stub. Google Play Billing Library 6.x integrates here. This keeps `GameEngine` clean — no IAP logic bleeds in.

---

## Critical Files

| File | Change |
|---|---|
| `GameConstants.kt` | Bomb constants, `STREAK_WINDOW_MS` |
| `GameEngine.kt` | `ActivePiece.specialType`, `specialBarProgress`, `diamonds`, `explodeBomb()`, `spendDiamond()`, bar-fill increments |
| `FluidTetrisView.kt` | Draw bar + diamond counter, reroll/clear-row buttons, bomb emoji overlay, shake animation |
| `HighScoreManager.kt` | `saveDiamonds` / `loadDiamonds` |
| `PerkManager.kt` (new) | Stub only |

---

## Suggested Implementation Order
1. **Bomb pieces** — self-contained, high-impact, no UI debt
2. **Special bar + diamond counter** — builds on bomb's `onBombExploded` callback for extra fill
3. **Reroll button** — first diamond spend, validates the economy loop
4. **Clear-bottom-row button** — second spend option
5. **Streak multiplier** — pure engine, no UI beyond a text label
6. **Achievement toasts** — polish layer
7. **IAP stub** — structural only, no real integration yet

---

## Verification

```bash
# Unit tests (no device needed)
./gradlew testDebugUnitTest

# Install on all connected devices
./gradlew installDebug
```

**Unit tests to write:**
- `BombPieceTest`: `bombLocks_clearsRadius2Cells`, `bombScoresPerClearedCell`, `bombThenCheckLines_stacksScore`, `normalPiece_noBombEffect`
- `DiamondEconomyTest`: `barFillsOnPieceLock`, `barFillsOnLineClear`, `diamondAwardedAt100`, `spendDiamond_rerollsNext`, `spendDiamond_insufficientFunds_noOp`
- `StreakTest`: `consecutiveClearsWithinWindow_multipliesScore`, `clearAfterWindow_resetsMultiplier`

**Visual verification (user-side):**
- Bomb piece renders in orange with 💣 glyph
- Grid visibly explodes in a radius on lock
- Bar fills and diamonds increment during normal play
- Reroll button swaps the next-piece preview
- No regression in normal piece spawn, lock, or line-clear behavior