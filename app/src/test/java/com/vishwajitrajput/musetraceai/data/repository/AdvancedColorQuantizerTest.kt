package com.vishwajitrajput.musetraceai.data.repository

import android.content.Context
import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.vishwajitrajput.musetraceai.core.storage.ImageFileStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.roundToInt

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AdvancedColorQuantizerTest {
    @Test
    fun quantizeCreatesSortedTraceableLayersAndPreview() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val imageFileStore = ImageFileStore(context)
        val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val color = when {
                    x < 16 && y < 16 -> Color.rgb(24, 24, 28)
                    x >= 16 && y < 16 -> Color.rgb(236, 188, 148)
                    x < 16 -> Color.rgb(80, 126, 214)
                    else -> Color.rgb(244, 244, 238)
                }
                bitmap.setPixel(x, y, color)
            }
        }
        val edges = BooleanArray(bitmap.width * bitmap.height) { index ->
            val x = index % bitmap.width
            val y = index / bitmap.width
            x == 15 || y == 15
        }

        val result = AdvancedColorQuantizer.quantize(bitmap, requestedColorCount = 16, edges, imageFileStore)

        assertEquals(bitmap.width, result.previewBitmap.width)
        assertEquals(bitmap.height, result.previewBitmap.height)
        assertTrue(result.layers.isNotEmpty())
        assertTrue(result.layers.size <= 16)
        assertTrue(result.layers.all { it.colorHex.matches(Regex("#[0-9A-F]{6}")) })
        assertTrue(result.layers.all { it.maskUri.isNotBlank() && it.layerBitmapUri.isNotBlank() })
        assertEquals(result.layers.map { it.luma() }.sorted(), result.layers.map { it.luma() })
        assertTrue(result.layers.any { it.red > 180 && it.red > it.green && it.green > it.blue })
    }

    private fun QuantizedLayerSpec.luma(): Int =
        (red * 0.299f + green * 0.587f + blue * 0.114f).roundToInt()
}
