package com.example.ruhi1

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
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

    private lateinit var tvStatus: TextView
    private lateinit var btnToggleSession: Button
    private lateinit var etApiKey: EditText
    private lateinit var btnSaveKey: Button
    private lateinit var sharedPrefs: SharedPreferences

    private var geminiClient: GeminiLiveClient? = null
    private var isSessionActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        btnToggleSession = findViewById(R.id.btnToggleSession)
        etApiKey = findViewById(R.id.etApiKey)
        btnSaveKey = findViewById(R.id.btnSaveKey)

        sharedPrefs = getSharedPreferences("RuhiPrefs", Context.MODE_PRIVATE)

        val savedKey = sharedPrefs.getString("GEMINI_API_KEY", "")
        if (!savedKey.isNullOrEmpty()) {
            etApiKey.setText(savedKey)
        }

        btnSaveKey.setOnClickListener {
            val key = etApiKey.text.toString().trim()
            if (key.isNotEmpty()) {
                sharedPrefs.edit().putString("GEMINI_API_KEY", key).apply()
                Toast.makeText(this, "API Key Saved!", Toast.LENGTH_SHORT).show()
                checkPermissionsAndStartServices()
            } else {
                Toast.makeText(this, "Please enter a valid API Key", Toast.LENGTH_SHORT).show()
            }
        }

        btnToggleSession.setOnClickListener {
            if (isSessionActive) {
                stopGeminiSession()
            } else {
                startGeminiSession()
            }
        }

        checkPermissionsAndStartServices()
    }

    private fun checkPermissionsAndStartServices() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            permissions.add(Manifest.permission.FOREGROUND_SERVICE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            permissions.add(Manifest.permission.FOREGROUND_SERVICE_MICROPHONE)
        }

        val needRequest = permissions.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        
        if (needRequest) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 100)
        } else {
            startBackgroundServices()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            startBackgroundServices()
        } else {
            Toast.makeText(this, "Core permissions denied! Some features won't work.", Toast.LENGTH_LONG).show()
        }
    }

    private fun startBackgroundServices() {
        try {
            val intent = Intent(this, WakeWordService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun startGeminiSession() {
        val apiKey = sharedPrefs.getString("GEMINI_API_KEY", "")
        if (apiKey.isNullOrEmpty()) {
            Toast.makeText(this, "Save your API Key first!", Toast.LENGTH_SHORT).show()
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Microphone permission required for Live voice", Toast.LENGTH_SHORT).show()
            checkPermissionsAndStartServices()
            return
        }

        tvStatus.text = "Connecting..."
        btnToggleSession.text = "CONNECTING"
        btnToggleSession.isEnabled = false

        geminiClient = GeminiLiveClient(this, apiKey) {
            runOnUiThread {
                isSessionActive = false
                tvStatus.text = "Session Ended"
                btnToggleSession.text = "START"
                btnToggleSession.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#6200EA"))
                btnToggleSession.isEnabled = true
            }
        }

        try {
            geminiClient?.startSession()
            isSessionActive = true
            tvStatus.text = "Connected! Speak Now (Mic Active)"
            btnToggleSession.text = "STOP"
            btnToggleSession.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#D50000"))
            btnToggleSession.isEnabled = true
            Toast.makeText(this, "You can now speak to Ruhi directly", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            tvStatus.text = "Error: " + e.message
            btnToggleSession.isEnabled = true
            isSessionActive = false
        }
    }

    private fun stopGeminiSession() {
        try { geminiClient?.stopSession() } catch(e: Exception) {}
        geminiClient = null
        isSessionActive = false
        tvStatus.text = "Disconnected"
        btnToggleSession.text = "START"
        btnToggleSession.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#6200EA"))
    }

    override fun onDestroy() {
        stopGeminiSession()
        super.onDestroy()
    }
}
