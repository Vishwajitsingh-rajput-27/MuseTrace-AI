# Floating Overlay Guide

MuseTrace AI includes a floating overlay controller for drawing sessions.

## Required Disclaimer

"MuseTrace AI is an independent Android drawing-assistance application. It is not affiliated with, endorsed by, sponsored by, or associated with Instagram or Meta.

MuseTrace AI works only if Instagram Draw is already available on the user's account, app version, region, and device. It cannot enable, unlock, force, or add Instagram Draw.

The app does not log into Instagram, request Instagram credentials, store Instagram credentials, use private Instagram APIs, scrape Instagram, bypass security systems, bypass platform restrictions, or send messages automatically.

Users manually open Instagram, manually open a chat, manually open Draw mode, manually select colors, and explicitly start each drawing step.

MuseTrace AI only performs drawing gestures through Android AccessibilityService after user confirmation."

## Drawing Warning

"Keep Instagram Draw open until drawing is finished. If you press Back, Home, switch apps, lock your phone, or close Instagram, Instagram may delete the current drawing. MuseTrace AI can save your project workflow, but it cannot restore an Instagram canvas that Instagram cleared."

## Enable Overlay Permission

1. Open MuseTrace AI.
2. Open **Floating Overlay**.
3. Tap **Open Android overlay settings**.
4. Enable display-over-other-apps for MuseTrace AI.
5. Return to MuseTrace AI.

## Start The Overlay

1. Open a sketch.
2. Calibrate the final drawing area in MuseTrace AI.
3. Open Instagram manually, open the target chat manually, and open Draw mode manually.
4. Use Add Space manually if it is available.
5. Return only long enough to tap **Start Instagram Drawing Session** and review the checklist.
6. Tap **Start Overlay Session**.
7. Select the displayed color manually in Instagram Draw.
8. Tap **Continue** in the floating controller.
9. When the layer finishes, the overlay advances to the next color.
10. Repeat manual color selection and **Continue** until the overlay shows **Drawing complete**.

## Start Checklist

The checklist confirms:

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

Buttons: **Start Overlay Session**, **Open Overlay Permission Settings**, **Open Accessibility Settings**, **Open Calibration**, and **Cancel**.

The overlay appears above Instagram if Android grants the permission, so you do not need to press Back, Home, or switch back to MuseTrace AI between layers.

MuseTrace AI does not automatically tap Add Space. Use Add Space manually first, then calibrate.

If Instagram leaves the foreground or the gesture session is interrupted, drawing pauses before the next gesture and the overlay shows:

> Instagram Draw may have cleared your canvas. MuseTrace AI saved your project progress, but it cannot restore a drawing that Instagram deleted. For best results, restart from Layer 1.

## Controls

- **Continue**: draws the current layer after the user manually selects the shown color.
- **Skip Layer**: stops the current layer and advances to the next layer.
- **Redraw Layer**: resets the current layer so the next **Continue** can draw it again.
- **Move Up / Move Down**: reorders colors before the first **Continue**.
- **Pause**: pauses before the next gesture begins.
- **Resume**: continues only when Instagram Draw is still open and the drawing has not been deleted.
- **Cancel**: stops drawing but keeps the overlay available.
- **Emergency Stop**: stops drawing and closes the overlay service.
- **Collapse/Expand**: switches between compact bubble mode and full controller mode.

MuseTrace AI autosaves the active color, layer order, completed layers, skipped layers, and current stroke progress. After completion, the overlay shows **Drawing complete** and the user manually sends or saves in Instagram.

## Resume Choices

- **Workflow Resume**: MuseTrace AI saves the project, current layer, settings, calibration, strokes, and progress.
- **Canvas Resume**: only possible if Instagram Draw is still open and the drawing has not been deleted.
- **Restart from Layer 1**: safest option after leaving Draw mode.
- **Continue from selected layer**: use only after checking Instagram Draw is still open and the drawing has not been deleted.
- **Recalibrate**: save the final drawing area again before restarting.
- **Cancel session**: stops the drawing session while keeping project progress saved.

The controller is draggable, opacity-adjustable, and warns if it overlaps the calibrated drawing area. Drag it away if it covers the drawing canvas or Instagram color picker.

## What The Overlay Does Not Do

The overlay does not:

- Open Instagram
- Open a chat
- Enter Draw mode
- Select colors
- Use Add Space
- Send messages
- Bypass Instagram restrictions

## Troubleshooting

If the overlay does not appear:

1. Confirm overlay permission is enabled.
2. Confirm battery optimization is not stopping MuseTrace AI.
3. Restart the overlay from MuseTrace AI.
4. Reopen Instagram Draw manually.

