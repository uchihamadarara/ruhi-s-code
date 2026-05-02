# Implementing "Hello Ruhi" Wake Word on Android

To implement a robust, offline, and battery-efficient wake word like Jarvis, you cannot use the standard Android `SpeechRecognizer` in an infinite loop (it drains battery and gets killed by the OS). 

The industry standard for custom wake words is **Picovoice Porcupine**.

## Step-by-Step Implementation

### 1. Add the Dependency
Open your `app/build.gradle.kts` (or `build.gradle`) in Android Studio and add:
```gradle
implementation("ai.picovoice:porcupine-android:3.0.1")
```

### 2. Train the "Hello Ruhi" Model
1. Go to [Picovoice Console](https://console.picovoice.ai/).
2. Create a free account.
3. Go to **Porcupine Wake Word** -> **Custom Wake Word**.
4. Type **"Hello Ruhi"** and select **Android** as the platform.
5. Train and download the `.ppn` model file.
6. In Android Studio, go to `app/src/main/` and create an `assets` folder (if it doesn't exist).
7. Paste the `.ppn` file into the `assets` folder. Rename it to `hello_ruhi_android.ppn`.

### 3. Get Your Access Key
From the Picovoice Console dashboard, copy your free **Access Key**. Open `WakeWordService.kt` and paste it in the `ACCESS_KEY` variable.

### 4. Create the Service
I have created `WakeWordService.kt` for you. Copy it into your project.

### 5. Add Service to AndroidManifest.xml
Inside the `<application>` tag, add the service declaration:
```xml
<service android:name=".WakeWordService" android:exported="false" />
```

### 6. Connect Wake Word to MainActivity
In your `MainActivity.kt`, you need a `BroadcastReceiver` that listens for the wake word trigger.

Add this inside `MainActivity.kt`:

```kotlin
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

// Inside MainActivity class:
private val wakeWordReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "WAKE_WORD_DETECTED") {
            // Play a small beep sound to acknowledge
            speak("Yes boss?")
            
            // Start listening for the actual command
            startListening()
        }
    }
}

override fun onResume() {
    super.onResume()
    // Register the receiver
    registerReceiver(wakeWordReceiver, IntentFilter("WAKE_WORD_DETECTED"))
    
    // Start the background Wake Word service
    startService(Intent(this, WakeWordService::class.java))
}

override fun onPause() {
    super.onPause()
    unregisterReceiver(wakeWordReceiver)
}
```

### Why Picovoice?
- It runs 100% offline (no internet required to detect "Hello Ruhi").
- Battery efficient (can run in the background).
- Extremely fast and accurate for custom words.
