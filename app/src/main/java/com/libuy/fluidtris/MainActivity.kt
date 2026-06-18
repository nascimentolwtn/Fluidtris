package com.libuy.fluidtris

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

private const val SPLASH_DURATION_MS = 2000L

class MainActivity : AppCompatActivity(), FluidTetrisView.GameListener {
    private var gameView: FluidTetrisView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable immersive fullscreen mode immediately (covers splash and game)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        Handler(Looper.getMainLooper()).postDelayed({
            setTheme(R.style.Theme_Fluidtris)
            enableEdgeToEdge()
            setContentView(R.layout.activity_main)

            // No padding needed since UI is hidden
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
                insets
            }

            gameView = findViewById<FluidTetrisView>(R.id.gameView)
            gameView?.gameListener = this
        }, SPLASH_DURATION_MS)
    }

    override fun onPause() {
        super.onPause()
        gameView?.onAppPause()
    }

    override fun onResume() {
        super.onResume()
        gameView?.onAppResume()
    }

    override fun onExitPressed() {
        finish()
    }
}