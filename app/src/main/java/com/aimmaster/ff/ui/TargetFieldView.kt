package com.aimmaster.ff.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.os.SystemClock
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import com.aimmaster.ff.training.SessionResult
import com.aimmaster.ff.training.TrainingMode
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Campo de treino 100% independente: desenha alvos, trata toques e calcula
 * pontuação, precisão e tempos. Não lê nem escreve nada fora do app.
 */
class TargetFieldView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var mode: TrainingMode = TrainingMode.HEADSHOT
    var targetScale = 1f
    var speedScale = 1f
    var haptics = true
    var onHud: ((String) -> Unit)? = null
    var onFinished: ((SessionResult) -> Unit)? = null

    val isRunning: Boolean get() = running

    private enum class Zone { HEAD, BODY, MISS }
    private enum class FlickState { WAIT, GHOST, READY, DRAG, FEEDBACK }

    private val sessionMs = 30_000L
    private val flickRounds: Int get() = if (mode == TrainingMode.DEMO) 10 else 15

    private var running = false
    private var startAt = 0L
    private var lastFrame = 0L
    private var lastHud = 0L

    // alvo
    private var tx = 0f
    private var ty = 0f
    private var tvx = 0f
    private var tSize = 0f
    private var tVisible = false
    private var tSpawn = 0L
    private var tLifeMs = 0L
    private var nextSpawnAt = 0L
    private var nextTurnAt = 0L
    private var reactionRecorded = false

    // estatísticas
    private var shots = 0
    private var heads = 0
    private var bodies = 0
    private var misses = 0
    private var score = 0
    private var reactionSum = 0L
    private var reactionCount = 0
    private var pullSum = 0L
    private var pullCount = 0

    // modo capa
    private var flickState = FlickState.WAIT
    private var round = 0
    private var dragStart = 0L
    private var ghostStart = 0L
    private var ghostDur = 1400L
    private var feedbackUntil = 0L
    private var feedbackText = ""
    private var feedbackColor = Color.WHITE
    private val trail = ArrayList<PointF>()

    // marcador de tiro
    private var markX = 0f
    private var markY = 0f
    private var markUntil = 0L
    private var markColor = Color.WHITE

    private val density = resources.displayMetrics.density
    private val gridPaint = Paint().apply {
        color = Color.parseColor("#1A2130")
        strokeWidth = density
    }
    private val bodyFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#2B3340") }
    private val bodyStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3 * density
        color = Color.parseColor("#FF5A1F")
    }
    private val headFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E63946") }
    private val headStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3 * density
        color = Color.WHITE
    }
    private val guidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4 * density
        color = Color.parseColor("#00D1FF")
    }
    private val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5 * density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.parseColor("#00D1FF")
    }
    private val idealPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5 * density
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#FFD000")
        alpha = 170
    }
    private val ghostPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFD000") }
    private val markPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4 * density
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 30 * density
        isFakeBoldText = true
    }

    private val frame = object : Runnable {
        override fun run() {
            if (!running) return
            update()
            invalidate()
            if (running) postOnAnimation(this)
        }
    }

    // ---------------------------------------------------------------- controle

    fun start() {
        resetState()
        running = true
        startAt = SystemClock.uptimeMillis()
        lastFrame = startAt
        nextSpawnAt = startAt + 600
        if (mode != TrainingMode.HEADSHOT) scheduleNextRound(startAt)
        removeCallbacks(frame)
        postOnAnimation(frame)
    }

    fun stop() {
        running = false
        removeCallbacks(frame)
    }

    private fun resetState() {
        shots = 0; heads = 0; bodies = 0; misses = 0; score = 0
        reactionSum = 0; reactionCount = 0; pullSum = 0; pullCount = 0
        round = 0
        tVisible = false
        reactionRecorded = false
        markUntil = 0
        feedbackUntil = 0
        trail.clear()
        flickState = FlickState.WAIT
    }

    private fun finish() {
        running = false
        removeCallbacks(frame)
        val now = SystemClock.uptimeMillis()
        val result = SessionResult(
            mode = mode.name,
            timestamp = System.currentTimeMillis(),
            shots = shots,
            headshots = heads,
            bodyHits = bodies,
            misses = misses,
            avgReactionMs = if (reactionCount > 0) reactionSum / reactionCount else 0L,
            avgPullMs = if (pullCount > 0) pullSum / pullCount else 0L,
            score = score,
            durationSec = ((now - startAt) / 1000).toInt()
        )
        invalidate()
        onFinished?.invoke(result)
    }

    // ---------------------------------------------------------------- lógica

    private fun update() {
        val now = SystemClock.uptimeMillis()
        val dt = (now - lastFrame).coerceAtMost(50L) / 1000f
        lastFrame = now
        when (mode) {
            TrainingMode.HEADSHOT -> updateHeadshot(now, dt)
            TrainingMode.FLICK, TrainingMode.DEMO -> updateFlick(now)
        }
        if (running) pushHud(now)
    }

    private fun updateHeadshot(now: Long, dt: Float) {
        if (now - startAt >= sessionMs) {
            finish()
            return
        }
        if (!tVisible) {
            if (now >= nextSpawnAt) spawnMoving(now)
            return
        }
        tx += tvx * dt
        val m = tSize * 0.3f
        if (tx < m) { tx = m; tvx = abs(tvx) }
        else if (tx > width - m) { tx = width - m; tvx = -abs(tvx) }
        if (now >= nextTurnAt) {
            tvx = newVelocity(if (tvx > 0) -1 else 1)
            nextTurnAt = now + Random.nextLong(350, 900)
        }
        if (now - tSpawn > tLifeMs) {
            tVisible = false
            nextSpawnAt = now + 300
        }
    }

    private fun newVelocity(dir: Int): Float =
        dir * width * 0.25f * speedScale * (0.6f + Random.nextFloat() * 0.8f)

    private fun spawnMoving(now: Long) {
        tSize = min(width, height) * 0.38f * targetScale
        val m = tSize * 0.6f
        tx = randRange(m, width - m)
        ty = randRange(tSize * 0.55f, height - tSize * 0.45f)
        tvx = newVelocity(if (Random.nextBoolean()) 1 else -1)
        tVisible = true
        tSpawn = now
        tLifeMs = (2800 / speedScale).toLong().coerceIn(1500L, 4000L)
        nextTurnAt = now + Random.nextLong(350, 900)
        reactionRecorded = false
    }

    private fun spawnStatic(now: Long) {
        tSize = min(width, height) * 0.55f * targetScale
        val m = tSize * 0.4f
        tx = randRange(m, width - m)
        ty = randRange(tSize * 0.55f, height - tSize * 0.45f)
        tvx = 0f
        tVisible = true
        tSpawn = now
    }

    private fun randRange(a: Float, b: Float): Float =
        if (b <= a) (a + b) / 2f else a + Random.nextFloat() * (b - a)

    private fun hitTest(x: Float, y: Float): Zone {
        val headR = 0.11f * tSize * 1.1f
        if (hypot(x - tx, y - (ty - 0.30f * tSize)) <= headR) return Zone.HEAD
        val inX = x >= tx - 0.17f * tSize && x <= tx + 0.17f * tSize
        val inY = y >= ty - 0.17f * tSize && y <= ty + 0.36f * tSize
        return if (inX && inY) Zone.BODY else Zone.MISS
    }

    private fun mark(x: Float, y: Float, color: Int, now: Long) {
        markX = x; markY = y; markColor = color; markUntil = now + 300
    }

    private fun buzz() {
        if (haptics) performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    // ---------------------------------------------------------------- modo mira

    private fun shootHeadshot(x: Float, y: Float, now: Long) {
        shots++
        val zone = if (tVisible) hitTest(x, y) else Zone.MISS
        when (zone) {
            Zone.HEAD -> {
                heads++
                val r = now - tSpawn
                recordReaction(r)
                score += 100 + ((600 - r) / 10).toInt().coerceIn(0, 50)
                tVisible = false
                nextSpawnAt = now + Random.nextLong(250, 600)
                mark(x, y, Color.parseColor("#2ECC71"), now)
                buzz()
            }
            Zone.BODY -> {
                bodies++
                recordReaction(now - tSpawn)
                score += 30
                mark(x, y, Color.parseColor("#FFB020"), now)
            }
            Zone.MISS -> {
                misses++
                score = (score - 10).coerceAtLeast(0)
                mark(x, y, Color.parseColor("#E63946"), now)
            }
        }
    }

    private fun recordReaction(ms: Long) {
        if (reactionRecorded) return
        reactionRecorded = true
        reactionSum += ms
        reactionCount++
    }

    // ---------------------------------------------------------------- modo capa

    private fun scheduleNextRound(now: Long) {
        flickState = FlickState.WAIT
        tVisible = false
        trail.clear()
        nextSpawnAt = now + Random.nextLong(600, 1400)
    }

    private fun updateFlick(now: Long) {
        when (flickState) {
            FlickState.WAIT -> if (now >= nextSpawnAt) {
                spawnStatic(now)
                if (mode == TrainingMode.DEMO) {
                    flickState = FlickState.GHOST
                    ghostStart = now
                    ghostDur = if (round < 3) 1400L else 350L
                } else {
                    flickState = FlickState.READY
                }
            }
            FlickState.GHOST -> if (now - ghostStart >= ghostDur + 500) {
                tSpawn = now
                flickState = FlickState.READY
            }
            FlickState.FEEDBACK -> if (now >= feedbackUntil) {
                if (round >= flickRounds) finish() else scheduleNextRound(now)
            }
            else -> Unit
        }
    }

    private fun touchFlick(e: MotionEvent, now: Long) {
        val x = e.x
        val y = e.y
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> if (flickState == FlickState.READY) {
                if (hitTest(x, y) == Zone.BODY) {
                    flickState = FlickState.DRAG
                    dragStart = now
                    reactionSum += now - tSpawn
                    reactionCount++
                    trail.clear()
                    trail.add(PointF(x, y))
                } else {
                    endRound(Zone.MISS, now, x, y, 0L)
                }
            }
            MotionEvent.ACTION_MOVE -> if (flickState == FlickState.DRAG) {
                trail.add(PointF(x, y))
            }
            MotionEvent.ACTION_UP -> if (flickState == FlickState.DRAG) {
                endRound(hitTest(x, y), now, x, y, now - dragStart)
            }
            MotionEvent.ACTION_CANCEL -> if (flickState == FlickState.DRAG) {
                endRound(Zone.MISS, now, x, y, now - dragStart)
            }
        }
    }

    private fun endRound(zone: Zone, now: Long, x: Float, y: Float, pullMs: Long) {
        shots++
        when (zone) {
            Zone.HEAD -> {
                heads++
                pullSum += pullMs
                pullCount++
                score += 100 + ((400 - pullMs) / 8).toInt().coerceIn(0, 50)
                feedbackText = "HEADSHOT  $pullMs ms"
                feedbackColor = Color.parseColor("#2ECC71")
                buzz()
            }
            Zone.BODY -> {
                bodies++
                score += 30
                feedbackText = "Acertou o corpo: faltou subir"
                feedbackColor = Color.parseColor("#FFB020")
            }
            Zone.MISS -> {
                misses++
                feedbackText = "Errou"
                feedbackColor = Color.parseColor("#E63946")
            }
        }
        if (mode == TrainingMode.DEMO && trail.size > 1) {
            val dev = pathDeviationPct()
            feedbackText += "  |  desvio $dev%"
            if (zone == Zone.HEAD) score += (50 - dev * 6).coerceIn(0, 50)
        }
        round++
        mark(x, y, feedbackColor, now)
        feedbackUntil = now + (if (mode == TrainingMode.DEMO) 1600L else 700L)
        flickState = FlickState.FEEDBACK
    }

    // ---------------------------------------------------------------- toque / HUD

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (!running) return true
        val now = SystemClock.uptimeMillis()
        when (mode) {
            TrainingMode.HEADSHOT -> {
                if (e.actionMasked == MotionEvent.ACTION_DOWN ||
                    e.actionMasked == MotionEvent.ACTION_POINTER_DOWN
                ) {
                    val i = e.actionIndex
                    shootHeadshot(e.getX(i), e.getY(i), now)
                }
            }
            TrainingMode.FLICK, TrainingMode.DEMO -> touchFlick(e, now)
        }
        return true
    }

    private fun pushHud(now: Long) {
        if (now - lastHud < 100) return
        lastHud = now
        val acc = if (shots == 0) 0 else heads * 100 / shots
        val text = if (mode == TrainingMode.HEADSHOT) {
            val left = ((sessionMs - (now - startAt)) / 1000 + 1).coerceAtLeast(0)
            "Tempo: ${left}s    Pontos: $score    Precisão: $acc%"
        } else {
            "Rodada: ${min(round + 1, flickRounds)}/$flickRounds    Pontos: $score    Precisão: $acc%"
        }
        onHud?.invoke(text)
    }

    // ---------------------------------------------------------------- desenho

    override fun onDraw(c: Canvas) {
        c.drawColor(Color.parseColor("#0E1117"))
        val step = 60 * density
        var gx = 0f
        while (gx < width) { c.drawLine(gx, 0f, gx, height.toFloat(), gridPaint); gx += step }
        var gy = 0f
        while (gy < height) { c.drawLine(0f, gy, width.toFloat(), gy, gridPaint); gy += step }

        val now = SystemClock.uptimeMillis()
        if (tVisible) drawTarget(c, now)
        if (mode == TrainingMode.DEMO && tVisible) drawDemoOverlay(c, now)

        if (trail.size > 1) {
            for (i in 1 until trail.size) {
                c.drawLine(trail[i - 1].x, trail[i - 1].y, trail[i].x, trail[i].y, trailPaint)
            }
        }
        if (now < markUntil) {
            markPaint.color = markColor
            markPaint.alpha = ((markUntil - now) * 255 / 300).toInt().coerceIn(0, 255)
            c.drawCircle(markX, markY, 22 * density, markPaint)
        }
        if (mode != TrainingMode.HEADSHOT && flickState == FlickState.FEEDBACK) {
            textPaint.color = feedbackColor
            c.drawText(feedbackText, width / 2f, height * 0.14f, textPaint)
        }
    }

    private fun drawTarget(c: Canvas, now: Long) {
        val s = tSize
        val body = RectF(tx - 0.17f * s, ty - 0.17f * s, tx + 0.17f * s, ty + 0.36f * s)
        c.drawRoundRect(body, s * 0.06f, s * 0.06f, bodyFill)
        c.drawRoundRect(body, s * 0.06f, s * 0.06f, bodyStroke)
        val hy = ty - 0.30f * s
        c.drawCircle(tx, hy, 0.11f * s, headFill)
        c.drawCircle(tx, hy, 0.11f * s, headStroke)
        if (mode != TrainingMode.HEADSHOT && flickState == FlickState.READY) {
            val pulse = 0.5f + 0.5f * sin(now / 150.0).toFloat()
            c.drawCircle(tx, ty - 0.04f * s, 0.07f * s * (1f + 0.25f * pulse), guidePaint)
        }
    }

    // ---------------------------------------------------------------- demonstração

    private fun drawDemoOverlay(c: Canvas, now: Long) {
        val ax = tx
        val ay = ty - 0.04f * tSize
        val by = ty - 0.30f * tSize
        val guided = (flickState == FlickState.READY || flickState == FlickState.DRAG) && round < 3
        if (flickState == FlickState.GHOST) {
            val p = ((now - ghostStart).toFloat() / ghostDur).coerceIn(0f, 1f)
            val e = 1f - (1f - p) * (1f - p) * (1f - p)
            val gy = ay + (by - ay) * e
            c.drawLine(ax, ay, ax, gy, idealPaint)
            c.drawCircle(ax, gy, 16 * density, ghostPaint)
            textPaint.color = Color.parseColor("#FFD000")
            val msg = if (ghostDur > 1000) "Observe a puxada (câmera lenta)" else "Observe a puxada (velocidade real)"
            c.drawText(msg, width / 2f, height * 0.14f, textPaint)
        } else if (flickState == FlickState.FEEDBACK || guided) {
            c.drawLine(ax, ay, ax, by, idealPaint)
        }
        if (flickState == FlickState.READY) {
            textPaint.color = Color.WHITE
            c.drawText("Sua vez: repita a puxada", width / 2f, height * 0.14f, textPaint)
        }
    }

    /** Distância média do seu trajeto até a linha ideal peito→cabeça, em % do tamanho do alvo. */
    private fun pathDeviationPct(): Int {
        val ax = tx
        val ay = ty - 0.04f * tSize
        val by = ty - 0.30f * tSize
        var sum = 0f
        for (p in trail) sum += distToSegment(p.x, p.y, ax, ay, ax, by)
        return (sum / trail.size / tSize * 100f).toInt()
    }

    private fun distToSegment(px: Float, py: Float, ax: Float, ay: Float, bx: Float, by: Float): Float {
        val dx = bx - ax
        val dy = by - ay
        val len2 = dx * dx + dy * dy
        val t = if (len2 == 0f) 0f else (((px - ax) * dx + (py - ay) * dy) / len2).coerceIn(0f, 1f)
        return hypot(px - (ax + t * dx), py - (ay + t * dy))
    }
}
