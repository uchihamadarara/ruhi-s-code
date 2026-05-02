# Ruhi AI Assistant - Native Android Setup

So you want to build this all natively inside Android Studio instead of a Web App? Perfect. Here is how you wire up all the files inside the `android_setup` directory.

## 1. Create a New Project in Android Studio
1. Open Android Studio -> **New Project** -> **Empty Views Activity** (or Empty Compose Activity if you want Jetpack Compose, but these files use standard Views for simplicity).
2. Name it "Ruhi Assistant".
3. Language: **Kotlin**.

## 2. Copy the Files
1. Copy the contents of `build_gradle_dependencies.txt` into your `app/build.gradle` (or `app/build.gradle.kts`) inside the `dependencies { ... }` block. Click **Sync Now**.
2. Replace your `app/src/main/AndroidManifest.xml` with the provided `AndroidManifest.xml`.
3. Copy all the `.kt` files into your package directory `app/src/main/java/com/yourname/ruhiassistant/`:
   - `MainActivity.kt`
   - `CommandProcessor.kt`
   - `MemoryManager.kt`
   - `GeminiService.kt`
   - `WakeWordService.kt`
   - `RuhiAccessibilityService.kt`

## 3. UI Setup
The UI is built completely programmatically in `MainActivity.kt`. You DO NOT need an `activity_main.xml` layout file!

## 4. API Keys and Setup
1. **Gemini API:** Inside `MainActivity.kt`, look for `geminiService = GeminiService(apiKey = "YOUR_GEMINI_API_KEY_HERE")` and replace it with your Google AI Studio API Key.
2. **Wake Word:** We are using Android's native `SpeechRecognizer` in a lightweight background Service instead of external libraries.

## What happens now?
- **Programmatic UI:** The UI is completely programmatic. No XML layout file is needed for the main activity!
- **Service Toggles:** You can turn on/off the Wake Word, Screen Parser, and Call Announcer directly from the simple switches in the app.
- **Android APIs:** `CommandProcessor.kt` uses real native Android APIs like `Intent.ACTION_CALL` to dial numbers, `AlarmClock.ACTION_SET_ALARM` to set alarms, `CameraManager` for the Flashlight, and `MediaStore` for Photo Gallery interactions.
- **Storage:** `MemoryManager.kt` uses Android's `SharedPreferences` to natively save contacts and memories offline.
- **Speech:** `MainActivity.kt` uses Android's native `SpeechRecognizer` (Speech to Text) and `TextToSpeech` engines, bypassing the website fully.
