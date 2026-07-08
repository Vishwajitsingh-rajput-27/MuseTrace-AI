package com.vishwajitrajput.musetraceai.feature.guide

import android.os.Bundle
import android.view.View
import com.vishwajitrajput.musetraceai.core.common.LegalCopy
import com.vishwajitrajput.musetraceai.core.ui.BaseFragment
import com.vishwajitrajput.musetraceai.core.ui.infoCard
import com.vishwajitrajput.musetraceai.core.ui.warningCard
import com.vishwajitrajput.musetraceai.databinding.FragmentGuideBinding

class GuideFragment : BaseFragment<FragmentGuideBinding>(FragmentGuideBinding::inflate) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.guideContainer.removeAllViews()
        binding.guideContainer.addView(requireContext().infoCard("MuseTrace AI Guide", "Use this guide in order the first time you set up the app. During drawing, keep Instagram Draw open and use the floating overlay instead of switching back to MuseTrace AI."))
        binding.guideContainer.addView(requireContext().infoCard("Required disclaimer", LegalCopy.DISCLAIMER))
        binding.guideContainer.addView(requireContext().warningCard("Drawing warning", LegalCopy.DRAWING_WARNING))
        binding.guideContainer.addView(
            requireContext().warningCard(
                "Resume safety",
                "${LegalCopy.RESUME_WARNING}\n\n${LegalCopy.RESUME_EXPLANATION}",
            ),
        )
        guideTopics.forEach { topic ->
            val card = if (topic.warning) {
                requireContext().warningCard(topic.title, topic.body)
            } else {
                requireContext().infoCard(topic.title, topic.body)
            }
            binding.guideContainer.addView(card)
        }
    }

    private companion object {
        private val guideTopics = listOf(
            GuideTopic(
                title = "1. Install APK",
                body = "Download MuseTrace-AI.apk from the official GitHub release page. Open the APK on your Android phone, allow installation from that source if Android asks, then tap Install. If Android blocks the file, open Settings, allow installs from your browser or file manager, and try again.",
            ),
            GuideTopic(
                title = "2. Enter Gemini API key",
                body = "Open MuseTrace AI, go to Settings, paste your Gemini API key, and tap Save key securely. Use Test Gemini Key to check it. The key is stored on the phone with encrypted storage and is not shown in logs.",
            ),
            GuideTopic(
                title = "3. Generate image",
                body = "Open AI Image Generator. Type what you want, choose a drawing style and aspect ratio, then tap Generate. When the image appears, tap Use for drawing to send it to the editor.",
            ),
            GuideTopic(
                title = "4. Import image",
                body = "Use the Editor to pick a photo from Gallery or capture one with Camera. Crop, rotate, flip, resize, and adjust brightness, contrast, saturation, sharpness, noise reduction, edges, background simplification, and portrait settings before continuing.",
            ),
            GuideTopic(
                title = "5. Enable Accessibility permission",
                body = "Android needs Accessibility permission so MuseTrace AI can draw strokes after you press Continue. Open Android Accessibility settings, choose MuseTrace AI, and turn it on. The app does not read Instagram messages, type messages, tap login buttons, or send anything.",
            ),
            GuideTopic(
                title = "6. Enable Display Over Other Apps permission",
                body = "Android needs Display Over Other Apps permission so the floating controller can stay on top of Instagram Draw. Open the overlay permission screen from MuseTrace AI or Android Settings, select MuseTrace AI, and allow it.",
            ),
            GuideTopic(
                title = "7. Open Instagram manually",
                body = "Open the Instagram app yourself. MuseTrace AI does not log in, bypass login, or open private Instagram features for you. Make sure your account already has Instagram Draw available.",
                warning = true,
            ),
            GuideTopic(
                title = "8. Open chat manually",
                body = "Choose the chat yourself inside Instagram. MuseTrace AI will not choose a person, open a chat, send a message, or scrape anything.",
            ),
            GuideTopic(
                title = "9. Open Draw mode manually",
                body = "Open Instagram Draw mode yourself. If Draw is not available on your account, app version, region, or device, MuseTrace AI cannot add it.",
                warning = true,
            ),
            GuideTopic(
                title = "10. Use Add Space manually",
                body = "If Instagram shows Add Space and you want a larger drawing area, tap Add Space yourself before calibration. MuseTrace AI does not automatically tap Add Space. Use Add Space manually first, then calibrate.",
                warning = true,
            ),
            GuideTopic(
                title = "11. Calibrate drawing area",
                body = "Return to MuseTrace AI only before drawing starts. Open Calibration and save the final Instagram Draw area by setting top-left, top-right, bottom-left, and bottom-right points. The preview should match the real area where strokes are allowed.",
            ),
            GuideTopic(
                title = "12. Start floating overlay session",
                body = "Open the Drawing Session screen, read the checklist, then tap Start Overlay Session. The overlay appears above other apps. Go back to Instagram Draw manually and keep it open.",
            ),
            GuideTopic(
                title = "13. Select each color manually",
                body = "For each layer, the overlay shows the color number, color sample, HEX, RGB, estimated time, and stroke count. Select that color yourself in Instagram Draw.",
            ),
            GuideTopic(
                title = "14. Tap Continue in overlay",
                body = "After you manually choose the shown color in Instagram Draw, tap Continue in the floating overlay. MuseTrace AI draws only after that confirmation. When the layer finishes, the overlay moves to the next color.",
            ),
            GuideTopic(
                title = "15. Pause, resume, or cancel",
                body = "Use Pause if you need to stop gestures briefly while Instagram Draw stays open. Use Resume to continue. Use Cancel to stop the session. Use Emergency Stop if something looks wrong and you want all drawing gestures to stop immediately.",
            ),
            GuideTopic(
                title = "16. Save project",
                body = "MuseTrace AI saves the project workflow, layers, strokes, settings, calibration, and progress. This helps you reopen the project later, but it does not restore an Instagram canvas that Instagram cleared.",
            ),
            GuideTopic(
                title = "17. Restart from Layer 1 if Instagram clears canvas",
                body = "If you press Back, Home, switch apps, lock the phone, or Instagram closes Draw mode, Instagram may delete the current canvas. If that happens, choose Restart from Layer 1 for the safest result.",
                warning = true,
            ),
        )
    }
}

private data class GuideTopic(
    val title: String,
    val body: String,
    val warning: Boolean = false,
)
