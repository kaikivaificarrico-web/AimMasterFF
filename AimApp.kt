package com.aimmaster.ff

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.aimmaster.ff.data.SettingsRepository

class AimApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val dark = SettingsRepository(this).darkMode
        AppCompatDelegate.setDefaultNightMode(
            if (dark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}
