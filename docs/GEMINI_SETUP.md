# Gemini Setup

MuseTrace AI uses Gemini as the default AI provider for prompt-to-image generation.

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

## Get A Gemini API Key

1. Open Google AI Studio in a browser.
2. Create or select a Gemini API key.
3. Copy the key.

Do not share the key publicly. Do not commit it to GitHub.

## Save The Key In MuseTrace AI

1. Open MuseTrace AI.
2. Go to **Settings**.
3. Paste the Gemini API key.
4. Tap **Save key securely**.

The key is stored on the phone using encrypted Android storage. It is not hardcoded in the app source.

## Remove The Key

1. Open MuseTrace AI.
2. Go to **Settings**.
3. Tap **Clear Gemini Key**.

## Common Errors

### Gemini generation fails

Check:

- The key is valid.
- The key was saved in Settings.
- The phone has internet access.
- The Gemini API is available for the key and account.

### Never Commit Keys

The repository ignores common secret files such as:

- `.env`
- `local.properties`
- `secrets.properties`
- `apikeys.properties`
- `google-services.json`
- signing keys and keystores

