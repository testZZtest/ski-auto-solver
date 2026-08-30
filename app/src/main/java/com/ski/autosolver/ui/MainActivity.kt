package com.ski.autosolver.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*
import android.text.InputType
import com.ski.autosolver.R
import com.ski.autosolver.accessibility.TouchAccessibilityService
import com.ski.autosolver.capture.CaptureService
import com.ski.autosolver.engine.GameConfiguration
import com.ski.autosolver.model.GameType
import com.ski.autosolver.overlay.OverlayService

class MainActivity : Activity() {
    private val projectionRequest = 7001
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        status = findViewById(R.id.statusText)
        val spinner = findViewById<Spinner>(R.id.gameSpinner)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, GameType.entries.map { it.label })
        spinner.setSelection(GameType.entries.indexOf(GameConfiguration.gameType))
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) { GameConfiguration.gameType = GameType.entries[pos] }
            override fun onNothingSelected(p: AdapterView<*>?) = Unit
        }

        val tubeEdit = findViewById<EditText>(R.id.tubeCountEdit)
        tubeEdit.inputType = InputType.TYPE_CLASS_NUMBER
        tubeEdit.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) GameConfiguration.waterTubeCount = tubeEdit.text.toString().toIntOrNull()?.coerceIn(2, 16) ?: 8 }

        findViewById<Button>(R.id.overlayButton).setOnClickListener { enableOverlay() }
        findViewById<Button>(R.id.captureButton).setOnClickListener { requestCapture() }
        findViewById<Button>(R.id.accessButton).setOnClickListener { startActivity(Intent("android.settings.ACCESSIBILITY_SETTINGS")) }
        findViewById<Switch>(R.id.autoSwitch).setOnCheckedChangeListener { _, checked -> GameConfiguration.enabled = checked }
        updateStatus()
    }

    private fun enableOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            Toast.makeText(this, "Permita a sobreposição e toque novamente.", Toast.LENGTH_LONG).show()
            return
        }
        startService(Intent(this, OverlayService::class.java)); updateStatus()
    }

    private fun requestCapture() {
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(manager.createScreenCaptureIntent(), projectionRequest)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == projectionRequest && resultCode == RESULT_OK && data != null) {
            val i = Intent(this, CaptureService::class.java).apply {
                putExtra(CaptureService.EXTRA_RESULT_CODE, resultCode)
                putExtra(CaptureService.EXTRA_RESULT_DATA, data)
            }
            startForegroundService(i)
            GameConfiguration.enabled = true
            findViewById<Switch>(R.id.autoSwitch).isChecked = true
        }
        updateStatus()
    }

    private fun updateStatus() {
        status.text = "Overlay: ${if (Settings.canDrawOverlays(this)) "OK" else "não permitido"}\n" +
                "Toque automático: ${if (TouchAccessibilityService.instance != null) "conectado" else "ative nas configurações"}\n" +
                "Captura: conceda a permissão pelo botão acima\n" +
                "Solver: ${GameConfiguration.gameType.label}"
    }
}
