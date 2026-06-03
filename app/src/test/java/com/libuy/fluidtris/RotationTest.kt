package com.libuy.fluidtris

import org.junit.Test
import org.junit.Assert.assertEquals

class RotationTest {

    // I-piece: 1 row × 4 cols [[1, 1, 1, 1]]
    private val iPiece = listOf(listOf(1, 1, 1, 1))

    // O-piece: 2×2 [[1,1], [1,1]]
    private val oPiece = listOf(listOf(1, 1), listOf(1, 1))

    // T-piece: 2×3 [[1,1,1], [0,1,0]]
    private val tPiece = listOf(listOf(1, 1, 1), listOf(0, 1, 0))

    @Test
    fun ipiece_90deg_shape_is_4rows_1col() {
        val rotated = rotatePiece(iPiece, 90f)
        assertEquals("90° rotated I-piece should have 4 rows", 4, rotated.size)
        assertEquals("90° rotated I-piece should have 1 col", 1, rotated[0].size)
    }

    @Test
    fun ipiece_90deg_all_cells_are_1() {
        val rotated = rotatePiece(iPiece, 90f)
        for (row in rotated) {
            for (cell in row) {
                assertEquals("All cells in rotated I-piece should be 1", 1, cell)
            }
        }
    }

    @Test
    fun ipiece_270deg_shape_is_4rows_1col() {
        val rotated = rotatePiece(iPiece, 270f)
        assertEquals("270° rotated I-piece should have 4 rows", 4, rotated.size)
        assertEquals("270° rotated I-piece should have 1 col", 1, rotated[0].size)
    }

    @Test
    fun ipiece_270deg_all_cells_are_1() {
        val rotated = rotatePiece(iPiece, 270f)
        for (row in rotated) {
            for (cell in row) {
                assertEquals("All cells in rotated I-piece should be 1", 1, cell)
            }
        }
    }

    @Test
    fun tpiece_90deg_shape_is_3rows_2cols() {
        val rotated = rotatePiece(tPiece, 90f)
        assertEquals("90° rotated T-piece should have 3 rows", 3, rotated.size)
        assertEquals("90° rotated T-piece should have 2 cols", 2, rotated[0].size)
    }

    @Test
    fun tpiece_270deg_shape_is_3rows_2cols() {
        val rotated = rotatePiece(tPiece, 270f)
        assertEquals("270° rotated T-piece should have 3 rows", 3, rotated.size)
        assertEquals("270° rotated T-piece should have 2 cols", 2, rotated[0].size)
    }

    @Test
    fun ipiece_180deg_is_same_as_0deg() {
        val rotated = rotatePiece(iPiece, 180f)
        assertEquals("180° rotated I-piece should still be 1×4", iPiece.size, rotated.size)
        assertEquals("180° rotated I-piece should still be 1×4", iPiece[0].size, rotated[0].size)
    }

    @Test
    fun any_piece_four_90deg_rotations_returns_to_original() {
        var rotated = tPiece
        repeat(4) {
            rotated = rotatePiece(rotated, 90f)
        }
        assertEquals("Four 90° rotations should return to original shape", tPiece, rotated)
    }

    @Test
    fun opiece_90deg_unchanged() {
        val rotated = rotatePiece(oPiece, 90f)
        assertEquals("O-piece should be unchanged after any rotation", oPiece, rotated)
    }

    @Test
    fun opiece_180deg_unchanged() {
        val rotated = rotatePiece(oPiece, 180f)
        assertEquals("O-piece should be unchanged after any rotation", oPiece, rotated)
    }

    @Test
    fun opiece_270deg_unchanged() {
        val rotated = rotatePiece(oPiece, 270f)
        assertEquals("O-piece should be unchanged after any rotation", oPiece, rotated)
    }
}
