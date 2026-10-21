package com.aimmaster.ff.ui

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.aimmaster.ff.data.StatsRepository
import com.aimmaster.ff.databinding.ActivityStatsBinding
import com.aimmaster.ff.training.TrainingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StatsActivity : AppCompatActivity() {

    private lateinit var b: ActivityStatsBinding
    private lateinit var repo: StatsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityStatsBinding.inflate(layoutInflater)
        setContentView(b.root)
        repo = StatsRepository(this)

        b.btnBack.setOnClickListener { finish() }
        b.btnClear.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Apagar histórico?")
                .setMessage("Todos os resultados de treino serão removidos deste aparelho.")
                .setPositiveButton("Apagar") { _, _ -> repo.clear(); render() }
                .setNegativeButton("Cancelar", null)
                .show()
        }
        render()
    }

    private fun render() {
        val all = repo.all()
        if (all.isEmpty()) {
            b.tvSummary.text = "Nenhum treino registrado ainda. Faça uma sessão para ver sua evolução aqui."
            b.tvRecent.text = ""
            b.chart.setValues(emptyList())
            return
        }

        val sb = StringBuilder()
        for (m in TrainingMode.values()) {
            val l = all.filter { it.mode == m.name }
            if (l.isEmpty()) continue
            val react = l.map { it.avgReactionMs }.filter { it > 0 }
            sb.append("${m.label}\n")
            sb.append("  Sessões: ${l.size}\n")
            sb.append("  Melhor pontuação: ${l.maxOf { it.score }}\n")
            sb.append("  Precisão média: ${l.map { it.accuracy }.average().toInt()}%\n")
            sb.append("  Reação média: ${if (react.isEmpty()) 0 else react.average().toInt()} ms\n")
            if (m != TrainingMode.HEADSHOT) {
                val pull = l.map { it.avgPullMs }.filter { it > 0 }
                sb.append("  Puxada média: ${if (pull.isEmpty()) 0 else pull.average().toInt()} ms\n")
            }
            sb.append("\n")
        }
        if (all.size >= 6) {
            val recent = all.takeLast(5).map { it.accuracy }.average()
            val before = all.dropLast(5).takeLast(5).map { it.accuracy }.average()
            val diff = (recent - before).toInt()
            sb.append("Evolução (últimas 5 vs 5 anteriores): " + (if (diff > 0) "+$diff" else "$diff") + " pontos de precisão")
        } else {
            sb.append("Faça pelo menos 6 sessões para ver sua evolução.")
        }
        b.tvSummary.text = sb.toString().trim()

        b.chart.setValues(all.takeLast(20).map { it.accuracy })

        val fmt = SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR"))
        val recentText = all.takeLast(8).reversed().joinToString("\n") {
            val name = when (it.mode) {
                TrainingMode.FLICK.name -> "Capa"
                TrainingMode.DEMO.name -> "Demo"
                else -> "Mira"
            }
            "${fmt.format(Date(it.timestamp))}  $name  ${it.score} pts  ${it.accuracy}%  ${it.avgReactionMs} ms"
        }
        b.tvRecent.text = "Últimas sessões\n$recentText"
    }
}
