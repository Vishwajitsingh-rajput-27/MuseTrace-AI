# MuseTrace AI

**Prompt. Layer. Trace.**

MuseTrace AI is an Android-only drawing-assistance app that converts gallery, camera, and Gemini-generated images into 16/24/32-color semi-realistic layered sketches. It helps users recreate those sketches inside Instagram Draw using Android AccessibilityService gestures and a floating overlay controller.

Direct download: <https://github.com/Vishwajitsingh-rajput-27/MuseTrace-AI/releases/latest>

## What It Does

MuseTrace AI turns a source image into a layered tracing plan. The source can come from:

- Gallery image import
- Camera capture
- Gemini-generated image prompt

The app then creates color layers, preview panels, stroke paths, calibration settings, and a user-controlled drawing session for Android devices.

## Features

- Android package: `com.vishwajitrajput.musetraceai`
- Native Kotlin Android app with Material 3
- Native shadcn-inspired design system built with Android views and theme tokens
- Gemini-first AI image generation
- Secure Gemini API key storage using encrypted Android storage
- 16, 24, and 32 color sketch layering
- Gallery and camera input
- Sketch, layer, and stroke previews
- Four-point calibration profiles for the Instagram Draw drawing area
- AccessibilityService gesture drawing after explicit user confirmation
- Foreground drawing session service
- Floating overlay controller using Android overlay permission
- Local history with Room
- Settings with DataStore

## Disclaimer

"MuseTrace AI is an independent Android drawing-assistance application. It is not affiliated with, endorsed by, sponsored by, or associated with Instagram or Meta.

MuseTrace AI works only if Instagram Draw is already available on the user's account, app version, region, and device. It cannot enable, unlock, force, or add Instagram Draw.

The app does not log into Instagram, request Instagram credentials, store Instagram credentials, use private Instagram APIs, scrape Instagram, bypass security systems, bypass platform restrictions, or send messages automatically.

Users manually open Instagram, manually open a chat, manually open Draw mode, manually select colors, and explicitly start each drawing step.

MuseTrace AI only performs drawing gestures through Android AccessibilityService after user confirmation."

## Drawing Warning

"Keep Instagram Draw open until drawing is finished. If you press Back, Home, switch apps, lock your phone, or close Instagram, Instagram may delete the current drawing. MuseTrace AI can save your project workflow, but it cannot restore an Instagram canvas that Instagram cleared."

## APK Download

Download the latest APK from:

<https://github.com/Vishwajitsingh-rajput-27/MuseTrace-AI/releases/latest>

The release asset is named:

```text
MuseTrace-AI.apk
```

Quick install flow:

```text
Download MuseTrace-AI.apk -> Allow install unknown apps -> Install APK -> Enter Gemini key -> Enable Accessibility -> Enable Display Over Other Apps -> Start drawing.
```

## GitHub Pages Landing Page

This repository includes a static GitHub Pages fallback page at:

```text
docs/index.html
```

Use this option when you want a simple project landing page without deploying the Next.js website in `/website`.

To enable GitHub Pages:

1. Open the GitHub repository: `Vishwajitsingh-rajput-27/MuseTrace-AI`.
2. Go to **Settings**.
3. Open **Pages**.
4. Under **Build and deployment**, set **Source** to **Deploy from a branch**.
5. Set **Branch** to `main`.
6. Set the folder to `/docs`.
7. Click **Save**.
8. Wait for GitHub Pages to publish the site.

The static page includes the MuseTrace AI app name, tagline, features, screenshots section, setup steps, Gemini setup, Accessibility setup, overlay permission setup, Instagram Draw workflow, Add Space workflow, disclaimer, and a download button that links to:

<https://github.com/Vishwajitsingh-rajput-27/MuseTrace-AI/releases/latest>

## Install The APK

1. Open the latest release page on your Android phone.
2. Download `MuseTrace-AI.apk`.
3. Open the downloaded APK.
4. If Android blocks the install, allow installs from that browser or file manager.
5. Continue the install.
6. Open **MuseTrace AI**.

Android may show a warning for APKs installed outside Google Play. Only install APKs from the official release link above.

## Gemini API Key Setup

MuseTrace AI uses Gemini as the default AI provider.

1. Get a Gemini API key from Google AI Studio.
2. Open MuseTrace AI.
3. Go to **Settings**.
4. Paste the Gemini API key.
5. Tap **Save key securely**.

The key is stored on-device using encrypted Android storage. Do not paste the key into GitHub issues, commits, screenshots, or logs.

More details: [docs/GEMINI_SETUP.md](docs/GEMINI_SETUP.md)

## Accessibility Permission Setup

MuseTrace AI uses Android AccessibilityService only for user-confirmed drawing gestures.

1. Open Android Settings.
2. Go to **Accessibility**.
3. Find **MuseTrace AI**.
4. Enable the service.
5. Return to MuseTrace AI.

MuseTrace AI does not read Instagram messages or log in to Instagram.

More details: [docs/PHONE_SETUP.md](docs/PHONE_SETUP.md)

## Overlay Permission Setup

The floating controller needs Android's display-over-other-apps permission.

1. Open MuseTrace AI.
2. Go to **Floating Overlay**.
3. Tap **Open Android overlay settings**.
4. Enable overlay permission for MuseTrace AI.
5. Return to the app and start the overlay.

More details: [docs/FLOATING_OVERLAY_GUIDE.md](docs/FLOATING_OVERLAY_GUIDE.md)

## Instagram Draw Workflow

1. Create or open a layered sketch in MuseTrace AI.
2. Open Instagram manually.
3. Open the chat manually.
4. Open Instagram Draw mode manually.
5. Use Add Space manually if available.
6. Calibrate the final drawing area with top-left, top-right, bottom-left, and bottom-right points.
7. Return to MuseTrace AI or use the floating overlay.
8. MuseTrace AI shows the current layer color.
9. Select that color manually in Instagram Draw.
10. Tap **Continue** in the floating overlay.
11. Repeat for each layer.

More details: [docs/INSTAGRAM_DRAW_GUIDE.md](docs/INSTAGRAM_DRAW_GUIDE.md)

## Add Space Workflow

If Instagram offers Add Space, use it manually before drawing so the canvas has enough room.

MuseTrace AI does not automatically tap Add Space. Use Add Space manually first, then calibrate.

MuseTrace AI cannot enable Add Space, force it to appear, or change Instagram feature availability.

Calibration profiles are saved separately for **Normal Draw**, **Add Space Small**, **Add Space Medium**, **Add Space Maximum**, and **Custom**. Recalibrate after changing phone orientation, Instagram layout, or Add Space size.

More details: [docs/ADD_SPACE_GUIDE.md](docs/ADD_SPACE_GUIDE.md)

## Floating Overlay Workflow

The floating overlay gives drawing controls while Instagram Draw stays open, so the user does not need to press Back, Home, or switch back to MuseTrace AI between layers.

Before starting, tap **Start Instagram Drawing Session** and confirm the checklist:

1. Open Instagram manually.
2. Open the target chat manually.
3. Open Draw mode manually.
4. Use Add Space manually if needed.
5. Do not press Back.
6. Do not press Home.
7. Do not switch apps.
8. Do not lock phone.
9. Keep Instagram Draw open until finished.
10. Floating overlay will guide colors and Continue buttons.

The checklist provides **Start Overlay Session**, **Open Overlay Permission Settings**, **Open Accessibility Settings**, **Open Calibration**, and **Cancel**. Starting an overlay session starts the foreground session service, shows the floating overlay, saves the active project workflow, and prepares Color 1.

- **Continue** draws the current layer after the user manually selects the shown color.
- **Skip Layer** advances to the next layer.
- **Redraw Layer** resets the current layer so the next **Continue** can draw it again.
- **Move Up** and **Move Down** reorder colors before the first **Continue**.
- **Pause** pauses before the next gesture. **Resume** is only a Canvas Resume when Instagram Draw is still open and the drawing has not been deleted.
- **Cancel** stops drawing while keeping the overlay available.
- **Emergency Stop** stops drawing and closes the overlay.
- **Collapse/Expand** switches between compact bubble mode and full controller mode.

The overlay advances from Color 1 of 16 to Color 2 of 16 after each layer completes, autosaves workflow progress, and shows **Drawing complete** when all layers are done. The user manually sends or saves in Instagram; MuseTrace AI never sends automatically.

The overlay is draggable, opacity-adjustable, remembers its last position, and warns if it overlaps the calibrated drawing area. It does not open Instagram, choose colors, send messages, tap Add Space, or bypass Instagram limits.

If Instagram is no longer the foreground app or the gesture session is interrupted, MuseTrace AI shows:

> Instagram Draw may have cleared your canvas. MuseTrace AI saved your project progress, but it cannot restore a drawing that Instagram deleted. For best results, restart from Layer 1.

Workflow Resume saves the project, current layer, settings, calibration, strokes, and progress. Canvas Resume is only possible if Instagram Draw is still open and the drawing has not been deleted. The resume choices are **Restart from Layer 1**, **Continue from selected layer**, **Recalibrate**, and **Cancel session**.

## Build From Source

Requirements:

- Android Studio
- JDK 17
- Android SDK for compile SDK 35

Open the repository in Android Studio and run the `app` configuration.

Command-line build:

```powershell
gradle testDebugUnitTest
gradle assembleDebug
```

If you generate a Gradle wrapper locally, use:

```powershell
.\gradlew testDebugUnitTest
.\gradlew assembleDebug
```

## Limitations

- Android only.
- Instagram Draw must already be available on the user's account, device, region, and Instagram version.
- MuseTrace AI cannot enable, unlock, force, or add Instagram Draw.
- MuseTrace AI cannot automatically select Instagram colors.
- Gesture drawing quality depends on four-point calibration, screen size, device performance, and Instagram UI layout.
- Gemini image generation requires a valid Gemini API key and network access.
- Release APK signing depends on the build environment. GitHub Actions will upload the best available APK artifact.

## Troubleshooting

- Gemini generation fails: confirm the API key is saved in Settings and the phone has network access.
- Overlay does not appear: enable display-over-other-apps permission.
- Drawing does not start: enable MuseTrace AI AccessibilityService and save a four-point calibration profile.
- Drawing is offset: recalibrate top-left, top-right, bottom-left, and bottom-right for the final Instagram Draw area.
- Instagram Draw is missing: update Instagram or try another account/device/region where Draw is available.

More details: [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md)

## Security

Never commit API keys, signing keys, keystores, or secrets. See [SECURITY.md](SECURITY.md).

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

