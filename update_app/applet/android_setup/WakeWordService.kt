package com.example.ruhiassistant

import ai.picovoice.porcupine.PorcupineManager
import ai.picovoice.porcupine.PorcupineManagerCallback
import ai.picovoice.porcupine.PorcupineException
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class WakeWordService : Service() {
    private var porcupineManager: PorcupineManager? = null
    
    // You need to get a free Access Key from https://console.picovoice.ai/
    private val ACCESS_KEY = "YOUR_PICOVOICE_ACCESS_KEY_HERE" 

    override fun onCreate() {
        super.onCreate()
        initPorcupine()
    }

    private fun initPorcupine() {
        try {
            // Callback triggered when "Hello Ruhi" is detected
            val callback = PorcupineManagerCallback { keywordIndex ->
                Log.d("WakeWord", "Wake word 'Hello Ruhi' detected!")
                
                // Send broadcast to MainActivity to wake up and start listening
                val intent = Intent("WAKE_WORD_DETECTED")
                sendBroadcast(intent)
            }

            // Note: You must train the "Hello Ruhi" wake word in the Picovoice Console,
            // download the generated .ppn file, and place it in your Android app's assets/ folder.
            porcupineManager = PorcupineManager.Builder()
                .setAccessKey(ACCESS_KEY)
                .setKeywordPath("hello_ruhi_android.ppn") // The custom model file
                .setSensitivity(0.7f)
                .build(applicationContext, callback)

            porcupineManager?.start()
            Log.d("WakeWord", "Porcupine started listening in the background...")

        } catch (e: PorcupineException) {
            Log.e("WakeWord", "Failed to initialize Porcupine: ${e.message}")
        }
    }

    override fun onDestroy() {
        try {
            porcupineManager?.stop()
            porcupineManager?.delete()
        } catch (e: PorcupineException) {
            Log.e("WakeWord", "Failed to stop Porcupine: ${e.message}")
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
