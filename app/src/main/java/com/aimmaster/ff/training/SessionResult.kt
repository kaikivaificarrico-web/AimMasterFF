package com.aimmaster.ff.training

import org.json.JSONObject

data class SessionResult(
    val mode: String,
    val timestamp: Long,
    val shots: Int,
    val headshots: Int,
    val bodyHits: Int,
    val misses: Int,
    val avgReactionMs: Long,
    val avgPullMs: Long,
    val score: Int,
    val durationSec: Int
) {
    /** Precisão de cabeça: % de tiros que acertaram a cabeça. */
    val accuracy: Int get() = if (shots == 0) 0 else headshots * 100 / shots

    fun toJson(): JSONObject = JSONObject().apply {
        put("mode", mode)
        put("ts", timestamp)
        put("shots", shots)
        put("heads", headshots)
        put("bodies", bodyHits)
        put("misses", misses)
        put("react", avgReactionMs)
        put("pull", avgPullMs)
        put("score", score)
        put("dur", durationSec)
    }

    companion object {
        fun fromJson(o: JSONObject) = SessionResult(
            mode = o.optString("mode", TrainingMode.HEADSHOT.name),
            timestamp = o.optLong("ts"),
            shots = o.optInt("shots"),
            headshots = o.optInt("heads"),
            bodyHits = o.optInt("bodies"),
            misses = o.optInt("misses"),
            avgReactionMs = o.optLong("react"),
            avgPullMs = o.optLong("pull"),
            score = o.optInt("score"),
            durationSec = o.optInt("dur")
        )
    }
}
