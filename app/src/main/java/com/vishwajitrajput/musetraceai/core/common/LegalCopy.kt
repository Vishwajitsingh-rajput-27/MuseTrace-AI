package com.vishwajitrajput.musetraceai.core.common

object LegalCopy {
    const val DISCLAIMER = "\"MuseTrace AI is an independent Android drawing-assistance application. It is not affiliated with, endorsed by, sponsored by, or associated with Instagram or Meta.\n\nMuseTrace AI works only if Instagram Draw is already available on the user's account, app version, region, and device. It cannot enable, unlock, force, or add Instagram Draw.\n\nThe app does not log into Instagram, request Instagram credentials, store Instagram credentials, use private Instagram APIs, scrape Instagram, bypass security systems, bypass platform restrictions, or send messages automatically.\n\nUsers manually open Instagram, manually open a chat, manually open Draw mode, manually select colors, and explicitly start each drawing step.\n\nMuseTrace AI only performs drawing gestures through Android AccessibilityService after user confirmation.\""

    const val DRAWING_WARNING = "\"Keep Instagram Draw open until drawing is finished. If you press Back, Home, switch apps, lock your phone, or close Instagram, Instagram may delete the current drawing. MuseTrace AI can save your project workflow, but it cannot restore an Instagram canvas that Instagram cleared.\""
    const val RESUME_WARNING = "Instagram Draw may have cleared your canvas. MuseTrace AI saved your project progress, but it cannot restore a drawing that Instagram deleted. For best results, restart from Layer 1."

    const val RESUME_EXPLANATION = "Workflow Resume saves the project, current layer, settings, calibration, strokes, and progress. Canvas Resume is only possible if Instagram Draw is still open and the drawing has not been deleted."
}
