# Security Policy

## Supported Project

Security reports are accepted for the current `main` branch of MuseTrace AI.

## Secrets Policy

Never commit:

- Gemini API keys
- Android signing keys
- Keystores
- Passwords
- Tokens
- `.env` files
- `google-services.json`
- `local.properties`

The app stores the Gemini API key on-device with encrypted Android storage. Keys must not be logged, printed, uploaded, or included in screenshots.

## Instagram Safety Boundary

"MuseTrace AI is an independent Android drawing-assistance application. It is not affiliated with, endorsed by, sponsored by, or associated with Instagram or Meta.

MuseTrace AI works only if Instagram Draw is already available on the user's account, app version, region, and device. It cannot enable, unlock, force, or add Instagram Draw.

The app does not log into Instagram, request Instagram credentials, store Instagram credentials, use private Instagram APIs, scrape Instagram, bypass security systems, bypass platform restrictions, or send messages automatically.

Users manually open Instagram, manually open a chat, manually open Draw mode, manually select colors, and explicitly start each drawing step.

MuseTrace AI only performs drawing gestures through Android AccessibilityService after user confirmation."

## Drawing Warning

"Keep Instagram Draw open until drawing is finished. If you press Back, Home, switch apps, lock your phone, or close Instagram, Instagram may delete the current drawing. MuseTrace AI can save your project workflow, but it cannot restore an Instagram canvas that Instagram cleared."

## Disallowed Behavior

MuseTrace AI must not:

- Log into Instagram
- Ask for Instagram username or password
- Store Instagram credentials
- Use Instagram private APIs
- Scrape Instagram
- Bypass login, captcha, restrictions, security systems, or rate limits
- Send messages automatically
- Automatically select Instagram colors
- Unlock or force Instagram Draw

## Reporting A Vulnerability

If you find a vulnerability, create a private security advisory on GitHub for:

`Vishwajitsingh-rajput-27/MuseTrace-AI`

Include:

- A short summary
- Impact
- Steps to reproduce
- Android version and device
- App version or commit
- Screenshots or logs with secrets removed

Do not include API keys, access tokens, passwords, private chats, or personal data.

## Response Expectations

Security issues will be reviewed as soon as practical. Confirmed issues should be fixed before public disclosure.

