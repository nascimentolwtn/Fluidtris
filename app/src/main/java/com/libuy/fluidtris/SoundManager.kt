package com.libuy.fluidtris

import android.content.Context
import android.media.MediaPlayer

internal class SoundManager(private val context: Context) {
    var enabled = true

    fun playMove() {
        if (enabled) play(R.raw.move_sound)
    }

    fun playRigid() {
        if (enabled) play(R.raw.rigid_sound)
    }

    private fun play(resId: Int) {
        try {
            val mp = MediaPlayer.create(context, resId)
            mp?.setOnCompletionListener { it.release() }
            mp?.start()
        } catch (_: Exception) {}
    }
}
