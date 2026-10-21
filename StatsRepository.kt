package com.aimmaster.ff.data

import android.content.Context
import com.aimmaster.ff.training.SessionResult
import org.json.JSONArray

class StatsRepository(context: Context) {
    private val p = context.getSharedPreferences("stats", Context.MODE_PRIVATE)

    fun all(): List<SessionResult> {
        val raw = p.getString("sessions", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { SessionResult.fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun add(r: SessionResult) {
        val list = (all() + r).takeLast(MAX_SESSIONS)
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        p.edit().putString("sessions", arr.toString()).apply()
    }

    fun clear() {
        p.edit().remove("sessions").apply()
    }

    companion object {
        private const val MAX_SESSIONS = 300
    }
}
