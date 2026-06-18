# Plan: Improve Drag-While-Snap UX

## Problem

When a player drags a snap-animating piece and it hits a solid block, the current
behavior is: bounce spring + release drag. On the next frame `checkPieceCollisions`
re-engages snap. The cycle makes lateral repositioning feel unresponsive.

## Option 1 — Clamp instead of bounce

**What changes:** In `onTouchMove`, when a center drag hits a solid block, stop
the piece at the collision edge but keep `draggedPiece` active (do not set
`draggedPiece = null`). Remove the bounce spring on collision.

**Feel:** Piece sticks to the obstruction; player can slide back or rotate freely.
No surprise release.

**Risk:** Piece can get "stuck" against a wall if the player keeps pushing. Need
to make sure snap doesn't re-engage while the drag is held.

---

## Option 2 — Ghost drag (recommended)

**What changes:** While `draggedPiece !== null`, skip all grid collision checks
in `onTouchMove` entirely. Piece follows the finger exactly. Snap/bounce physics
resume only after `onTouchUp`.

**Feel:** Most fluid. Player has full control during touch; collision only matters
for physics between frames.

**Risk:** Piece can visually overlap solid cells while being dragged. Acceptable
because the overlap is intentional and the player controls it.

**Key guard:** Still enforce wall clamping (`keepPieceInsideWalls`) so the piece
can't leave the play area.

---

## Option 3 — Ghost drag + resolve on release

**What changes:** Same as Option 2 during drag. On `onTouchUp`, if the piece
overlaps any grid cell, run a push-resolve (same loop as `turnPieceRigidInternal`)
to find the nearest non-overlapping position before resuming physics.

**Feel:** Finger control is unrestricted. Landing spot is corrected silently on
release, so no visual pop during play.

**Risk:** If the piece is deeply embedded on release, the push-resolve may jump
it noticeably. More implementation surface than Option 2.

---

## Decision

To be decided. Option 2 is the simplest and most fluid. Option 3 adds a clean
landing guarantee. Option 1 is conservative.

## Files to change

- `GameEngine.kt` — `onTouchMove` (collision guard), possibly `onTouchUp` (resolve)
- Tests — `SnapSlideTest.kt` (update/add cases for chosen behavior)
