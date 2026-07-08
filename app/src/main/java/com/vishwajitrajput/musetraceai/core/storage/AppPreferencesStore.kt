package com.vishwajitrajput.musetraceai.core.storage

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vishwajitrajput.musetraceai.core.common.AppConstants
import com.vishwajitrajput.musetraceai.domain.SettingsRepository
import com.vishwajitrajput.musetraceai.domain.model.AppSettings
import com.vishwajitrajput.musetraceai.domain.model.AppTheme
import com.vishwajitrajput.musetraceai.domain.model.CalibrationProfile
import com.vishwajitrajput.musetraceai.domain.model.CalibrationProfileType
import com.vishwajitrajput.musetraceai.domain.model.CanvasQuality
import com.vishwajitrajput.musetraceai.domain.model.TracePoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private val Context.museTraceDataStore by preferencesDataStore(name = "musetrace_settings")

@Singleton
class AppPreferencesStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsRepository {
    override val settings: Flow<AppSettings> = context.museTraceDataStore.data.map { prefs ->
        val selectedType = CalibrationProfileType.fromStorageKey(prefs[Keys.SELECTED_CALIBRATION_TYPE])
        val profiles = CalibrationProfileType.entries.associateWith { type ->
            prefs[Keys.calibrationProfile(type)]?.decodeCalibrationProfile(type)
                ?: legacyCalibrationProfile(type, prefs[Keys.CALIBRATION_LEFT], prefs[Keys.CALIBRATION_TOP], prefs[Keys.CALIBRATION_WIDTH], prefs[Keys.CALIBRATION_HEIGHT])
                ?: CalibrationProfile(type = type)
        }
        AppSettings(
            defaultColorCount = prefs[Keys.DEFAULT_COLOR_COUNT] ?: AppConstants.DEFAULT_COLOR_COUNT,
            calibrationProfiles = profiles,
            selectedCalibrationType = selectedType,
            disclaimerAccepted = prefs[Keys.DISCLAIMER_ACCEPTED] ?: false,
            drawingSpeed = (prefs[Keys.DRAWING_SPEED] ?: 1f).coerceIn(0.35f, 2.5f),
            smoothingLevel = (prefs[Keys.SMOOTHING_LEVEL] ?: 0.65f).coerceIn(0f, 1f),
            simplificationLevel = (prefs[Keys.SIMPLIFICATION_LEVEL] ?: 0.55f).coerceIn(0f, 1f),
            minimumStrokeLength = (prefs[Keys.MINIMUM_STROKE_LENGTH] ?: 6f).coerceIn(1f, 40f),
            gestureDelayMs = (prefs[Keys.GESTURE_DELAY_MS] ?: 120L).coerceIn(0L, 800L),
            canvasQuality = CanvasQuality.fromStorageKey(prefs[Keys.CANVAS_QUALITY]),
            overlayOpacity = (prefs[Keys.OVERLAY_OPACITY] ?: 0.94f).coerceIn(0.48f, 1f),
            overlaySize = (prefs[Keys.OVERLAY_SIZE] ?: 1f).coerceIn(0.8f, 1.35f),
            appTheme = AppTheme.fromStorageKey(prefs[Keys.APP_THEME]),
            simpleMode = prefs[Keys.SIMPLE_MODE] ?: true,
            crashRecoveryEnabled = prefs[Keys.CRASH_RECOVERY_ENABLED] ?: true,
            keepScreenAwake = prefs[Keys.KEEP_SCREEN_AWAKE] ?: false,
        )
    }

    override suspend fun saveDefaultColorCount(count: Int) {
        val safeCount = if (count in AppConstants.ALLOWED_COLOR_COUNTS) count else AppConstants.DEFAULT_COLOR_COUNT
        context.museTraceDataStore.edit { it[Keys.DEFAULT_COLOR_COUNT] = safeCount }
    }

    override suspend fun saveCalibration(left: Int, top: Int, width: Int, height: Int) {
        val profile = CalibrationProfile.fromBounds(
            type = CalibrationProfileType.NormalDraw,
            left = left,
            top = top,
            width = width,
            height = height,
            screenWidth = 0,
            screenHeight = 0,
            savedAtMillis = System.currentTimeMillis(),
        )
        context.museTraceDataStore.edit {
            it[Keys.CALIBRATION_LEFT] = left.coerceAtLeast(0)
            it[Keys.CALIBRATION_TOP] = top.coerceAtLeast(0)
            it[Keys.CALIBRATION_WIDTH] = width.coerceAtLeast(120)
            it[Keys.CALIBRATION_HEIGHT] = height.coerceAtLeast(120)
            it[Keys.SELECTED_CALIBRATION_TYPE] = CalibrationProfileType.NormalDraw.storageKey
            it[Keys.calibrationProfile(CalibrationProfileType.NormalDraw)] = profile.encode()
        }
    }

    override suspend fun saveCalibrationProfile(profile: CalibrationProfile) {
        context.museTraceDataStore.edit {
            it[Keys.SELECTED_CALIBRATION_TYPE] = profile.type.storageKey
            it[Keys.calibrationProfile(profile.type)] = profile.encode()
        }
    }

    override suspend fun selectCalibrationProfile(type: CalibrationProfileType) {
        context.museTraceDataStore.edit { it[Keys.SELECTED_CALIBRATION_TYPE] = type.storageKey }
    }

    override suspend fun saveDisclaimerAccepted(accepted: Boolean) {
        context.museTraceDataStore.edit { it[Keys.DISCLAIMER_ACCEPTED] = accepted }
    }

    override suspend fun saveDrawingSpeed(speed: Float) {
        context.museTraceDataStore.edit { it[Keys.DRAWING_SPEED] = speed.coerceIn(0.35f, 2.5f) }
    }

    override suspend fun saveSmoothingLevel(level: Float) {
        context.museTraceDataStore.edit { it[Keys.SMOOTHING_LEVEL] = level.coerceIn(0f, 1f) }
    }

    override suspend fun saveSimplificationLevel(level: Float) {
        context.museTraceDataStore.edit { it[Keys.SIMPLIFICATION_LEVEL] = level.coerceIn(0f, 1f) }
    }

    override suspend fun saveMinimumStrokeLength(length: Float) {
        context.museTraceDataStore.edit { it[Keys.MINIMUM_STROKE_LENGTH] = length.coerceIn(1f, 40f) }
    }

    override suspend fun saveGestureDelay(delayMs: Long) {
        context.museTraceDataStore.edit { it[Keys.GESTURE_DELAY_MS] = delayMs.coerceIn(0L, 800L) }
    }

    override suspend fun saveCanvasQuality(quality: CanvasQuality) {
        context.museTraceDataStore.edit { it[Keys.CANVAS_QUALITY] = quality.storageKey }
    }

    override suspend fun saveOverlayOpacity(opacity: Float) {
        context.museTraceDataStore.edit { it[Keys.OVERLAY_OPACITY] = opacity.coerceIn(0.48f, 1f) }
    }

    override suspend fun saveOverlaySize(size: Float) {
        context.museTraceDataStore.edit { it[Keys.OVERLAY_SIZE] = size.coerceIn(0.8f, 1.35f) }
    }

    override suspend fun saveAppTheme(theme: AppTheme) {
        context.museTraceDataStore.edit { it[Keys.APP_THEME] = theme.storageKey }
    }

    override suspend fun saveSimpleMode(simpleMode: Boolean) {
        context.museTraceDataStore.edit { it[Keys.SIMPLE_MODE] = simpleMode }
    }

    override suspend fun saveCrashRecoveryEnabled(enabled: Boolean) {
        context.museTraceDataStore.edit { it[Keys.CRASH_RECOVERY_ENABLED] = enabled }
    }

    override suspend fun saveKeepScreenAwake(enabled: Boolean) {
        context.museTraceDataStore.edit { it[Keys.KEEP_SCREEN_AWAKE] = enabled }
    }

    private fun legacyCalibrationProfile(
        type: CalibrationProfileType,
        left: Int?,
        top: Int?,
        width: Int?,
        height: Int?,
    ): CalibrationProfile? {
        if (type != CalibrationProfileType.NormalDraw) return null
        if (left == null || top == null || width == null || height == null) return null
        return CalibrationProfile.fromBounds(
            type = type,
            left = left,
            top = top,
            width = width,
            height = height,
            screenWidth = 0,
            screenHeight = 0,
            savedAtMillis = System.currentTimeMillis(),
        )
    }

    private object Keys {
        val DEFAULT_COLOR_COUNT = intPreferencesKey("default_color_count")
        val SELECTED_CALIBRATION_TYPE = stringPreferencesKey("selected_calibration_type")
        val CALIBRATION_LEFT = intPreferencesKey("calibration_left")
        val CALIBRATION_TOP = intPreferencesKey("calibration_top")
        val CALIBRATION_WIDTH = intPreferencesKey("calibration_width")
        val CALIBRATION_HEIGHT = intPreferencesKey("calibration_height")
        val DISCLAIMER_ACCEPTED = booleanPreferencesKey("disclaimer_accepted")
        val DRAWING_SPEED = floatPreferencesKey("drawing_speed")
        val SMOOTHING_LEVEL = floatPreferencesKey("smoothing_level")
        val SIMPLIFICATION_LEVEL = floatPreferencesKey("simplification_level")
        val MINIMUM_STROKE_LENGTH = floatPreferencesKey("minimum_stroke_length")
        val GESTURE_DELAY_MS = longPreferencesKey("gesture_delay_ms")
        val CANVAS_QUALITY = stringPreferencesKey("canvas_quality")
        val OVERLAY_OPACITY = floatPreferencesKey("overlay_opacity")
        val OVERLAY_SIZE = floatPreferencesKey("overlay_size")
        val APP_THEME = stringPreferencesKey("app_theme")
        val SIMPLE_MODE = booleanPreferencesKey("simple_mode")
        val CRASH_RECOVERY_ENABLED = booleanPreferencesKey("crash_recovery_enabled")
        val KEEP_SCREEN_AWAKE = booleanPreferencesKey("keep_screen_awake")

        fun calibrationProfile(type: CalibrationProfileType) =
            stringPreferencesKey("calibration_profile_${type.storageKey}")
    }
}

private fun CalibrationProfile.encode(): String =
    JSONObject()
        .put("type", type.storageKey)
        .put("topLeft", topLeft.toJson())
        .put("topRight", topRight.toJson())
        .put("bottomLeft", bottomLeft.toJson())
        .put("bottomRight", bottomRight.toJson())
        .put("screenWidth", screenWidth)
        .put("screenHeight", screenHeight)
        .put("savedAtMillis", savedAtMillis)
        .toString()

private fun String.decodeCalibrationProfile(fallbackType: CalibrationProfileType): CalibrationProfile? =
    runCatching {
        val json = JSONObject(this)
        CalibrationProfile(
            type = CalibrationProfileType.fromStorageKey(json.optString("type")).takeIf { it == fallbackType } ?: fallbackType,
            topLeft = json.getJSONArray("topLeft").toTracePoint(),
            topRight = json.getJSONArray("topRight").toTracePoint(),
            bottomLeft = json.getJSONArray("bottomLeft").toTracePoint(),
            bottomRight = json.getJSONArray("bottomRight").toTracePoint(),
            screenWidth = json.optInt("screenWidth", 0),
            screenHeight = json.optInt("screenHeight", 0),
            savedAtMillis = json.optLong("savedAtMillis", 0L),
        )
    }.getOrNull()

private fun TracePoint.toJson(): JSONArray = JSONArray().put(x.toDouble()).put(y.toDouble())

private fun JSONArray.toTracePoint(): TracePoint =
    TracePoint(
        x = optDouble(0, 0.0).toFloat(),
        y = optDouble(1, 0.0).toFloat(),
    )
