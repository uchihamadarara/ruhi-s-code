# Project Context: Ruhi Assistant

The user is building a customized native Android personal AI voice assistant named "Ruhi". This application is heavily reliant on device-level systems, custom services, and Gemini API.

## Core Persona (Ruhi)
- Intelligent, slightly sassy Indian female AI voice assistant.
- Uses `gemini-3.1-flash` as her cloud-connected brain for generating conversational text and interpreting screen text.
- Responds specifically to the user as her "Boss".

## Technical Constraints and Principles
- Built exclusively with native Android APIs (Kotlin). Do NOT use third-party offline intent/wake-word libraries like Picovoice. 
- The assistant is purely a personal tool running on the user's OPPO phone. It does not need to comply with restrictive Google Play Store policies (e.g., call answering policies, reading notifications, accessibility usage rules) as it is personal.

## Existing Native Features (Do not override or forget these)
1. **Wake-word Detection & Voice Recognition**: Uses native Android `SpeechRecognizer`.
2. **Text-To-Speech**: Speaks natively using Android TTS.
3. **WhatsApp Automation**: `RuhiAccessibilityService` sends WhatsApp messages entirely automatically.
4. **File Management**: `FileManager` can save, read, and delete short text messages on the internal file system.
5. **Hardware**: Controls flashlight (`CameraManager`), checks battery (`BatteryManager`), controls volume (`AudioManager`), sets native alarms (`AlarmClock`), and opens the camera (`MediaStore`).
6. **Incoming Call Announcer & Manager**: Uses `CallReceiver` (listening to `TelephonyManager`) and `TelecomManager` to announce "Boss, X's call is coming. Should I answer or reject?". CommandProcessor handles accepting or rejecting via voice.
7. **WhatsApp Message Reader**: Uses `RuhiNotificationService` (`NotificationListenerService`) to monitor incoming com.whatsapp notifications and broadcast them to the main activity so Ruhi can read them aloud.
8. **Screen Reader & Analyzer**: Triggered via voice ("What is on my screen?"). The `AccessibilityService` extracts text/content descriptions from the active window, sends it to `gemini-3.1-flash`, and speaks back the AI-analysis.
9. **Native Photo Gallery Manager**: Uses `MediaStore.Images` via `PhotoManager`. Can announce total photo count, launch the photo gallery UI, and delete the most recently captured photo.
10. **Wi-Fi & Bluetooth**: Skipped intentionally by user request. 

*Memory Updated: All recent functionalities including Gallery, Call Management, Screen Parsing, and Notification Listeners have been successfully logged and configured for current and future context.*
