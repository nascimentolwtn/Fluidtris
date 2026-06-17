package com.libuy.fluidtris

import android.content.Context
import android.media.MediaPlayer

internal class SoundManager(private val context: Context) {
    var enabled = true
    private var currentMediaPlayer: MediaPlayer? = null
    private val soundQueue = mutableListOf<Int>()

    fun playMove() {
        if (enabled) queueSound(R.raw.move_sound)
    }

    fun playRigid() {
        if (enabled) playImmediate(R.raw.rigid_sound)
    }

    fun playLevelUpSound() {
        if (enabled) queueSound(R.raw.game_level_up)
    }

    private fun queueSound(resId: Int) {
        soundQueue.add(resId)
        if (currentMediaPlayer == null) playNextInQueue()
    }

    private fun playImmediate(resId: Int) {
        try {
            val mp = MediaPlayer.create(context, resId)
            mp?.setOnCompletionListener { it.release() }
            mp?.start()
        } catch (_: Exception) {}
    }

    private fun playNextInQueue() {
        if (soundQueue.isEmpty()) return
        val resId = soundQueue.removeAt(0)
        try {
            currentMediaPlayer?.release()
            val mp = MediaPlayer.create(context, resId)
            currentMediaPlayer = mp
            mp?.setOnCompletionListener {
                it.release()
                currentMediaPlayer = null
                playNextInQueue()
            }
            mp?.start()
        } catch (_: Exception) {
            currentMediaPlayer = null
            playNextInQueue()
        }
    }
}
