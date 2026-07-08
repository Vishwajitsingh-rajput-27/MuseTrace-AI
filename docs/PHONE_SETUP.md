# Phone Setup

This guide prepares an Android phone for MuseTrace AI.

## Required Disclaimer

"MuseTrace AI is an independent Android drawing-assistance application. It is not affiliated with, endorsed by, sponsored by, or associated with Instagram or Meta.

MuseTrace AI works only if Instagram Draw is already available on the user's account, app version, region, and device. It cannot enable, unlock, force, or add Instagram Draw.

The app does not log into Instagram, request Instagram credentials, store Instagram credentials, use private Instagram APIs, scrape Instagram, bypass security systems, bypass platform restrictions, or send messages automatically.

Users manually open Instagram, manually open a chat, manually open Draw mode, manually select colors, and explicitly start each drawing step.

MuseTrace AI only performs drawing gestures through Android AccessibilityService after user confirmation."

## Drawing Warning

"Keep Instagram Draw open until drawing is finished. If you press Back, Home, switch apps, lock your phone, or close Instagram, Instagram may delete the current drawing. MuseTrace AI can save your project workflow, but it cannot restore an Instagram canvas that Instagram cleared."

## Resume Safety

If drawing is interrupted, MuseTrace AI shows:

> Instagram Draw may have cleared your canvas. MuseTrace AI saved your project progress, but it cannot restore a drawing that Instagram deleted. For best results, restart from Layer 1.

Workflow Resume saves project progress. Canvas Resume is only possible if Instagram Draw is still open and the drawing has not been deleted.

## Requirements

- Android phone or tablet
- MuseTrace AI APK installed
- Instagram installed
- Instagram Draw already available on the account, device, region, and Instagram app version
- Network access for Gemini image generation

MuseTrace AI cannot enable, unlock, force, or add Instagram Draw.

## Install MuseTrace AI

1. Download `MuseTrace-AI.apk` from the latest release:
   <https://github.com/Vishwajitsingh-rajput-27/MuseTrace-AI/releases/latest>
2. Open the APK on the phone.
3. Allow installs from the browser or file manager if Android asks.
4. Finish installing.
5. Open **MuseTrace AI**.

## Enable AccessibilityService

MuseTrace AI uses AccessibilityService only for user-confirmed drawing gestures.

1. Open Android Settings.
2. Open **Accessibility**.
3. Find **MuseTrace AI**.
4. Enable it.
5. Return to MuseTrace AI.

The app does not read Instagram messages, log into Instagram, choose colors, or send messages.

## Enable Overlay Permission

1. Open MuseTrace AI.
2. Open **Floating Overlay**.
3. Tap **Open Android overlay settings**.
4. Enable display-over-other-apps for MuseTrace AI.
5. Return to MuseTrace AI.

## Recommended Device Settings

- Keep the phone awake during drawing.
- Disable aggressive battery optimization for MuseTrace AI if the foreground service is stopped unexpectedly.
- Keep Instagram updated.
- Use stable brightness and orientation while calibrating.
- MuseTrace AI does not automatically tap Add Space. Use Add Space manually first, then calibrate.

## First Sketch

1. Open MuseTrace AI.
2. Choose a gallery image, capture a camera image, or generate with Gemini.
3. Create a layered sketch.
4. Open the sketch preview.
5. Open Instagram manually, open the target chat manually, and open Draw mode manually.
6. Use Add Space manually if it is available.
7. Calibrate the draw area with top-left, top-right, bottom-left, and bottom-right points.
8. Start the drawing session only after Instagram Draw is already open.

