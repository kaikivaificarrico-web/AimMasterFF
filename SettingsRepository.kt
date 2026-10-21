package com.aimmaster.ff.data

import android.content.Context

class SettingsRepository(context: Context) {
    private val p = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var darkMode: Boolean
        get() = p.getBoolean("dark", true)
        set(v) { p.edit().putBoolean("dark", v).apply() }

    var haptics: Boolean
        get() = p.getBoolean("haptics", true)
        set(v) { p.edit().putBoolean("haptics", v).apply() }

    /** 0..100 */
    var targetSizePct: Int
        get() = p.getInt("size", 33)
        set(v) { p.edit().putInt("size", v).apply() }

    /** 0..100 */
    var speedPct: Int
        get() = p.getInt("speed", 33)
        set(v) { p.edit().putInt("speed", v).apply() }

    val targetScale: Float get() = 0.5f + targetSizePct / 100f * 1.5f
    val speedScale: Float get() = 0.5f + speedPct / 100f * 1.5f
}
