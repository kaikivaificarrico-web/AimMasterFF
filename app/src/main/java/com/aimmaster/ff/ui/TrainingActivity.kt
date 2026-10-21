package com.aimmaster.ff.ui

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.aimmaster.ff.data.SettingsRepository
import com.aimmaster.ff.data.StatsRepository
import com.aimmaster.ff.databinding.ActivityTrainingBinding
import com.aimmaster.ff.training.SessionResult
import com.aimmaster.ff.training.TrainingMode

class TrainingActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MODE = "mode"
    }

    private lateinit var b: ActivityTrainingBinding
    private lateinit var mode: TrainingMode
    private lateinit var stats: StatsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityTrainingBinding.inflate(layoutInflater)
        setContentView(b.root)

        mode = TrainingMode.valueOf(intent.getStringExtra(EXTRA_MODE) ?: TrainingMode.HEADSHOT.name)
        stats = StatsRepository(this)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, b.root).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val settings = SettingsRepository(this)
        b.field.mode = mode
        b.field.targetScale = settings.targetScale
        b.field.speedScale = settings.speedScale
        b.field.haptics = settings.haptics
        b.field.onHud = { b.hud.text = it }
        b.field.onFinished = { showResult(it) }

        b.btnPrimary.setOnClickListener { startSession() }
        b.btnExit.setOnClickListener { finish() }

        showIntro(paused = false)
    }

    private fun startSession() {
        b.panelHolder.visibility = View.GONE
        b.hud.text = ""
        b.field.start()
    }

    private fun showIntro(paused: Boolean) {
        b.panelHolder.visibility = View.VISIBLE
        b.panelTitle.text = mode.label
        b.panelBody.text = when (mode) {
            TrainingMode.HEADSHOT ->
                "Toque na cabeça (círculo vermelho) dos alvos que se movem. " +
                    "Você tem 30 segundos. Cabeça = 100 pts, corpo = 30 pts, erro = -10 pts. " +
                    "Acertar rápido dá bônus."
            TrainingMode.FLICK ->
                "Quando o alvo aparecer, toque no peito (anel azul) e arraste o dedo até a cabeça, " +
                    "soltando dentro do círculo vermelho. São 15 rodadas. " +
                    "Medimos seu tempo de reação e de puxada."
            TrainingMode.DEMO ->
                "Primeiro observe a mira fantasma (amarela) fazer a puxada perfeita do peito até a cabeça: " +
                    "em câmera lenta nas 3 primeiras rodadas e depois em velocidade real. " +
                    "Depois repita o movimento. Mostramos o seu desvio em relação ao trajeto ideal. São 10 rodadas."
        }
        b.btnPrimary.text = if (paused) "Recomeçar" else "Iniciar"
    }

    private fun showResult(r: SessionResult) {
        stats.add(r)
        b.hud.text = ""
        b.panelHolder.visibility = View.VISIBLE
        b.panelTitle.text = "Resultado: ${mode.label}"
        val sb = StringBuilder()
        sb.append("Pontuação: ${r.score}\n")
        sb.append("Precisão de cabeça: ${r.accuracy}%\n")
        sb.append("Tiros: ${r.shots}  |  Cabeça: ${r.headshots}  |  Corpo: ${r.bodyHits}  |  Erros: ${r.misses}\n")
        sb.append("Tempo de reação médio: ${r.avgReactionMs} ms")
        if (mode != TrainingMode.HEADSHOT) {
            sb.append("\nTempo de puxada médio: ${r.avgPullMs} ms")
        }
        b.panelBody.text = sb.toString()
        b.btnPrimary.text = "Repetir"
    }

    override fun onPause() {
        super.onPause()
        if (b.field.isRunning) {
            b.field.stop()
            b.hud.text = ""
            showIntro(paused = true)
        }
    }
}
