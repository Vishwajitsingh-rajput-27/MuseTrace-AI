# Contributing To MuseTrace AI

Thanks for helping improve MuseTrace AI.

## Ground Rules

- Keep the app Android-only.
- Keep the package name `com.vishwajitrajput.musetraceai`.
- Use Kotlin, Material 3, MVVM, clean architecture, Hilt, Coroutines, StateFlow, Navigation, ViewBinding, Room, DataStore, OpenCV Android, AccessibilityService, foreground services, overlay support, and encrypted key storage.
- Do not add Instagram login, credential storage, private APIs, scraping, bypass behavior, automated messaging, or automatic color selection.
- Do not commit Gemini API keys, signing keys, keystores, tokens, or secrets.
- Do not add empty screens, dead buttons, unfinished implementations, or fake results.

## Required Disclaimer

"MuseTrace AI is an independent Android drawing-assistance application. It is not affiliated with, endorsed by, sponsored by, or associated with Instagram or Meta.

MuseTrace AI works only if Instagram Draw is already available on the user's account, app version, region, and device. It cannot enable, unlock, force, or add Instagram Draw.

The app does not log into Instagram, request Instagram credentials, store Instagram credentials, use private Instagram APIs, scrape Instagram, bypass security systems, bypass platform restrictions, or send messages automatically.

Users manually open Instagram, manually open a chat, manually open Draw mode, manually select colors, and explicitly start each drawing step.

MuseTrace AI only performs drawing gestures through Android AccessibilityService after user confirmation."

## Drawing Warning

"Keep Instagram Draw open until drawing is finished. If you press Back, Home, switch apps, lock your phone, or close Instagram, Instagram may delete the current drawing. MuseTrace AI can save your project workflow, but it cannot restore an Instagram canvas that Instagram cleared."

## Local Setup

1. Install Android Studio.
2. Install JDK 17.
3. Install Android SDK 35.
4. Open the repository in Android Studio.
5. Let Gradle sync.
6. Run the app on an Android device or emulator.

## Before Opening A Pull Request

Run:

```powershell
gradle testDebugUnitTest
gradle assembleDebug
```

If a Gradle wrapper exists in your checkout, run:

```powershell
.\gradlew testDebugUnitTest
.\gradlew assembleDebug
```

## Pull Request Checklist

- The app compiles.
- Tests pass.
- No secrets are committed.
- New screens use the native `Sf*` Material 3 design system.
- Accessibility and overlay behavior remains user-confirmed.
- README or docs are updated when behavior changes.

## Security Reports

Do not open public issues for vulnerabilities or secret leaks. Follow [SECURITY.md](SECURITY.md).

