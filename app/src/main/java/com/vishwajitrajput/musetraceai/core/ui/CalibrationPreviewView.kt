package com.vishwajitrajput.musetraceai.core.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.vishwajitrajput.musetraceai.R
import com.vishwajitrajput.musetraceai.domain.model.CalibrationCorner
import com.vishwajitrajput.musetraceai.domain.model.CalibrationProfile
import com.vishwajitrajput.musetraceai.domain.model.TracePoint
import kotlin.math.min

class CalibrationPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = context.resources.getDimension(R.dimen.sf_border_width) * 2f
        strokeJoin = Paint.Join.ROUND
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.color(R.color.mt_text_secondary)
        textSize = 12f * resources.displayMetrics.scaledDensity
    }
    private var profile = CalibrationProfile()
    private var selectedCorner = CalibrationCorner.TopLeft

    fun bind(profile: CalibrationProfile, selectedCorner: CalibrationCorner) {
        this.profile = profile
        this.selectedCorner = selectedCorner
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val screenWidth = profile.screenWidth.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        val screenHeight = profile.screenHeight.takeIf { it > 0 } ?: resources.displayMetrics.heightPixels
        val padding = resources.getDimension(R.dimen.space_16)
        val previewWidth = (width - padding * 2f).coerceAtLeast(1f)
        val previewHeight = (height - padding * 2f).coerceAtLeast(1f)
        val scale = min(previewWidth / screenWidth.coerceAtLeast(1), previewHeight / screenHeight.coerceAtLeast(1))
        val offsetX = padding + (previewWidth - screenWidth * scale) / 2f
        val offsetY = padding + (previewHeight - screenHeight * scale) / 2f
        fun map(point: TracePoint) = TracePoint(offsetX + point.x * scale, offsetY + point.y * scale)

        fillPaint.color = context.color(R.color.mt_surface_muted)
        canvas.drawRoundRect(
            RectF(offsetX, offsetY, offsetX + screenWidth * scale, offsetY + screenHeight * scale),
            18f,
            18f,
            fillPaint,
        )

        val metrics = profile.metrics()
        metrics.overlaySafeZones.forEach { zone ->
            fillPaint.color = context.color(R.color.mt_primary_container).withAlpha(115)
            canvas.drawRect(
                offsetX + zone.left * scale,
                offsetY + zone.top * scale,
                offsetX + zone.right * scale,
                offsetY + zone.bottom * scale,
                fillPaint,
            )
        }

        drawQuad(
            canvas = canvas,
            points = listOf(profile.topLeft, profile.topRight, profile.bottomRight, profile.bottomLeft).map(::map),
            color = context.color(R.color.mt_primary).withAlpha(72),
            strokeColor = context.color(R.color.mt_primary),
        )
        drawQuad(
            canvas = canvas,
            points = listOf(metrics.safeTopLeft, metrics.safeTopRight, metrics.safeBottomRight, metrics.safeBottomLeft).map(::map),
            color = context.color(R.color.mt_success_container).withAlpha(95),
            strokeColor = context.color(R.color.mt_success),
        )
        CalibrationCorner.entries.forEach { corner ->
            val point = map(profile.point(corner))
            fillPaint.color = if (corner == selectedCorner) {
                context.color(R.color.mt_warning)
            } else {
                context.color(R.color.mt_text)
            }
            canvas.drawCircle(point.x, point.y, if (corner == selectedCorner) 8f else 5f, fillPaint)
        }
        if (!profile.isUsable()) {
            canvas.drawText("Calibration missing or incomplete", offsetX + 14f, offsetY + 28f, textPaint)
        }
    }

    private fun drawQuad(canvas: Canvas, points: List<TracePoint>, color: Int, strokeColor: Int) {
        if (points.size != 4) return
        val path = Path().apply {
            moveTo(points[0].x, points[0].y)
            lineTo(points[1].x, points[1].y)
            lineTo(points[2].x, points[2].y)
            lineTo(points[3].x, points[3].y)
            close()
        }
        fillPaint.color = color
        canvas.drawPath(path, fillPaint)
        strokePaint.color = strokeColor
        canvas.drawPath(path, strokePaint)
    }

    private fun Int.withAlpha(alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(this), Color.green(this), Color.blue(this))
}
