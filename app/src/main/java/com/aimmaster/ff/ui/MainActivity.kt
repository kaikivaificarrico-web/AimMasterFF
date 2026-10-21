package com.aimmaster.ff.ui

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.aimmaster.ff.databinding.ActivityMainBinding
import com.aimmaster.ff.training.TrainingMode

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private var pulse: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnAim.setOnClickListener { openTraining(TrainingMode.HEADSHOT) }
        b.btnFlick.setOnClickListener { openTraining(TrainingMode.FLICK) }
        b.btnDemo.setOnClickListener { openTraining(TrainingMode.DEMO) }
        b.btnSens.setOnClickListener { startActivity(Intent(this, SensitivityActivity::class.java)) }
        b.btnStats.setOnClickListener { startActivity(Intent(this, StatsActivity::class.java)) }
        b.btnSettings.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }

        animateIn()
    }

    private fun openTraining(mode: TrainingMode) {
        startActivity(Intent(this, TrainingActivity::class.java).putExtra(TrainingActivity.EXTRA_MODE, mode.name))
    }

    private fun animateIn() {
        for (i in 0 until b.menuContainer.childCount) {
            val v = b.menuContainer.getChildAt(i)
            v.alpha = 0f
            v.translationY = 60f
            v.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(i * 80L)
                .setDuration(450)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
        pulse = ObjectAnimator.ofFloat(b.title, "alpha", 1f, 0.7f, 1f).apply {
            duration = 2400
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
    }

    override fun onDestroy() {
        pulse?.cancel()
        super.onDestroy()
    }
}
