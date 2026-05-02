package com.example.ruhi1

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.provider.AlarmClock
import android.view.KeyEvent
import android.annotation.SuppressLint

class CommandProcessor(
    private val context: Context, 
    private val speakAction: (String, Boolean) -> Unit,
    private val geminiService: GeminiService
) {

    private fun speak(text: String, listenAfter: Boolean = false) {
        speakAction(text, listenAfter)
    }

    private val memoryManager = MemoryManager(context)
    private val fileManager = FileManager(context)
    private val photoManager = PhotoManager(context)

    @SuppressLint("MissingPermission")
    fun process(command: String) {
        val lowerCmd = command.lowercase().trim()

        // --- 0. SCREEN ANALYSIS ---
        if (lowerCmd == "what is on my screen" || lowerCmd == "analyze screen" || lowerCmd == "screen per kya hai") {
            speak("Let me check what's on your screen, boss.")
            val intent = Intent("com.ruhi.ACTION_ANALYZE_SCREEN").apply {
                setPackage(context.packageName)
            }
            context.sendBroadcast(intent)
            return
        }

        // --- 0.1 CALL MANAGEMENT ---
        if (lowerCmd == "accept call" || lowerCmd == "answer call" || lowerCmd == "pick up" || lowerCmd == "phone uthao") {
            try {
                val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
                telecomManager.acceptRingingCall()
                speak("Call accepted, boss.")
            } catch (e: Exception) {
                speak("I couldn't accept the call. Make sure I have phone permissions.")
            }
            return
        }

        if (lowerCmd == "reject call" || lowerCmd == "cut call" || lowerCmd == "decline call" || lowerCmd == "phone kaat do") {
            try {
                val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    telecomManager.endCall()
                } else {
                    // Fallback for older versions if needed
                }
                speak("Call ended, boss.")
            } catch (e: Exception) {
                speak("I couldn't reject the call. Make sure I have phone permissions.")
            }
            return
        }

        // --- 1. MEMORY FEATURE ---
        if (lowerCmd.startsWith("remember that ")) {
            val fact = lowerCmd.substringAfter("remember that ").trim()
            memoryManager.saveFact(fact)
            speak("Got it boss, I'll remember this.")
            return
        }
        if (lowerCmd.contains("what did i tell you to remember") || lowerCmd.contains("my notes")) {
            val facts = memoryManager.getFacts()
            if (facts.isEmpty()) {
                speak("You haven't told me to remember anything yet, boss.")
            } else {
                speak("Here is what you asked me to remember: " + facts.joinToString(". "))
            }
            return
        }
        
        if (lowerCmd.startsWith("save number for ") || lowerCmd.startsWith("save contact ") || lowerCmd.startsWith("save nickname ")) {
            // Very naive string splitting "save number for [Name/Nickname] as [Number]"
            try {
                val intentWord = if (lowerCmd.startsWith("save number for ")) "for " else (if (lowerCmd.startsWith("save contact ")) "contact " else "nickname ")
                val part = lowerCmd.substringAfter(intentWord)
                val name = part.substringBefore(" as ").trim()
                val number = part.substringAfter(" as ").trim()
                memoryManager.saveContact(name, number)
                speak("Number saved for $name, boss.")
            } catch (e: Exception) {
                speak("Tell me like: save nickname brother as 9876543210")
            }
            return
        }

        // --- 1B. NATIVE FILE STORAGE ---
        val saveFileMatch = Regex("^(?:save|create)\\s+file\\s+(.+?)\\s+(?:saying|with\\s+content)\\s+(.+)$").find(lowerCmd)
        if (saveFileMatch != null) {
            val filename = saveFileMatch.groupValues[1].replace(" ", "_").trim() + ".txt"
            val content = saveFileMatch.groupValues[2].trim()
            val success = fileManager.saveFile(filename, content)
            if (success) {
                speak("Saved file $filename successfully, boss.")
            } else {
                speak("Failed to save the file, boss.")
            }
            return
        }

        val readFileMatch = Regex("^(?:read|open)\\s+file\\s+(.+)$").find(lowerCmd)
        if (readFileMatch != null) {
            val filename = readFileMatch.groupValues[1].replace(" ", "_").trim() + ".txt"
            val content = fileManager.readFile(filename)
            if (content != null) {
                speak("The file $filename says: $content")
            } else {
                speak("I couldn't find a file named $filename, boss.")
            }
            return
        }

        val deleteFileMatch = Regex("^(?:delete|remove)\\s+file\\s+(.+)$").find(lowerCmd)
        if (deleteFileMatch != null) {
            val filename = deleteFileMatch.groupValues[1].replace(" ", "_").trim() + ".txt"
            val success = fileManager.deleteFile(filename)
            if (success) {
                speak("Deleted file $filename, boss.")
            } else {
                speak("Could not delete $filename. Maybe it doesn't exist.")
            }
            return
        }
        
        if (lowerCmd == "list my files" || lowerCmd == "what files do i have") {
            val files = fileManager.listFiles()
            if (files.isEmpty()) {
                speak("You don't have any saved files, boss.")
            } else {
                val readableFiles = files.joinToString(", ") { it.replace(".txt", "").replace("_", " ") }
                speak("You have the following files: $readableFiles")
            }
            return
        }

        // --- 1C. NATIVE PHOTO GALLERY ---
        if (lowerCmd == "open gallery" || lowerCmd == "show my photos" || lowerCmd == "view my photos" || lowerCmd == "open photos" || lowerCmd == "sort my photos" || lowerCmd == "manage my photos") {
            speak("Opening your photo gallery, boss.")
            try {
                photoManager.openGallery()
            } catch (e: Exception) {
                speak("I couldn't open the gallery. You might need to give me storage permissions.")
            }
            return
        }

        if (lowerCmd == "how many photos do i have" || lowerCmd == "count my photos") {
            val count = photoManager.getPhotoCount()
            speak("You have $count photos on your device, boss.")
            return
        }

        if (lowerCmd == "delete my last photo" || lowerCmd == "delete latest photo") {
            try {
                val success = photoManager.deleteLatestPhoto()
                if (success) {
                    speak("Deleted your latest photo, boss.")
                } else {
                    speak("I couldn't delete the photo. It might require manual permission.")
                }
            } catch (e: Exception) {
                speak("I don't have permission to modify your photos.")
            }
            return
        }

        // --- 2. HARDWARE: FLASHLIGHT CAONTROL ---
        if (lowerCmd.contains("turn on flashlight") || lowerCmd.contains("torch on")) {
            toggleFlashlight(true)
            speak("Flashlight is on, boss.")
            return
        }
        if (lowerCmd.contains("turn off flashlight") || lowerCmd.contains("torch off")) {
            toggleFlashlight(false)
            speak("Flashlight turned off.")
            return
        }

        // --- 2B. BATTERY STATUS ---
        if (lowerCmd.contains("battery") || lowerCmd.contains("kitni charge hai")) {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
            val batteryLevel = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
            speak("Your phone is currently at $batteryLevel percent battery, boss.")
            return
        }

        // --- 2C. VOLUME CONTROL ---
        if (lowerCmd.contains("volume up") || lowerCmd.contains("increase volume") || lowerCmd.contains("awaaz badhao")) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
            speak("Volume increased, boss.")
            return
        }
        if (lowerCmd.contains("volume down") || lowerCmd.contains("decrease volume") || lowerCmd.contains("awaaz kam karo")) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
            speak("Volume decreased.")
            return
        }
        if (lowerCmd == "mute phone") {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
            speak("Muting the phone, boss.")
            return
        }

        // --- 2D. CAMERA CONTROL ---
        if (lowerCmd.contains("open camera") || lowerCmd.contains("take a photo") || lowerCmd.contains("camera kholo")) {
            speak("Opening camera, boss.")
            try {
                val intent = Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                speak("I couldn't open the camera, boss.")
            }
            return
        }

        // --- 3. ALARM / TIMER ---
        val alarmMatch = Regex(".*?alarm.*?(\\d+).*").find(lowerCmd)
        if (alarmMatch != null) {
            val hrStr = alarmMatch.groupValues[1]
            try {
                val hour = hrStr.toInt()
                val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_MESSAGE, "Ruhi Alarm")
                    putExtra(AlarmClock.EXTRA_HOUR, hour)
                    putExtra(AlarmClock.EXTRA_MINUTES, 0)
                    putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                speak("Alarm set for $hour o'clock, boss.")
            } catch (e: Exception) {
                speak("I need permission to set alarms, boss.")
            }
            return
        }

        // --- 4. PHONE CALLING ---
        val callMatch = Regex("^(?:call|dial|phone)\\s+(.+)$|(.+?)\\s+(?:ko\\s+)?(?:call|phone)\\s*(?:karo|lagao|kar|laga)").find(lowerCmd)
        if (callMatch != null) {
            val target = (callMatch.groupValues[1].takeIf { it.isNotEmpty() } ?: callMatch.groupValues[2]).trim()
            
            val isNumber = target.matches(Regex("^[0-9+\\s]+$"))
            var numberToDial = if (isNumber) target.replace(" ", "") else memoryManager.getContactNumber(target)
            
            // Fallback to Native Contacts if not found in MemoryManager
            if (numberToDial == null && !isNumber) {
                try {
                    val uri = android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                    val projection = arrayOf(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val selection = "${android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
                    val selectionArgs = arrayOf("%$target%")
                    val cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
                    cursor?.use {
                        if (it.moveToFirst()) {
                            numberToDial = it.getString(0).replace(" ", "")
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (numberToDial != null) {
                speak("Calling $target, boss.")
                try {
                    val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$numberToDial")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(callIntent)
                } catch (e: SecurityException) {
                    speak("I need permission to make phone calls, boss.")
                }
            } else {
                speak("Sorry boss, I couldn't find a number for $target in my memory. Tell me their number first.")
            }
            return
        }
        
        // --- 5. AUTOMATIC WHATSAPP MESSAGING ---
        val whatsappMatch = Regex("^(?:send\\s+a?\\s*whatsapp|whatsapp)\\s+(?:message\\s+)?(?:to\\s+)?(.+?)\\s+(?:saying|that|message)\\s+(.+)$|(.+?)\\s+ko\\s+whatsapp\\s+(?:pe\\s+)?(?:message\\s+)?(?:karo|bhejo)\\s*(?:saying|ki)\\s*(.+)$").find(lowerCmd)
        if (whatsappMatch != null) {
            val target = (whatsappMatch.groupValues[1].takeIf { it.isNotEmpty() } ?: whatsappMatch.groupValues[3]).trim()
            val message = (whatsappMatch.groupValues[2].takeIf { it.isNotEmpty() } ?: whatsappMatch.groupValues[4]).trim()
            
            var number = memoryManager.getContactNumber(target)
            
            // Fallback to Native Contacts if not found in MemoryManager
            if (number == null) {
                try {
                    val uri = android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                    val projection = arrayOf(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val selection = "${android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
                    val selectionArgs = arrayOf("%$target%")
                    val cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
                    cursor?.use {
                        if (it.moveToFirst()) {
                            number = it.getString(0).replace(" ", "")
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (number != null) {
                speak("Sending WhatsApp message to $target, boss.")
                val intent = Intent("com.ruhi.ACTION_SEND_WHATSAPP").apply {
                    putExtra("contact", target)
                    putExtra("number", number)
                    putExtra("message", message)
                    // Important for explicit broadcast receivers in newer Android
                    setPackage(context.packageName)
                }
                context.sendBroadcast(intent)
            } else {
                speak("Sorry boss, I couldn't find a number for $target in my memory.")
            }
            return
        }

        // --- 5A. PLAYING MEDIA ---
        if (lowerCmd.startsWith("play ") && lowerCmd.contains(" on youtube")) {
            val query = lowerCmd.replace("play ", "").replace(" on youtube", "").trim()
            speak("Playing $query on YouTube, boss")
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$query")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return
        }

        if (lowerCmd.startsWith("search ") && lowerCmd.contains(" on spotify")) {
            val query = lowerCmd.replace("search ", "").replace(" on spotify", "").trim()
            speak("Searching $query on Spotify, boss")
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com/search/$query")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return
        }

        // --- 5B. OPENING APPS ---
        val openMatch = Regex("^(?:open|start|launch)\\s+(.+)$|(.+?)\\s+(?:open|chalu|shuru)\\s*(?:karo|kar|karo\\s*na)?$").find(lowerCmd)
        if (openMatch != null) {
            val appName = (openMatch.groupValues[1].takeIf { it.isNotEmpty() } ?: openMatch.groupValues[2]).trim()
            speak("Let me see if $appName is installed, boss.")
            
            val packageManager = context.packageManager
            val packages = packageManager.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)
            
            var foundPackage: String? = null
            for (packageInfo in packages) {
                val label = packageManager.getApplicationLabel(packageInfo).toString().lowercase()
                if (label.contains(appName)) {
                    foundPackage = packageInfo.packageName
                    break
                }
            }

            if (foundPackage != null) {
                val intent = packageManager.getLaunchIntentForPackage(foundPackage)
                if (intent != null) {
                    speak("Opening $appName.")
                    context.startActivity(intent)
                } else {
                    speak("Sorry boss, I can't open the main page of $appName.")
                }
            } else {
                speak("It seems $appName is not installed on your phone, boss. Searching it online.")
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=$appName&c=apps")).apply {
                   addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
            return
        }

        // --- 6. AI CLOUD FALLBACK ---
        // Integrate your Gemini API call here.
        geminiService.generateResponse(command, 
            onSuccess = { response ->
                speak(response, true) // Listen again after Gemini responds!
            },
            onError = { error ->
                speak("Sorry boss, my internet brain is offline right now.")
                error.printStackTrace()
            }
        )
    }

    private fun toggleFlashlight(status: Boolean) {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList[0]
            cameraManager.setTorchMode(cameraId, status)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
