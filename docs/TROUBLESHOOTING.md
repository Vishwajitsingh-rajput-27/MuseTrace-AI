# Troubleshooting

## Required Disclaimer

"MuseTrace AI is an independent Android drawing-assistance application. It is not affiliated with, endorsed by, sponsored by, or associated with Instagram or Meta.

MuseTrace AI works only if Instagram Draw is already available on the user's account, app version, region, and device. It cannot enable, unlock, force, or add Instagram Draw.

The app does not log into Instagram, request Instagram credentials, store Instagram credentials, use private Instagram APIs, scrape Instagram, bypass security systems, bypass platform restrictions, or send messages automatically.

Users manually open Instagram, manually open a chat, manually open Draw mode, manually select colors, and explicitly start each drawing step.

MuseTrace AI only performs drawing gestures through Android AccessibilityService after user confirmation."

## Drawing Warning

"Keep Instagram Draw open until drawing is finished. If you press Back, Home, switch apps, lock your phone, or close Instagram, Instagram may delete the current drawing. MuseTrace AI can save your project workflow, but it cannot restore an Instagram canvas that Instagram cleared."

## Gemini Image Generation Fails

Check:

- A Gemini API key is saved in Settings.
- The phone has network access.
- The Gemini key is valid.
- The Gemini API is available for the account.

Remove and re-save the key if needed.

## APK Will Not Install

Check:

- The APK came from the official latest release:
  <https://github.com/Vishwajitsingh-rajput-27/MuseTrace-AI/releases/latest>
- Android allows installs from the browser or file manager.
- There is enough storage on the phone.
- An older incompatible app build is not already installed.

## AccessibilityService Is Missing

Check:

- MuseTrace AI is installed.
- Android Settings > Accessibility lists MuseTrace AI.
- The phone is not blocking accessibility services through work-profile or device-policy restrictions.

## Drawing Does Not Start

Check:

- MuseTrace AI AccessibilityService is enabled.
- A four-point calibration profile is saved.
- A drawing session is open.
- Instagram Draw is already visible.
- You manually selected the current layer color.
- You tapped **Start Instagram Drawing Session** and then **Start Overlay Session**.
- You tapped **Continue** in the floating overlay after manually selecting the shown color.

## Drawing Pauses With Resume Warning

If MuseTrace AI shows:

> Instagram Draw may have cleared your canvas. MuseTrace AI saved your project progress, but it cannot restore a drawing that Instagram deleted. For best results, restart from Layer 1.

MuseTrace AI saved workflow progress, but it cannot restore pixels that Instagram deleted from the Draw canvas.

- Choose **Restart from Layer 1** for the safest result.
- Choose **Continue from selected layer** only if Instagram Draw is still open and the drawing is still visible.
- Choose **Recalibrate** if the drawing area changed.
- Choose **Cancel session** to stop drawing while keeping project progress saved.

## Drawing Is Misaligned

Open **Calibration** and adjust:

- Top-left and top-right if the top edge is shifted or rotated.
- Bottom-left and bottom-right if the bottom edge is shifted or rotated.
- All four points if the drawing is stretched, skewed, or scaled incorrectly.

Recalibrate whenever Instagram UI layout, device orientation, or screen size changes.

MuseTrace AI does not automatically tap Add Space. Use Add Space manually first, then calibrate.

## Overlay Does Not Appear

Check:

- Display-over-other-apps permission is enabled.
- MuseTrace AI foreground service is running.
- Android battery optimization is not stopping the app.
- The overlay was started from MuseTrace AI.

## Instagram Draw Is Missing

MuseTrace AI cannot add or unlock Instagram Draw. Try:

- Updating Instagram.
- Trying a different account.
- Trying a different device.
- Trying a supported region if available.

## Add Space Is Missing

MuseTrace AI cannot force Add Space to appear. Continue with the available Draw canvas and recalibrate.

