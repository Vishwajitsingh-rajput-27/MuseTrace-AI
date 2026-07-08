package com.vishwajitrajput.musetraceai.core.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import com.vishwajitrajput.musetraceai.domain.model.TraceLayer
import com.vishwajitrajput.musetraceai.domain.model.TraceProject
import kotlin.math.min

class SketchCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = 2.5f
    }
    private var project: TraceProject? = null
    private var layerIndex: Int? = null
    private var strokeLimit: Int = Int.MAX_VALUE

    fun showProject(project: TraceProject, layerIndex: Int? = null, strokeLimit: Int = Int.MAX_VALUE) {
        this.project = project
        this.layerIndex = layerIndex
        this.strokeLimit = strokeLimit
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val current = project ?: return
        val inset = 18f
        val drawWidth = width - inset * 2f
        val drawHeight = height - inset * 2f
        val scale = min(drawWidth / current.width.coerceAtLeast(1), drawHeight / current.height.coerceAtLeast(1))
        val offsetX = inset + (drawWidth - current.width * scale) / 2f
        val offsetY = inset + (drawHeight - current.height * scale) / 2f

        val layers = layerIndex?.let { index ->
            current.layers.getOrNull(index)?.let(::listOf).orEmpty()
        } ?: current.layers

        layers.forEach { layer -> drawLayer(canvas, layer, offsetX, offsetY, scale) }
    }

    private fun drawLayer(canvas: Canvas, layer: TraceLayer, offsetX: Float, offsetY: Float, scale: Float) {
        paint.color = runCatching { Color.parseColor(layer.colorHex) }.getOrDefault(Color.WHITE)
        paint.alpha = 230
        var drawn = 0
        layer.strokes.forEach { stroke ->
            if (drawn >= strokeLimit) return
            val first = stroke.points.firstOrNull() ?: return@forEach
            val path = Path().apply { moveTo(offsetX + first.x * scale, offsetY + first.y * scale) }
            stroke.points.drop(1).forEach { point ->
                path.lineTo(offsetX + point.x * scale, offsetY + point.y * scale)
            }
            canvas.drawPath(path, paint)
            drawn++
        }
    }
}
