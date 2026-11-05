package com.routeplanner.app.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.routeplanner.app.R

class SplashActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())

    private val navigateRunnable = Runnable {
        startActivity(
            Intent(this, MainActivity::class.java)
        )
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        installSplashScreen()

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_splash)

        // Obtener referencias
        val routeMap = findViewById<RouteMapAnimationView>(
            R.id.routeMap
        )

        val title = findViewById<TextView>(R.id.txtTitle)

        val planifica = findViewById<TextView>(R.id.txtPlanifica)
        val calcula = findViewById<TextView>(R.id.txtCalcula)
        val optimiza = findViewById<TextView>(R.id.txtOptimiza)

        val dot1 = findViewById<TextView>(R.id.dot1)
        val dot2 = findViewById<TextView>(R.id.dot2)
        val dot3 = findViewById<TextView>(R.id.dot3)

        // 1. Animación del mapa, marcadores y recorrido
        routeMap.startAnimation()

        // 2. Aparición del nombre
        title.animate()
            .alpha(1f)
            .setStartDelay(1900)
            .setDuration(800)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // 3. Aparición de Planifica y su punto
        animateText(planifica, 2800)
        animateText(dot1, 3250)

        // 4. Aparición de Calcula y su punto
        animateText(calcula, 3650)
        animateText(dot2, 4100)

        // 5. Aparición de Optimiza y su punto
        animateText(optimiza, 4500)
        animateText(dot3, 4950)

        // 6. Acceder a la pantalla principal
        handler.postDelayed(navigateRunnable, 6200)
    }

    private fun animateText(
        view: TextView,
        delay: Long
    ) {
        view.translationY = 12f
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(delay)
            .setDuration(500)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    override fun onDestroy() {
        handler.removeCallbacks(navigateRunnable)
        super.onDestroy()
    }
}