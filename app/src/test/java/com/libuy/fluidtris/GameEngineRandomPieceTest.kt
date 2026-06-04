package com.libuy.fluidtris

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineRandomPieceTest {

    private val VW = 1080
    private val VH = 1920
    private val validRotations = setOf(0f, 90f, 180f, 270f)

    private fun engine(): GameEngine {
        val e = GameEngine(onPieceLocked = {}, onLineCleared = {})
        e.resetGame(VW, VH)
        return e
    }

    // ---- resetGame ----

    @Test
    fun afterReset_currentPieceIsValid() {
        val e = engine()
        assertTrue(e.currentPiece in 0 until GameConstants.PIECES.size)
    }

    @Test
    fun afterReset_nextPieceIsValid() {
        val e = engine()
        assertTrue(e.nextPiece in 0 until GameConstants.PIECES.size)
    }

    @Test
    fun afterReset_nextPieceRotationIsValid() {
        val e = engine()
        assertTrue(e.nextPieceRotation in validRotations)
    }

    @Test
    fun afterReset_currentPieceColorMatchesPiece() {
        val e = engine()
        assertEquals(GameConstants.PIECE_COLORS[e.currentPiece], e.currentPieceColor)
    }

    @Test
    fun afterReset_nextPieceColorMatchesPiece() {
        val e = engine()
        assertEquals(GameConstants.PIECE_COLORS[e.nextPiece], e.nextPieceColor)
    }

    // ---- spawn (turnPieceRigid promotes nextPiece) ----

    @Test
    fun afterSpawn_currentPieceBecomesWhatWasNext() {
        val e = engine()
        e.currentPiece = 0  // force I-piece for predictable lock position
        e.pieceRotation = 0f
        val savedNext = e.nextPiece
        e.turnPieceRigid(VW, VH)
        assertEquals(savedNext, e.currentPiece)
    }

    @Test
    fun afterSpawn_pieceRotationMatchesSavedNextPieceRotation() {
        val e = engine()
        e.currentPiece = 0
        e.pieceRotation = 0f
        val savedRotation = e.nextPieceRotation
        e.turnPieceRigid(VW, VH)
        assertEquals(savedRotation, e.pieceRotation, 0.001f)
    }

    @Test
    fun afterSpawn_newNextPieceIsValid() {
        val e = engine()
        e.currentPiece = 0
        e.pieceRotation = 0f
        e.turnPieceRigid(VW, VH)
        assertTrue(e.nextPiece in 0 until GameConstants.PIECES.size)
    }

    @Test
    fun afterSpawn_newNextPieceRotationIsValid() {
        val e = engine()
        e.currentPiece = 0
        e.pieceRotation = 0f
        e.turnPieceRigid(VW, VH)
        assertTrue(e.nextPieceRotation in validRotations)
    }

    @Test
    fun afterSpawn_newNextPieceColorMatchesPiece() {
        val e = engine()
        e.currentPiece = 0
        e.pieceRotation = 0f
        e.turnPieceRigid(VW, VH)
        assertEquals(GameConstants.PIECE_COLORS[e.nextPiece], e.nextPieceColor)
    }

    // ---- statistical: values actually vary ----

    @Test
    fun multipleResets_pieceTypesVary() {
        val pieces = (1..50).map {
            GameEngine(onPieceLocked = {}, onLineCleared = {})
                .also { e -> e.resetGame(VW, VH) }.currentPiece
        }.toSet()
        assertTrue("Expected at least 3 distinct piece types across 50 resets", pieces.size >= 3)
    }

    @Test
    fun multipleResets_rotationsVary() {
        val rotations = (1..50).map {
            GameEngine(onPieceLocked = {}, onLineCleared = {})
                .also { e -> e.resetGame(VW, VH) }.nextPieceRotation
        }.toSet()
        assertTrue("Expected at least 2 distinct rotations across 50 resets", rotations.size >= 2)
    }
}