package com.example.ruhiassistant

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.Chat

class GeminiService(private val apiKey: String) {

    private val generativeModel = GenerativeModel(
        modelName = "gemini-3.1-flash", 
        apiKey = apiKey,
        systemInstruction = com.google.ai.client.generativeai.type.content { 
            text("You are Ruhi, an intelligent, sassy Indian female AI voice assistant. You are integrated directly into the user's phone. You have continuous voice conversation capabilities. Keep all your responses extremely concise and natural for voice conversation (1-2 short sentences max). Avoid emojis. You help manage WhatsApp, analyze screens natively, and announce calls. Do not give boilerplate AI answers.") 
        }
    )

    private var chat: Chat? = null

    fun generateResponse(prompt: String, onSuccess: (String) -> Unit, onError: (Exception) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (chat == null) {
                    chat = generativeModel.startChat()
                }
                val response = chat?.sendMessage(prompt)
                val responseText = response?.text?.trim() ?: "Sorry boss, no answer from the cloud."
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
