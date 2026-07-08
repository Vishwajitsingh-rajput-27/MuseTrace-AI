package com.vishwajitrajput.musetraceai.feature.troubleshooting

import android.os.Bundle
import android.view.View
import com.vishwajitrajput.musetraceai.core.common.LegalCopy
import com.vishwajitrajput.musetraceai.core.ui.BaseFragment
import com.vishwajitrajput.musetraceai.core.ui.infoCard
import com.vishwajitrajput.musetraceai.core.ui.warningCard
import com.vishwajitrajput.musetraceai.databinding.FragmentTroubleshootingBinding

class TroubleshootingFragment : BaseFragment<FragmentTroubleshootingBinding>(FragmentTroubleshootingBinding::inflate) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.troubleshootingContainer.removeAllViews()
        binding.troubleshootingContainer.addView(requireContext().infoCard("Troubleshooting", "Find the problem below, then follow the steps in order. These fixes use normal Android and Instagram controls only."))
        binding.troubleshootingContainer.addView(requireContext().infoCard("Required disclaimer", LegalCopy.DISCLAIMER))
        binding.troubleshootingContainer.addView(requireContext().warningCard("Drawing warning", LegalCopy.DRAWING_WARNING))
        binding.troubleshootingContainer.addView(
            requireContext().warningCard(
                "Resume safety",
                "${LegalCopy.RESUME_WARNING}\n\n${LegalCopy.RESUME_EXPLANATION}\n\nUse Restart from Layer 1 if you are not sure the Instagram canvas is still open.",
            ),
        )
        troubleshootingTopics.forEach { topic ->
            val card = if (topic.warning) {
                requireContext().warningCard(topic.title, topic.body)
            } else {
                requireContext().infoCard(topic.title, topic.body)
            }
            binding.troubleshootingContainer.addView(card)
        }
    }

    private companion object {
        private val troubleshootingTopics = listOf(
            TroubleshootingTopic(
                title = "Draw option missing",
                body = "MuseTrace AI cannot enable or unlock Instagram Draw. Update Instagram, restart the phone, check another account only if it is yours, and wait if the feature is not available in your region or app version.",
                warning = true,
            ),
            TroubleshootingTopic(
                title = "Add Space missing",
                body = "Add Space is controlled by Instagram. If you do not see it, continue with the normal Draw area or try after updating Instagram. MuseTrace AI does not add, force, or tap Add Space.",
                warning = true,
            ),
            TroubleshootingTopic(
                title = "Overlay not showing",
                body = "Open Settings or Drawing Session and use Open Overlay Permission Settings. Allow Display Over Other Apps for MuseTrace AI. Then start the overlay session again. On some phones, battery saver or security apps can hide overlays, so allow MuseTrace AI to run normally.",
            ),
            TroubleshootingTopic(
                title = "Overlay covering drawing area",
                body = "Drag the overlay away from the drawing area. If it keeps opening in a bad spot, go to Settings and tap Reset overlay position. The app will try to place it outside the calibrated area when possible.",
            ),
            TroubleshootingTopic(
                title = "Calibration wrong",
                body = "Open Instagram Draw first, use Add Space manually if needed, then return to Calibration. Save top-left, top-right, bottom-left, and bottom-right again. The preview should line up with the final Draw canvas, not the whole phone screen.",
            ),
            TroubleshootingTopic(
                title = "Drawing outside area",
                body = "Stop the session, recalibrate the four corners, and start again. Make sure you calibrated after opening the same Instagram Draw layout you will use for drawing. If Add Space changed the canvas size, calibrate again after using it.",
            ),
            TroubleshootingTopic(
                title = "Colors not matching",
                body = "Instagram colors are selected manually, so choose the color shown in the overlay as closely as possible. Use the HEX and RGB values as a guide. If a color is too hard to match, try a lower color count or merge similar colors before starting.",
            ),
            TroubleshootingTopic(
                title = "Gestures not working",
                body = "Check that Accessibility is enabled for MuseTrace AI, Instagram is open in the foreground, Draw mode is still active, and calibration is usable. Tap Continue only after selecting the color manually. If gestures still fail, cancel and start a new overlay session.",
            ),
            TroubleshootingTopic(
                title = "Accessibility disabled",
                body = "Android can turn Accessibility off after app updates, restarts, or battery restrictions. Open Android Accessibility settings, select MuseTrace AI, and turn it on again. If Android warns you, read it carefully and only continue if you trust the app.",
            ),
            TroubleshootingTopic(
                title = "Overlay permission disabled",
                body = "Open Android Settings, search for Display Over Other Apps or Appear on Top, choose MuseTrace AI, and allow it. Then return to MuseTrace AI and start the overlay session again.",
            ),
            TroubleshootingTopic(
                title = "Gemini key invalid",
                body = "Open Settings, clear the saved key, paste the correct Gemini API key, and tap Save key securely. Then tap Test Gemini Key. Do not paste extra spaces before or after the key.",
            ),
            TroubleshootingTopic(
                title = "AI generation failed",
                body = "Check your internet connection, test the Gemini key, and try a shorter prompt. If the service is busy or rate limited, wait a few minutes and try again. You can still import a Gallery or Camera image instead.",
            ),
            TroubleshootingTopic(
                title = "Image too detailed",
                body = "Use the editor to simplify the background, increase contrast, reduce noise, and choose 16 colors for a cleaner drawing. Prompts work best when they ask for bold outlines, simple shapes, high contrast, and no tiny text.",
            ),
            TroubleshootingTopic(
                title = "Instagram canvas cleared",
                body = "Instagram may clear the canvas if you press Back, press Home, switch apps, lock the phone, or leave Draw mode. MuseTrace AI can save your project workflow, but it cannot restore a canvas Instagram deleted. Restart from Layer 1 for the safest result.",
                warning = true,
            ),
        )
    }
}

private data class TroubleshootingTopic(
    val title: String,
    val body: String,
    val warning: Boolean = false,
)
