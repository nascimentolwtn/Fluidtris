package com.libuy.fluidtris

import android.content.Context

internal class HighScoreManager(context: Context) {
    private val prefs = context.getSharedPreferences("fluidtris_prefs", Context.MODE_PRIVATE)

    fun loadHighScore(): Int {
        return prefs.getInt("high_score", 0)
    }

    fun saveHighScore(score: Int) {
        prefs.edit().putInt("high_score", score).apply()
    }

    fun isNewHighScore(score: Int): Boolean {
        return score > loadHighScore()
    }
}
