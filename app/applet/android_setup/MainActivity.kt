package com.example.ruhiassistant

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
    private val ruhiEventsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "WAKE_WORD_DETECTED" -> {
                    speak("Yes boss?")
                    startListening()
                }
                "com.ruhi.ACTION_ANNOUNCE_CALL" -> {
                    val callerName = intent.getStringExtra("callerName")
                    speak("Boss, $callerName's call is coming. Should I answer or reject?")
                    // Start listening for an answer shortly
                    tvStatus.postDelayed({ startListening() }, 3500)
                }
                "com.ruhi.ACTION_READ_NOTIFICATION" -> {
                    val sender = intent.getStringExtra("sender")
                    val message = intent.getStringExtra("message")
                    speak("Boss, new message from $sender on WhatsApp. It says: $message")
                }
                "com.ruhi.ACTION_SCREEN_TEXT_RESULT" -> {
                    val text = intent.getStringExtra("text") ?: ""
                    if (text.isNotBlank()) {
                        CoroutineScope(Dispatchers.IO).launch {
                            val analysis = geminiService.chat("Analyze this screen text and tell me briefly what is on my screen: $text")
                            launch(Dispatchers.Main) {
                                speak(analysis)
                            }
                        }
                    } else {
                        speak("I couldn't read anything on the screen, boss.")
                    }
                }
            }
        }
    }

    private lateinit var tts: TextToSpeech
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var tvStatus: TextView
    private lateinit var btnListen: Button
    
    private lateinit var commandProcessor: CommandProcessor
    private lateinit var geminiService: GeminiService

    private lateinit var layoutContainer: android.widget.LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Remove standard setContentView and build programmatically
        layoutContainer = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            setBackgroundColor(android.graphics.Color.parseColor("#F5F5F5"))
        }

        tvStatus = TextView(this).apply {
            text = "Ruhi AI Assistant"
            textSize = 20f
            setPadding(0, 0, 0, 32)
            setTextColor(android.graphics.Color.BLACK)
        }
        layoutContainer.addView(tvStatus)

        btnListen = Button(this).apply {
            text = "Tap to Listen"
        }
        layoutContainer.addView(btnListen)

        val sectionTitle = TextView(this).apply {
            text = "Service Toggles"
            textSize = 18f
            setPadding(0, 32, 0, 16)
            setTextColor(android.graphics.Color.DKGRAY)
        }
        layoutContainer.addView(sectionTitle)

        // Services Toggle UI
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
                
                setOnCheckedChangeListener { _, isChecked ->
                    prefs.edit().putBoolean(key, isChecked).apply()
                    if (key == "service_wakeword") {
                        val serviceIntent = Intent(this@MainActivity, WakeWordService::class.java)
                        if (isChecked) {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                startForegroundService(serviceIntent)
                            } else {
                                startService(serviceIntent)
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

        // Request Necessary Permissions
        requestPermissions()

        // Setup AI Services
        geminiService = GeminiService(apiKey = "YOUR_GEMINI_API_KEY_HERE") // Replace with actual key securely
        commandProcessor = CommandProcessor(this, ::speak, geminiService)

        // Register Wake Word and Other Services Broadcasts
        val filter = IntentFilter().apply {
            addAction("WAKE_WORD_DETECTED")
            addAction("com.ruhi.ACTION_ANNOUNCE_CALL")
            addAction("com.ruhi.ACTION_READ_NOTIFICATION")
            addAction("com.ruhi.ACTION_SCREEN_TEXT_RESULT")
        }
        ContextCompat.registerReceiver(
            this, ruhiEventsReceiver, filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        
        // Start Background Wake Word Service based on preference
        if (prefs.getBoolean("service_wakeword", true)) {
            val serviceIntent = Intent(this, WakeWordService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }

        // Initialize Text to Speech
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale("en", "IN") // Indian Accent
                tts.setPitch(0.9f)
                tts.setSpeechRate(0.95f)
            }
        }

        // Initialize Speech Recognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        
        btnListen.setOnClickListener {
            startListening()
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
        }
        val needed = permissions.filter { 
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED 
        }
        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), 1)
        }
        
        checkAccessibilityService()
    }

    private fun checkAccessibilityService() {
        if (!isAccessibilityServiceEnabled(this, RuhiAccessibilityService::class.java)) {
            // Prompt the user to enable accessibility service
            speak("Boss, to send WhatsApp messages automatically or analyze the screen, please enable Ruhi's Accessibility Service in Settings.")
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
        
        val enabledListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        val packageName = packageName
        if (enabledListeners == null || !enabledListeners.contains(packageName)) {
            speak("Boss, to read incoming WhatsApp messages, please enable Notification Access for Ruhi.")
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
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

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                tvStatus.text = "Error recognizing speech: $error"
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val command = matches[0]
                    tvStatus.text = "You: $command"
                    commandProcessor.process(command)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer.startListening(intent)
        tvStatus.text = "Listening for your command..."
    }

    private fun speak(text: String) {
        tvStatus.text = "Ruhi: $text"
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onDestroy() {
        if (::tts.isInitialized) tts.shutdown()
        if (::speechRecognizer.isInitialized) speechRecognizer.destroy()
        unregisterReceiver(ruhiEventsReceiver)
        super.onDestroy()
    }
}
