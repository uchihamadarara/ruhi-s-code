# User Preferences / Memory

- **Target Platform**: The user is building a **Native Android Application** in Android Studio, NOT a web application.
- **Application Type**: Advanced AI Voice Assistant (like Ruhi).
- **Features & Architecture**: 
  - All future feature suggestions, code implementations, and UX/UI designs must be tailored for a **native Android app** (using Kotlin, Android SDKs, Permissions, Background Services, Foreground Services, Intents, etc.).
  - Web-specific concepts (like LocalStorage, browser CORS, iframe restrictions, web media playback APIs) should be replaced with native Android equivalents (SharedPreferences/Room Database, Android AudioManager, ExoPlayer, Intent.ACTION_CALL, etc.).
  - The AI should interact with the device natively. For example: Making direct phone calls using `Intent.ACTION_CALL`, sending WhatsApp messages natively, opening other applications natively via `PackageManager`.
- **Voice Engine**: Leverage Android's `SpeechRecognizer`, `TextToSpeech` (TTS), or external native SDKs (like Google Cloud STT/TTS, Porcupine for wake-word) instead of Web Speech API. 
- **Agent Behavior**: From now on, when providing code or architecture, default to Kotlin and Android Studio project structure unless asked otherwise.
