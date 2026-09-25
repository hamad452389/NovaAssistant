package com.nova.assistant

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var statusText: TextView
    private lateinit var apiKeyInput: EditText

    private val permissionsNeeded = mutableListOf(Manifest.permission.RECORD_AUDIO).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    private val permissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            startNovaService()
        } else {
            Toast.makeText(this, "Nova ko chalane ke liye mic permission zaroori hai", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("nova_prefs", MODE_PRIVATE)
        statusText = findViewById(R.id.statusText)
        apiKeyInput = findViewById(R.id.apiKeyInput)
        apiKeyInput.setText(prefs.getString("api_key", ""))

        findViewById<Button>(R.id.saveKeyButton).setOnClickListener {
            prefs.edit().putString("api_key", apiKeyInput.text.toString().trim()).apply()
            Toast.makeText(this, "API key saved", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.startButton).setOnClickListener {
            if (hasAllPermissions()) startNovaService() else permissionLauncher.launch(permissionsNeeded)
        }

        findViewById<Button>(R.id.stopButton).setOnClickListener {
            stopService(Intent(this, NovaVoiceService::class.java))
            statusText.text = "Nova band hai"
        }
    }

    private fun hasAllPermissions(): Boolean = permissionsNeeded.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun startNovaService() {
        val apiKey = prefs.getString("api_key", "") ?: ""
        if (apiKey.isBlank()) {
            Toast.makeText(this, "Pehle apna Claude API key save karein", Toast.LENGTH_LONG).show()
            return
        }
        val intent = Intent(this, NovaVoiceService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        statusText.text = "Nova sun rahi hai... 'Nova' bol kar command dein"
    }
}
