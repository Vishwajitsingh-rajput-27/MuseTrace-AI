package com.vishwajitrajput.musetraceai.core.storage

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vishwajitrajput.musetraceai.domain.model.OverlayControllerState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.overlayDataStore by preferencesDataStore(name = "musetrace_overlay_state")

@Singleton
class OverlayPositionStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val state = context.overlayDataStore.data.map { prefs ->
        OverlayControllerState(
            hasPosition = prefs[Keys.HAS_POSITION] ?: false,
            x = prefs[Keys.X] ?: 0,
            y = prefs[Keys.Y] ?: 0,
            collapsed = prefs[Keys.COLLAPSED] ?: false,
            opacity = (prefs[Keys.OPACITY] ?: DEFAULT_OPACITY).coerceIn(MIN_OPACITY, MAX_OPACITY),
            sizeScale = (prefs[Keys.SIZE_SCALE] ?: DEFAULT_SIZE).coerceIn(MIN_SIZE, MAX_SIZE),
            updatedAtMillis = prefs[Keys.UPDATED_AT] ?: 0L,
        )
    }

    suspend fun load(): OverlayControllerState = state.first()

    suspend fun save(next: OverlayControllerState) {
        context.overlayDataStore.edit { prefs ->
            prefs[Keys.HAS_POSITION] = next.hasPosition
            prefs[Keys.X] = next.x
            prefs[Keys.Y] = next.y
            prefs[Keys.COLLAPSED] = next.collapsed
            prefs[Keys.OPACITY] = next.opacity.coerceIn(MIN_OPACITY, MAX_OPACITY)
            prefs[Keys.SIZE_SCALE] = next.sizeScale.coerceIn(MIN_SIZE, MAX_SIZE)
            prefs[Keys.UPDATED_AT] = next.updatedAtMillis.takeIf { it > 0L } ?: System.currentTimeMillis()
        }
    }

    suspend fun saveOpacity(opacity: Float) {
        val current = load()
        save(current.copy(opacity = opacity.coerceIn(MIN_OPACITY, MAX_OPACITY)))
    }

    suspend fun saveSize(size: Float) {
        val current = load()
        save(current.copy(sizeScale = size.coerceIn(MIN_SIZE, MAX_SIZE)))
    }

    suspend fun resetPosition() {
        context.overlayDataStore.edit { prefs ->
            prefs[Keys.HAS_POSITION] = false
            prefs.remove(Keys.X)
            prefs.remove(Keys.Y)
            prefs[Keys.UPDATED_AT] = System.currentTimeMillis()
        }
    }

    private object Keys {
        val HAS_POSITION = booleanPreferencesKey("has_position")
        val X = intPreferencesKey("x")
        val Y = intPreferencesKey("y")
        val COLLAPSED = booleanPreferencesKey("collapsed")
        val OPACITY = floatPreferencesKey("opacity")
        val SIZE_SCALE = floatPreferencesKey("size_scale")
        val UPDATED_AT = longPreferencesKey("updated_at")
    }

    private companion object {
        const val DEFAULT_OPACITY = 0.94f
        const val MIN_OPACITY = 0.48f
        const val MAX_OPACITY = 1f
        const val DEFAULT_SIZE = 1f
        const val MIN_SIZE = 0.8f
        const val MAX_SIZE = 1.35f
    }
}
