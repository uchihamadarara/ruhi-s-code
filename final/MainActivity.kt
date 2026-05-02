package com.example.ruhiassistant

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private val wakeWordReceiver = object : BroadcastReceiver() { override fun onReceive(context: Context?, intent: Intent?) { if (intent?.action == "WAKE_WORD_DETECTED") { speak("Haan boss"); startListening() } } }

    private lateinit var tts: TextToSpeech
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var tvStatus: TextView
    private lateinit var btnListen: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) { ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1) }
        ContextCompat.registerReceiver(this, wakeWordReceiver, IntentFilter("WAKE_WORD_DETECTED"), ContextCompat.RECEIVER_NOT_EXPORTED)
        startService(Intent(this, WakeWordService::class.java))

        tvStatus = findViewById(R.id.tvStatus) // Requires placing a TextView with this ID in XML
        btnListen = findViewById(R.id.btnListen) // Requires placing a Button with this ID in XML

        // Initialize Text to Speech
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale("en", "IN") // Indian English
                // Set calm voice settings (pitch etc) here
                tts.setPitch(0.9f)
                tts.setSpeechRate(0.9f)
            }
        }

        // Initialize Speech Recognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        
        btnListen.setOnClickListener {
            startListening()
        }
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                tvStatus.text = "Error: $error"
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val command = matches[0]
                    tvStatus.text = "You said: $command"
                    processCommand(command)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer.startListening(intent)
        tvStatus.text = "Listening..."
    }

    private fun processCommand(command: String) {
        val lowerCmd = command.lowercase()
        
        if (lowerCmd.contains("whatsapp") && lowerCmd.contains("message")) {
            speak("Okay boss, sending message...")
            val intent = Intent("com.ruhi.ACTION_SEND_WHATSAPP"); intent.putExtra("contact", "Dinesh"); intent.putExtra("message", "Hello"); sendBroadcast(intent);
        } else {
            // Integrate Gemini AI Call Here via Network Request
            speak("Yes boss.") 
        }
    }

    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onDestroy() {
        if (::tts.isInitialized) tts.shutdown()
        if (::speechRecognizer.isInitialized) speechRecognizer.destroy()
        unregisterReceiver(wakeWordReceiver)
        super.onDestroy()
    }
}
