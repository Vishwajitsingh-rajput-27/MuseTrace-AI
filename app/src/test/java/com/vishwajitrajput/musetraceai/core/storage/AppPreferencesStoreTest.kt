package com.vishwajitrajput.musetraceai.core.storage

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.vishwajitrajput.musetraceai.domain.model.AppTheme
import com.vishwajitrajput.musetraceai.domain.model.CanvasQuality
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AppPreferencesStoreTest {
    @Test
    fun settingsStoragePersistsAndClampsReleaseControls() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = AppPreferencesStore(context)

        store.saveDefaultColorCount(24)
        store.saveDrawingSpeed(99f)
        store.saveGestureDelay(-1L)
        store.saveOverlayOpacity(0f)
        store.saveOverlaySize(9f)
        store.saveCanvasQuality(CanvasQuality.High)
        store.saveAppTheme(AppTheme.Dark)
        store.saveSimpleMode(false)
        store.saveCrashRecoveryEnabled(false)
        store.saveKeepScreenAwake(true)

        val settings = store.settings.first()

        assertEquals(24, settings.defaultColorCount)
        assertEquals(2.5f, settings.drawingSpeed, 0.001f)
        assertEquals(0L, settings.gestureDelayMs)
        assertEquals(0.48f, settings.overlayOpacity, 0.001f)
        assertEquals(1.35f, settings.overlaySize, 0.001f)
        assertEquals(CanvasQuality.High, settings.canvasQuality)
        assertEquals(AppTheme.Dark, settings.appTheme)
        assertEquals(false, settings.simpleMode)
        assertEquals(false, settings.crashRecoveryEnabled)
        assertEquals(true, settings.keepScreenAwake)
    }
}
