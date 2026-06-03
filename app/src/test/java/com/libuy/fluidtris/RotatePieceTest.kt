package com.libuy.fluidtris

import org.junit.Test
import org.junit.Assert.assertEquals

class RotatePieceTest {

    private val I = listOf(listOf(1, 1, 1, 1))
    private val T = listOf(listOf(1, 1, 1), listOf(0, 1, 0))
    private val O = listOf(listOf(1, 1), listOf(1, 1))

    // I-piece

    @Test
    fun ipiece_0deg_unchanged() {
        assertEquals(listOf(listOf(1, 1, 1, 1)), rotatePiece(I, 0f))
    }

    @Test
    fun ipiece_90deg_isVertical() {
        assertEquals(listOf(listOf(1), listOf(1), listOf(1), listOf(1)), rotatePiece(I, 90f))
    }

    @Test
    fun ipiece_180deg_unchanged() {
        assertEquals(listOf(listOf(1, 1, 1, 1)), rotatePiece(I, 180f))
    }

    @Test
    fun ipiece_270deg_isVertical() {
        assertEquals(listOf(listOf(1), listOf(1), listOf(1), listOf(1)), rotatePiece(I, 270f))
    }

    @Test
    fun ipiece_360deg_isIdentity() {
        assertEquals(listOf(listOf(1, 1, 1, 1)), rotatePiece(I, 360f))
    }

    // T-piece

    @Test
    fun tpiece_0deg_unchanged() {
        assertEquals(listOf(listOf(1, 1, 1), listOf(0, 1, 0)), rotatePiece(T, 0f))
    }

    @Test
    fun tpiece_90deg() {
        assertEquals(listOf(listOf(0, 1), listOf(1, 1), listOf(0, 1)), rotatePiece(T, 90f))
    }

    @Test
    fun tpiece_180deg() {
        assertEquals(listOf(listOf(0, 1, 0), listOf(1, 1, 1)), rotatePiece(T, 180f))
    }

    @Test
    fun tpiece_270deg() {
        assertEquals(listOf(listOf(1, 0), listOf(1, 1), listOf(1, 0)), rotatePiece(T, 270f))
    }

    @Test
    fun tpiece_composedRotations() {
        assertEquals(rotatePiece(T, 180f), rotatePiece(rotatePiece(T, 90f), 90f))
    }

    // O-piece

    @Test
    fun opiece_allRotationsIdentical() {
        val expected = listOf(listOf(1, 1), listOf(1, 1))
        assertEquals(expected, rotatePiece(O, 0f))
        assertEquals(expected, rotatePiece(O, 90f))
        assertEquals(expected, rotatePiece(O, 180f))
        assertEquals(expected, rotatePiece(O, 270f))
    }
}
