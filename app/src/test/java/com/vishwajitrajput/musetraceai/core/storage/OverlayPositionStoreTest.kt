package com.vishwajitrajput.musetraceai.core.storage

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.vishwajitrajput.musetraceai.domain.model.OverlayControllerState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class OverlayPositionStoreTest {
    @Test
    fun overlayStateIsPersistedClampedAndResettable() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = OverlayPositionStore(context)

        store.save(
            OverlayControllerState(
                hasPosition = true,
                x = 123,
                y = 456,
                collapsed = true,
                opacity = 2f,
                sizeScale = 4f,
                updatedAtMillis = 1L,
            ),
        )

        val saved = store.load()
        assertTrue(saved.hasPosition)
        assertEquals(123, saved.x)
        assertEquals(456, saved.y)
        assertEquals(true, saved.collapsed)
        assertEquals(1f, saved.opacity, 0.001f)
        assertEquals(1.35f, saved.sizeScale, 0.001f)

        store.resetPosition()
        assertFalse(store.load().hasPosition)
    }
}
