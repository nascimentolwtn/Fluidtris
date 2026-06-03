# Add Backlog: Fluid Jelly Piece Physics

## Context

The current collision system uses hard stops — when a piece touches another piece or wall, it immediately stops falling and a timer starts for locking in place. This creates rigid, blocky behavior that doesn't feel as smooth or responsive as the "fluid" aesthetic the game is aiming for.

The goal is to document and track a feature idea: make pieces more fluid and jelly-like so they can slide along the edges of obstacles while continuing to fall, rather than stopping on first contact.

## Feature Idea

**Fluid piece sliding on collision**

When a falling piece collides with another piece or wall:
- Instead of stopping immediately, the piece should attempt to continue falling while sliding along the collision surface
- Individual blocks within the piece that collide become "locked" to that obstacle
- The piece continues falling/sliding until another block within it collides with a solid surface
- Once all blocks are in contact with something solid (or 3 seconds elapse), the piece locks in place

This creates more dynamic, fluid motion rather than hard stops, giving the game a "jelly-like" feel.

## Location in Backlog

Added to `.claude/napkin.md` under "Backlog (Game Features & Fixes)" section.

## Next Steps

- Design the collision response system to support partial piece contact
- Modify `checkCollisions()` and related logic in `FluidTetrisView.kt`
- Test with various piece shapes and wall/piece configurations