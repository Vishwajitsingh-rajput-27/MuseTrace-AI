# Instagram Draw Guide

MuseTrace AI helps users recreate layered sketches inside Instagram Draw, but the user controls Instagram manually.

## Safety Boundary

"MuseTrace AI is an independent Android drawing-assistance application. It is not affiliated with, endorsed by, sponsored by, or associated with Instagram or Meta.

MuseTrace AI works only if Instagram Draw is already available on the user's account, app version, region, and device. It cannot enable, unlock, force, or add Instagram Draw.

The app does not log into Instagram, request Instagram credentials, store Instagram credentials, use private Instagram APIs, scrape Instagram, bypass security systems, bypass platform restrictions, or send messages automatically.

Users manually open Instagram, manually open a chat, manually open Draw mode, manually select colors, and explicitly start each drawing step.

MuseTrace AI only performs drawing gestures through Android AccessibilityService after user confirmation."

## Drawing Warning

"Keep Instagram Draw open until drawing is finished. If you press Back, Home, switch apps, lock your phone, or close Instagram, Instagram may delete the current drawing. MuseTrace AI can save your project workflow, but it cannot restore an Instagram canvas that Instagram cleared."

## Disallowed Behavior

MuseTrace AI does not:

- Log into Instagram
- Ask for Instagram username or password
- Store Instagram credentials
- Use Instagram private APIs
- Scrape Instagram
- Bypass login, captcha, security systems, restrictions, or rate limits
- Send messages automatically
- Select colors automatically
- Unlock Instagram Draw

## Required Manual Steps

1. Open Instagram yourself.
2. Open the chat yourself.
3. Open Instagram Draw mode yourself.
4. Use Add Space yourself if available.
5. Calibrate the final drawing area yourself with top-left, top-right, bottom-left, and bottom-right points.
6. Select every layer color yourself.
7. Confirm each layer only by tapping **Continue** in the floating overlay.

## Drawing Workflow

1. Create or open a sketch in MuseTrace AI.
2. Open **Sketch Preview**.
3. Inspect the layer and stroke previews.
4. Calibrate the canvas area.
5. Open Instagram Draw manually.
6. Tap **Start Instagram Drawing Session** and review the checklist.
7. Tap **Start Overlay Session**.
8. Select the color shown by MuseTrace AI.
9. Tap **Continue** in the floating overlay.
10. Wait for the layer to complete.
11. Select the next color manually.
12. Repeat until all layers are complete.
13. When the overlay shows **Drawing complete**, manually send or save in Instagram.

The start checklist includes **Start Overlay Session**, **Open Overlay Permission Settings**, **Open Accessibility Settings**, **Open Calibration**, and **Cancel**.

Layer order can be changed before the first **Continue**. During drawing, MuseTrace AI autosaves the current color, completed layers, skipped layers, and current stroke progress.

## Foreground Safety

Drawing pauses immediately if Instagram is no longer the foreground app.

MuseTrace AI shows:

> Instagram Draw may have cleared your canvas. MuseTrace AI saved your project progress, but it cannot restore a drawing that Instagram deleted. For best results, restart from Layer 1.

Workflow Resume saves project progress. Canvas Resume is only possible if Instagram Draw is still open and the drawing has not been deleted. Choose **Restart from Layer 1**, **Continue from selected layer**, **Recalibrate**, or **Cancel session** from the overlay.

## Calibration Tips

- Calibrate after Instagram Draw is visible.
- MuseTrace AI does not automatically tap Add Space. Use Add Space manually first, then calibrate.
- Save separate profiles for Normal Draw, Add Space Small, Add Space Medium, Add Space Maximum, and Custom.
- Keep phone orientation stable.
- If strokes are shifted, adjust the nearest corner points.
- If strokes are stretched or rotated, recalibrate all four corner points.
- The preview shows the final drawing bounds, safe drawing area, and overlay-safe zones.

## Limitations

Instagram can change its UI at any time. MuseTrace AI only sends Android gestures to the calibrated area after confirmation.

