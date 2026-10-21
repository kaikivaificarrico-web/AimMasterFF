package com.aimmaster.ff.ui

import android.app.ActivityManager
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.aimmaster.ff.data.StatsRepository
import com.aimmaster.ff.databinding.ActivitySensitivityBinding
import com.aimmaster.ff.databinding.ItemSensBinding
import com.aimmaster.ff.training.DeviceClass
import com.aimmaster.ff.training.PlayStyle
import com.aimmaster.ff.training.SensitivityCalculator
import kotlin.math.min

class SensitivityActivity : AppCompatActivity() {

    private lateinit var b: ActivitySensitivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySensitivityBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.btnBack.setOnClickListener { finish() }

        b.spDevice.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, DeviceClass.values().map { it.label }
        )
        b.spStyle.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, PlayStyle.values().map { it.label }
        )

        val detected = detectDevice()
        b.spDevice.setSelection(detected.ordinal)
        b.spStyle.setSelection(PlayStyle.BALANCED.ordinal)

        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) = render()
            override fun onNothingSelected(p: AdapterView<*>?) = Unit
        }
        b.spDevice.onItemSelectedListener = listener
        b.spStyle.onItemSelectedListener = listener
        b.seekFine.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) = render()
            override fun onStartTrackingTouch(s: SeekBar?) = Unit
            override fun onStopTrackingTouch(s: SeekBar?) = Unit
        })

        render()
    }

    @Suppress("DEPRECATION")
    private fun detectDevice(): DeviceClass {
        val dm = resources.displayMetrics
        val smallestDp = min(dm.widthPixels, dm.heightPixels) / dm.density
        val hz = windowManager.defaultDisplay.refreshRate
        val lowRam = (getSystemService(ACTIVITY_SERVICE) as ActivityManager).isLowRamDevice
        b.tvDetected.text = "Detectado: tela de ${smallestDp.toInt()} dp, ${hz.toInt()} Hz, ${dm.densityDpi} dpi"
        return when {
            smallestDp >= 600 -> DeviceClass.TABLET
            lowRam -> DeviceClass.BASIC
            hz >= 90f -> DeviceClass.HIGH
            else -> DeviceClass.MID
        }
    }

    private fun render() {
        val device = DeviceClass.values()[b.spDevice.selectedItemPosition.coerceAtLeast(0)]
        val style = PlayStyle.values()[b.spStyle.selectedItemPosition.coerceAtLeast(0)]
        val fine = b.seekFine.progress - 10
        b.tvFine.text = "Ajuste fino: " + (if (fine > 0) "+$fine" else "$fine")

        val result = SensitivityCalculator.calculate(device, style, fine)
        b.rows.removeAllViews()
        for ((label, value) in result.items) {
            val row = ItemSensBinding.inflate(layoutInflater, b.rows, false)
            row.tvLabel.text = label
            row.tvValue.text = "$value%"
            row.bar.progress = value
            b.rows.addView(row.root)
        }
        b.tvHint.text = SensitivityCalculator.hintFrom(StatsRepository(this).all())
    }
}
