package com.example.ruhi1

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.text.TextUtils
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private var geminiLiveClient: GeminiLiveClient? = null
    
    private val ruhiEventsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "WAKE_WORD_DETECTED" -> {
                    startLiveSession("You were just woken up by the user saying Ruhi. Say a short and sweet greeting like 'Yes, boss?' or 'I am here.'")
                }
                "com.ruhi.ACTION_ANNOUNCE_CALL" -> {
                    val callerName = intent.getStringExtra("callerName")
                    startLiveSession("A call is coming from $callerName. Ask me if I want to answer or reject it.")
                }
                "com.ruhi.ACTION_READ_NOTIFICATION" -> {
                    val sender = intent.getStringExtra("sender")
                    val message = intent.getStringExtra("message")
                    startLiveSession("There is a new WhatsApp message from $sender. It says: $message. Read it out loud and ask if I want to reply.")
                }
                "com.ruhi.ACTION_SCREEN_TEXT_RESULT" -> {
                    val text = intent.getStringExtra("text") ?: ""
                    if (text.isNotBlank()) {
                        geminiLiveClient?.sendSystemText("I am providing the text on the user's screen. Tell me briefly what is on my screen: $text")
                    } else {
                        geminiLiveClient?.sendSystemText("I couldn't read anything on the screen.")
                    }
                }
            }
        }
    }

    private lateinit var tvStatus: TextView
    private lateinit var btnListen: Button
    private lateinit var layoutContainer: android.widget.LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        layoutContainer = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            setBackgroundColor(android.graphics.Color.parseColor("#F5F5F5"))
        }

        tvStatus = TextView(this).apply {
            text = "Ruhi AI Assistant (Live WebSocket Mode)"
            textSize = 20f
            setPadding(0, 0, 0, 32)
            setTextColor(android.graphics.Color.BLACK)
        }
        layoutContainer.addView(tvStatus)

        btnListen = Button(this).apply {
            text = "Tap to Start Live Session"
        }
        layoutContainer.addView(btnListen)

        val sectionTitle = TextView(this).apply {
            text = "Service Toggles"
            textSize = 18f
            setPadding(0, 32, 0, 16)
            setTextColor(android.graphics.Color.DKGRAY)
        }
        layoutContainer.addView(sectionTitle)

        val prefs = getSharedPreferences("RuhiSettings", Context.MODE_PRIVATE)
        val servicesMap = listOf(
            "Wake Word (Background)" to "service_wakeword",
            "Screen Analysis / WhatsApp Send" to "service_screen",
            "WhatsApp Voice Reader" to "service_whatsapp",
            "Call Announcer" to "service_calls"
        )

        for ((title, key) in servicesMap) {
            val switchView = android.widget.Switch(this).apply {
                text = title
                textSize = 16f
                setPadding(0, 16, 0, 16)
                isChecked = prefs.getBoolean(key, true)
                
                setOnCheckedChangeListener { buttonView, isCheckedParam ->
                    prefs.edit().putBoolean(key, isCheckedParam).apply()
                    if (key == "service_wakeword") {
                        val serviceIntent = Intent(this@MainActivity, WakeWordService::class.java)
                        if (isCheckedParam) {
                            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                try {
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                        startForegroundService(serviceIntent)
                                    } else {
                                        startService(serviceIntent)
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            } else {
                                buttonView.isChecked = false
                            }
                        } else {
                            stopService(serviceIntent)
                        }
                    }
                }
            }
            layoutContainer.addView(switchView)
        }

        setContentView(layoutContainer)
        requestPermissions()

        val filter = IntentFilter().apply {
            addAction("WAKE_WORD_DETECTED")
            addAction("com.ruhi.ACTION_ANNOUNCE_CALL")
            addAction("com.ruhi.ACTION_READ_NOTIFICATION")
            addAction("com.ruhi.ACTION_SCREEN_TEXT_RESULT")
        }
        ContextCompat.registerReceiver(this, ruhiEventsReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        
        if (prefs.getBoolean("service_wakeword", true)) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                val serviceIntent = Intent(this, WakeWordService::class.java)
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent)
                    } else {
                        startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        btnListen.setOnClickListener {
            startLiveSession(null)
        }
    }

    private fun startLiveSession(initialPrompt: String?) {
        if (geminiLiveClient?.isRecording == true) return
        
        tvStatus.text = "Ruhi: Active (Live Voice Session)"
        geminiLiveClient = GeminiLiveClient(
            context = this,
            apiKey = "YOUR_GEMINI_API_KEY_HERE", // Replace securely in real app
            onSessionEnded = {
                tvStatus.text = "Ruhi is sleeping... Say 'Ruhi' to wake me."
                geminiLiveClient = null
            }
        )
        geminiLiveClient?.startSession()
        
        if (initialPrompt != null) {
            // Need slight delay to allow websocket to connect before sending system text
            tvStatus.postDelayed({
                geminiLiveClient?.sendSystemText(initialPrompt)
            }, 1000)
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ANSWER_PHONE_CALLS,
            Manifest.permission.READ_CALL_LOG
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        val needed = permissions.filter { 
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED 
        }
        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), 1)
        }
        
        checkAdvancedPermissions()
    }

    private fun checkAdvancedPermissions() {
        checkAccessibilityService()
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val btnOverlay = Button(this).apply {
                    text = "Enable Display Over Other Apps (For Background Actions)"
                    setOnClickListener {
                        try {
                            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:$packageName"))
                            startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            tvStatus.text = "I need display over other apps permission."
                        }
                    }
                }
                layoutContainer.addView(btnOverlay)
            }
            
            val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                val btnBattery = Button(this).apply {
                    text = "Disable Battery Optimization (Keeps Ruhi Alive)"
                    setOnClickListener {
                        try {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, android.net.Uri.parse("package:$packageName"))
                            startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            tvStatus.text = "Could not open battery settings."
                        }
                    }
                }
                layoutContainer.addView(btnBattery)
            }
        }
    }

    private fun checkAccessibilityService() {
        if (!isAccessibilityServiceEnabled(this, RuhiAccessibilityService::class.java)) {
            tvStatus.text = "Boss, please enable Accessibility Service in Settings later."
            val btnAcc = Button(this).apply {
                text = "Enable Accessibility (Screen/WhatsApp)"
                setOnClickListener {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                }
            }
            layoutContainer.addView(btnAcc)
        }
        
        val enabledListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (enabledListeners == null || !enabledListeners.contains(packageName)) {
            val btnNotif = Button(this).apply {
                text = "Enable Notification Access (Read Msgs)"
                setOnClickListener {
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                }
            }
            layoutContainer.addView(btnNotif)
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context, accessibilityService: Class<*>): Boolean {
        val expectedComponentName = android.content.ComponentName(context, accessibilityService)
        val enabledServicesSetting = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)
        while (colonSplitter.hasNext()) {
            val componentNameString = colonSplitter.next()
            val enabledService = android.content.ComponentName.unflattenFromString(componentNameString)
            if (enabledService != null && enabledService == expectedComponentName) return true
        }
        return false
    }

    override fun onDestroy() {
        geminiLiveClient?.stopSession()
        unregisterReceiver(ruhiEventsReceiver)
        super.onDestroy()
    }
}
