package com.aimmaster.ff.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.aimmaster.ff.R

/** Gráfico de linha simples (valores de 0 a 100). */
class ChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var values: List<Int> = emptyList()
    private val d = resources.displayMetrics.density

    private val grid = Paint().apply {
        color = ContextCompat.getColor(context, R.color.surface2)
        strokeWidth = d
    }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.accent)
        style = Paint.Style.STROKE
        strokeWidth = 3 * d
        strokeJoin = Paint.Join.ROUND
    }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.accent)
    }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_dim)
        textSize = 12 * d
    }

    fun setValues(v: List<Int>) {
        values = v
        invalidate()
    }

    override fun onDraw(c: Canvas) {
        val padL = 36 * d
        val padR = 16 * d
        val padT = 16 * d
        val padB = 16 * d
        val w = width - padL - padR
        val h = height - padT - padB
        for (v in intArrayOf(0, 50, 100)) {
            val y = padT + h * (1 - v / 100f)
            c.drawLine(padL, y, padL + w, y, grid)
            c.drawText("$v", 4 * d, y + 4 * d, label)
        }
        if (values.isEmpty()) {
            c.drawText("Sem dados ainda", padL + 12 * d, padT + h / 2, label)
            return
        }
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = if (values.size == 1) padL + w / 2 else padL + w * i / (values.size - 1)
            val y = padT + h * (1 - v / 100f)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            c.drawCircle(x, y, 4 * d, dot)
        }
        c.drawPath(path, line)
    }
}
