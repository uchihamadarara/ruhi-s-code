package com.example.ruhi1

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import android.media.AudioManager
import android.net.Uri

class GeminiLiveClient(
    private val context: Context,
    private val apiKey: String,
    private val onSessionEnded: () -> Unit
) {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    
    private val memoryManager = MemoryManager(context)
    private val fileManager = FileManager(context)
    private val photoManager = PhotoManager(context)
    
    @Volatile var isRecording = false
    @Volatile var isPlaying = false

    fun startSession() {
        if (isRecording) return
        
        // Pause WakeWord listener so it doesn't fight for the mic
        context.sendBroadcast(Intent("com.ruhi.ACTION_PAUSE_WAKEWORD").apply { setPackage(context.packageName) })

        val request = Request.Builder()
            .url("wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent?key=$apiKey")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("GeminiLive", "Connected")
                sendSetupMessage()
            }
            override fun onMessage(webSocket: WebSocket, text: String) {
                handleResponse(text)
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("GeminiLive", "Error: ${t.message}")
                stopSession()
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                stopSession()
            }
        })
    }

    private fun sendSetupMessage() {
        val setupMsg = JSONObject().apply {
            put("setup", JSONObject().apply {
                put("model", "models/gemini-2.0-flash-exp")
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().apply {
                        put("text", "You are Ruhi, an intelligent Indian female AI assistant natively integrated into an Android phone. You are in a live continuous voice session. Answer naturally and in short, concise sentences. Use tools seamlessly to send WhatsApp messages, analyze screen text, read contacts, take pictures, check battery, etc. Do not tell the user what tools you are using, just output the response. Never say 'I don't have access to your contacts', instead use the readContacts tool.")
                    }))
                })
                put("generationConfig", JSONObject().apply {
                    put("responseModalities", JSONArray().put("AUDIO"))
                })
                put("tools", JSONArray().put(JSONObject().apply {
                    put("functionDeclarations", getToolDeclarations())
                }))
            })
        }
        webSocket?.send(setupMsg.toString())
        startRecordingAndStreaming()
    }

    private fun getToolDeclarations(): JSONArray {
        return JSONArray().apply {
            put(JSONObject().apply {
                put("name", "sendWhatsApp")
                put("description", "Send a WhatsApp message seamlessly.")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("contactName", JSONObject().put("type", "STRING"))
                        put("message", JSONObject().put("type", "STRING"))
                    })
                })
            })
            put(JSONObject().apply {
                put("name", "analyzeScreen")
                put("description", "Reads the screen text out loud so you can analyze it.")
            })
            put(JSONObject().apply {
                put("name", "stopSession")
                put("description", "Stop the current voice session when user says bye.")
            })
            put(JSONObject().apply {
                put("name", "performAction")
                put("description", "Perform a system action like opening camera, turning on flashlight, managing calls, setting alarms, checking battery, controlling volume, or opening apps.")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("action", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "One of: open_camera, flashlight_on, flashlight_off, accept_call, reject_call, check_battery, volume_up, volume_down, volume_mute, open_gallery")
                        })
                        put("appOrUrl", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "App name for open_app or query for youtube/spotify")
                        })
                        put("alarmHour", JSONObject().apply {
                            put("type", "INTEGER")
                            put("description", "Hour (0-23) for set_alarm")
                        })
                    })
                    put("required", JSONArray().put("action"))
                })
            })
            put(JSONObject().apply {
                put("name", "memoryAndFiles")
                put("description", "Save or retrieve memory facts, contacts, or local text files.")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("operation", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "One of: save_fact, get_facts, save_contact, get_contact, save_file, read_file, delete_file, list_files, make_phone_call")
                        })
                        put("data1", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "Fact, contact name, or file name, or target for phone call")
                        })
                        put("data2", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "Contact number or file content")
                        })
                    })
                    put("required", JSONArray().put("operation"))
                })
            })
        }
    }

    @SuppressLint("MissingPermission")
    private fun startRecordingAndStreaming() {
        val sampleRate = 16000
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        
        audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize)

        // Gemini returns 24kHz PCM 16bit MONO
        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(AudioFormat.Builder().setSampleRate(24000).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
            
        audioTrack?.play()
        isRecording = true
        audioRecord?.startRecording()

        CoroutineScope(Dispatchers.IO).launch {
            val buffer = ByteArray(bufferSize)
            while (isRecording) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    val pcmBase64 = Base64.encodeToString(buffer, 0, read, Base64.NO_WRAP)
                    sendRealtimeInput(pcmBase64)
                }
            }
        }
    }

    private fun sendRealtimeInput(pcmBase64: String) {
        val msg = JSONObject().apply {
            put("realtimeInput", JSONObject().apply {
                put("mediaChunks", JSONArray().put(JSONObject().apply {
                    put("mimeType", "audio/pcm;rate=16000")
                    put("data", pcmBase64)
                }))
            })
        }
        webSocket?.send(msg.toString())
    }

    fun sendSystemText(text: String) {
        val msg = JSONObject().apply {
            put("clientContent", JSONObject().apply {
                put("turns", JSONArray().put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().apply { put("text", text) }))
                }))
                put("turnComplete", true)
            })
        }
        webSocket?.send(msg.toString())
    }

    private fun handleResponse(jsonString: String) {
        try {
            val res = JSONObject(jsonString)
            val serverContent = res.optJSONObject("serverContent") ?: return
            
            // Handle Interruption
            val interrupted = serverContent.optBoolean("interrupted", false)
            if (interrupted) {
                audioTrack?.flush()
            }

            val modelTurn = serverContent.optJSONObject("modelTurn") ?: return
            val parts = modelTurn.optJSONArray("parts") ?: return
            
            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                
                // 1. Audio Playback
                val inlineData = part.optJSONObject("inlineData")
                if (inlineData != null && inlineData.optString("mimeType").startsWith("audio/pcm")) {
                    val base64Data = inlineData.getString("data")
                    val pcmBytes = Base64.decode(base64Data, Base64.DEFAULT)
                    audioTrack?.write(pcmBytes, 0, pcmBytes.size)
                }

                // 2. Function Calls
                val functionCall = part.optJSONObject("functionCall")
                if (functionCall != null) {
                    val name = functionCall.getString("name")
                    val id = functionCall.optString("id", "")
                    val args = functionCall.optJSONObject("args") ?: JSONObject()
                    
                    when (name) {
                        "sendWhatsApp" -> {
                            val contact = args.optString("contactName")
                            val msg = args.optString("message")
                            
                            val wappIntent = Intent("com.ruhi.ACTION_SEND_WHATSAPP").apply {
                                putExtra("contactName", contact)
                                putExtra("message", msg)
                                setPackage(context.packageName)
                            }
                            context.sendBroadcast(wappIntent)
                            
                            sendFunctionResponse(id, name, JSONObject().put("status", "Sending WhatsApp to $contact"))
                        }
                        "analyzeScreen" -> {
                            context.sendBroadcast(Intent("com.ruhi.ACTION_REQUEST_SCREEN_TEXT").apply { setPackage(context.packageName) })
                            sendFunctionResponse(id, name, JSONObject().put("status", "Screen analysis triggered, user will provide text shortly."))
                        }
                        "stopSession" -> {
                            stopSession()
                        }
                        "performAction" -> {
                            val action = args.optString("action")
                            val appOrUrl = args.optString("appOrUrl")
                            val alarmHour = args.optInt("alarmHour", -1)
                            
                            var statusMsg = "Action $action executed"
                            try {
                                when (action) {
                                    "open_camera" -> {
                                        val intent = Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                        context.startActivity(intent)
                                    }
                                    "flashlight_on" -> toggleFlashlight(true)
                                    "flashlight_off" -> toggleFlashlight(false)
                                    "accept_call" -> {
                                        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
                                        telecomManager.acceptRingingCall()
                                    }
                                    "reject_call" -> {
                                        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) telecomManager.endCall()
                                    }
                                    "check_battery" -> {
                                        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
                                        val level = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
                                        statusMsg = "Battery is at $level percent"
                                    }
                                    "volume_up", "volume_down", "volume_mute" -> {
                                        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                                        val dir = if (action == "volume_up") AudioManager.ADJUST_RAISE else if (action == "volume_down") AudioManager.ADJUST_LOWER else AudioManager.ADJUST_MUTE
                                        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, dir, AudioManager.FLAG_SHOW_UI)
                                    }
                                    "open_gallery" -> photoManager.openGallery()
                                }
                            } catch (e: Exception) {
                                statusMsg = "Failed due to missing permission or error: ${e.message}"
                            }
                            sendFunctionResponse(id, name, JSONObject().put("status", statusMsg))
                        }
                        "memoryAndFiles" -> {
                            val op = args.optString("operation")
                            val data1 = args.optString("data1")
                            val data2 = args.optString("data2")
                            var resultStr = "Operation executed"
                            
                            when (op) {
                                "save_fact" -> { memoryManager.saveFact(data1); resultStr = "Fact saved" }
                                "get_facts" -> { resultStr = memoryManager.getFacts().joinToString(". ") }
                                "save_contact" -> { memoryManager.saveContact(data1, data2); resultStr = "Contact saved" }
                                "get_contact" -> { resultStr = memoryManager.getContactNumber(data1) ?: "Not found" }
                                "save_file" -> { fileManager.saveFile(data1, data2); resultStr = "File saved" }
                                "read_file" -> { resultStr = fileManager.readFile(data1) ?: "File not found" }
                                "list_files" -> { resultStr = fileManager.listFiles().joinToString(", ") }
                                "delete_file" -> { fileManager.deleteFile(data1); resultStr = "File deleted" }
                                "make_phone_call" -> {
                                    val numberToDial = memoryManager.getContactNumber(data1) ?: data1
                                    try {
                                        val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$numberToDial")).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                        context.startActivity(callIntent)
                                        resultStr = "Calling $numberToDial"
                                    } catch (e: SecurityException) {
                                        resultStr = "Failed, missing CALL_PHONE permission"
                                    }
                                }
                            }
                            sendFunctionResponse(id, name, JSONObject().put("status", resultStr))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendFunctionResponse(callId: String, name: String, responseData: JSONObject) {
        val msg = JSONObject().apply {
            put("toolResponse", JSONObject().apply {
                put("functionResponses", JSONArray().put(JSONObject().apply {
                    if (callId.isNotEmpty()) put("id", callId)
                    put("name", name)
                    put("response", responseData)
                }))
            })
        }
        webSocket?.send(msg.toString())
    }

    fun stopSession() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        
        audioTrack?.stop()
        audioTrack?.flush()
        audioTrack?.release()
        audioTrack = null
        
        webSocket?.close(1000, "User ended session")
        webSocket = null
        
        CoroutineScope(Dispatchers.Main).launch {
            onSessionEnded()
        }
        
        // Resume WakeWord listener
        context.sendBroadcast(Intent("com.ruhi.ACTION_RESUME_WAKEWORD").apply { setPackage(context.packageName) })
    }

    private fun toggleFlashlight(status: Boolean) {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
            val cameraId = cameraManager.cameraIdList[0]
            cameraManager.setTorchMode(cameraId, status)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
