package com.aimmaster.ff.ui

import android.os.Bundle
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.aimmaster.ff.data.SettingsRepository
import com.aimmaster.ff.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var b: ActivitySettingsBinding
    private lateinit var s: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(b.root)
        s = SettingsRepository(this)

        b.btnBack.setOnClickListener { finish() }

        b.swDark.isChecked = s.darkMode
        b.swHaptics.isChecked = s.haptics
        b.seekSize.progress = s.targetSizePct
        b.seekSpeed.progress = s.speedPct
        updateLabels()

        b.swDark.setOnCheckedChangeListener { _, checked ->
            s.darkMode = checked
            AppCompatDelegate.setDefaultNightMode(
                if (checked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
        b.swHaptics.setOnCheckedChangeListener { _, checked -> s.haptics = checked }

        b.seekSize.setOnSeekBarChangeListener(object : SimpleSeek() {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                s.targetSizePct = p
                updateLabels()
            }
        })
        b.seekSpeed.setOnSeekBarChangeListener(object : SimpleSeek() {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                s.speedPct = p
                updateLabels()
            }
        })
    }

    private fun updateLabels() {
        b.tvSize.text = "Tamanho dos alvos: %.1fx".format(s.targetScale)
        b.tvSpeed.text = "Velocidade dos alvos: %.1fx".format(s.speedScale)
    }

    private abstract class SimpleSeek : SeekBar.OnSeekBarChangeListener {
        override fun onStartTrackingTouch(sb: SeekBar?) = Unit
        override fun onStopTrackingTouch(sb: SeekBar?) = Unit
    }
}
