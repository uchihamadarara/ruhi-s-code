package com.example.ruhiassistant

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Make sure to add this dependency in your app/build.gradle:
 * implementation("com.google.ai.client.generativeai:generativeai:0.2.2")
 * 
 * Or handle raw REST HTTP requests if you prefer OkHttp/Retrofit.
 */
import com.google.ai.client.generativeai.GenerativeModel

class GeminiService(private val apiKey: String) {

    private val generativeModel = GenerativeModel(
        modelName = "gemini-3.1-flash", 
        apiKey = apiKey,
        systemInstruction = com.google.ai.client.generativeai.type.content { 
            text("You are Ruhi, an intelligent, slightly sassy Indian female AI voice assistant. Your master uses an OPPO phone. You use native Android features: SpeechRecognizer, AccessibilityService for WhatsApp automation and Screen Analysis, NotificationListenerService for reading incoming WhatsApp messages, and Call Answering/Rejecting. Your brain is connected to the cloud via the Gemini API. You can also view, sort, and manage Photos natively via the gallery. You keep your responses brief, helpful, and natural for voice conversation.") 
        }
    )

    fun generateResponse(prompt: String, onSuccess: (String) -> Unit, onError: (Exception) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = generativeModel.generateContent(prompt)
                val responseText = response.text?.trim() ?: "Sorry boss, no answer from the cloud."
                withContext(Dispatchers.Main) {
                    onSuccess(responseText)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e)
                }
            }
        }
    }
}
