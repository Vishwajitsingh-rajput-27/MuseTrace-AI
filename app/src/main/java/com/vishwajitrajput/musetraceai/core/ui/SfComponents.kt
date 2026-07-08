package com.vishwajitrajput.musetraceai.core.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputLayout
import com.vishwajitrajput.musetraceai.R

open class SfCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.materialCardViewStyle,
) : MaterialCardView(context, attrs, defStyleAttr) {
    init {
        setCardBackgroundColor(context.color(R.color.mt_card))
        strokeColor = context.color(R.color.mt_border)
        strokeWidth = context.dimenPx(R.dimen.sf_border_width)
        radius = context.resources.getDimension(R.dimen.card_radius)
        cardElevation = context.resources.getDimension(R.dimen.space_2)
        useCompatPadding = false
    }
}

class SfInfoCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : SfCard(context, attrs) {
    init {
        setCardBackgroundColor(context.color(R.color.mt_card_alt))
    }
}

open class SfWarningCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : SfCard(context, attrs) {
    init {
        setCardBackgroundColor(context.color(R.color.mt_warning_container))
        strokeColor = context.color(R.color.mt_warning)
    }
}

class SfOverlayCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : SfCard(context, attrs) {
    init {
        setCardBackgroundColor(context.color(R.color.mt_scrim))
        radius = context.resources.getDimension(R.dimen.overlay_radius)
        cardElevation = context.resources.getDimension(R.dimen.space_8)
    }
}

open class SfButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.materialButtonStyle,
) : MaterialButton(context, attrs, defStyleAttr) {
    init {
        minimumHeight = context.dimenPx(R.dimen.sf_button_height)
        cornerRadius = context.dimenPx(R.dimen.pill_radius)
        backgroundTintList = context.tint(R.color.mt_primary)
        setTextColor(context.color(R.color.mt_on_primary))
        setAllCaps(false)
        insetTop = 0
        insetBottom = 0
    }
}

class SfGhostButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : SfButton(context, attrs, com.google.android.material.R.attr.materialButtonOutlinedStyle) {
    init {
        backgroundTintList = context.tint(R.color.mt_surface_muted)
        strokeColor = context.tint(R.color.mt_border)
        strokeWidth = context.dimenPx(R.dimen.sf_border_width)
        setTextColor(context.color(R.color.mt_text))
    }
}

class SfDangerButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : SfButton(context, attrs) {
    init {
        backgroundTintList = context.tint(R.color.mt_danger)
        setTextColor(context.color(R.color.mt_on_primary))
    }
}

class SfTextField @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : TextInputLayout(context, attrs, com.google.android.material.R.attr.textInputStyle) {
    init {
        boxBackgroundMode = BOX_BACKGROUND_OUTLINE
        setBoxBackgroundColor(context.color(R.color.mt_card))
        boxStrokeColor = context.color(R.color.mt_border)
        setBoxCornerRadii(
            context.resources.getDimension(R.dimen.card_radius_compact),
            context.resources.getDimension(R.dimen.card_radius_compact),
            context.resources.getDimension(R.dimen.card_radius_compact),
            context.resources.getDimension(R.dimen.card_radius_compact),
        )
    }
}

class SfSlider @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : Slider(context, attrs) {
    init {
        thumbTintList = context.tint(R.color.mt_primary)
        trackActiveTintList = context.tint(R.color.mt_primary)
        trackInactiveTintList = context.tint(R.color.mt_border)
        haloTintList = context.tint(R.color.mt_primary_container)
    }
}

class SfChip @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : Chip(context, attrs) {
    init {
        chipBackgroundColor = context.tint(R.color.mt_card_alt)
        chipStrokeColor = context.tint(R.color.mt_border)
        chipStrokeWidth = context.resources.getDimension(R.dimen.sf_border_width)
        setTextColor(context.color(R.color.mt_text))
    }
}

class SfTopBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : MaterialToolbar(context, attrs) {
    init {
        setBackgroundColor(context.color(R.color.mt_background))
        setTitleTextColor(context.color(R.color.mt_text))
        setSubtitleTextColor(context.color(R.color.mt_text_secondary))
        setNavigationIconTint(context.color(R.color.mt_text))
        elevation = 0f
    }
}

class SfSectionHeader @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {
    private val titleView = TextView(context)
    private val bodyView = TextView(context)

    init {
        orientation = VERTICAL
        val vertical = context.dimenPx(R.dimen.space_8)
        setPadding(0, vertical, 0, vertical)
        titleView.setTextColor(context.color(R.color.mt_text))
        titleView.textSize = 20f
        titleView.setTypeface(titleView.typeface, Typeface.BOLD)
        bodyView.setTextColor(context.color(R.color.mt_text_secondary))
        bodyView.textSize = 14f
        addView(
            titleView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
        addView(
            bodyView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
    }

    fun bind(title: String, body: String = "") {
        titleView.text = title
        bodyView.text = body
        bodyView.visibility = if (body.isBlank()) GONE else VISIBLE
    }
}

class SfColorSwatch @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = context.resources.getDimension(R.dimen.sf_border_width)
        color = context.color(R.color.mt_border_strong)
    }
    private var swatchColor = Color.WHITE

    fun setColorHex(colorHex: String) {
        swatchColor = runCatching { Color.parseColor(colorHex) }.getOrDefault(Color.WHITE)
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = context.dimenPx(R.dimen.space_32)
        setMeasuredDimension(resolveSize(size, widthMeasureSpec), resolveSize(size, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radius = width.coerceAtMost(height) / 2f
        paint.color = swatchColor
        canvas.drawCircle(width / 2f, height / 2f, radius - borderPaint.strokeWidth, paint)
        canvas.drawCircle(width / 2f, height / 2f, radius - borderPaint.strokeWidth, borderPaint)
    }
}

open class SfProgressCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : SfCard(context, attrs) {
    private val messageView = TextView(context)

    init {
        val padding = context.dimenPx(R.dimen.space_16)
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(padding, padding, padding, padding)
            addView(ProgressBar(context), LinearLayout.LayoutParams(padding * 2, padding * 2))
            addView(messageView, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = padding
            })
        }
        messageView.setTextColor(context.color(R.color.mt_text_secondary))
        messageView.textSize = 14f
        addView(row)
    }

    fun setMessage(message: String) {
        messageView.text = message
    }
}

class SfEmptyState @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {
    private val titleView = TextView(context)
    private val bodyView = TextView(context)

    init {
        orientation = VERTICAL
        gravity = android.view.Gravity.CENTER
        val padding = context.dimenPx(R.dimen.space_24)
        setPadding(padding, padding, padding, padding)
        background = ContextCompat.getDrawable(context, R.drawable.bg_preview)
        titleView.setTextColor(context.color(R.color.mt_text))
        titleView.textSize = 18f
        titleView.setTypeface(titleView.typeface, Typeface.BOLD)
        bodyView.setTextColor(context.color(R.color.mt_text_secondary))
        bodyView.textSize = 14f
        bodyView.gravity = android.view.Gravity.CENTER
        addView(
            titleView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
        addView(
            bodyView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
    }

    fun bind(title: String, body: String) {
        titleView.text = title
        bodyView.text = body
    }
}

class SfLoadingState @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : SfProgressCard(context, attrs)

class SfErrorState @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : SfWarningCard(context, attrs) {
    init {
        val padding = context.dimenPx(R.dimen.space_16)
        setContentPadding(padding, padding, padding, padding)
    }
}

class SfBottomSheet(private val sheetContext: Context) : BottomSheetDialog(sheetContext) {
    fun setActionContent(
        title: String,
        body: String,
        actionText: String,
        onAction: () -> Unit,
    ) {
        val padding = sheetContext.dimenPx(R.dimen.space_20)
        val content = LinearLayout(sheetContext).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
            addView(TextView(sheetContext).apply {
                text = title
                setTextColor(sheetContext.color(R.color.mt_text))
                textSize = 20f
                setTypeface(typeface, Typeface.BOLD)
            })
            addView(TextView(sheetContext).apply {
                text = body
                setTextColor(sheetContext.color(R.color.mt_text_secondary))
                textSize = 14f
            })
            addView(SfButton(sheetContext).apply {
                text = actionText
                setOnClickListener {
                    dismiss()
                    onAction()
                }
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = padding
            })
        }
        setContentView(content)
    }

    fun setActionsContent(
        title: String,
        body: String,
        actions: List<SfSheetAction>,
    ) {
        val padding = sheetContext.dimenPx(R.dimen.space_20)
        val content = LinearLayout(sheetContext).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
            addView(TextView(sheetContext).apply {
                text = title
                setTextColor(sheetContext.color(R.color.mt_text))
                textSize = 20f
                setTypeface(typeface, Typeface.BOLD)
            })
            addView(TextView(sheetContext).apply {
                text = body
                setTextColor(sheetContext.color(R.color.mt_text_secondary))
                textSize = 14f
            })
            actions.forEach { action ->
                val button = when (action.style) {
                    SfSheetActionStyle.Primary -> SfButton(sheetContext)
                    SfSheetActionStyle.Secondary -> SfGhostButton(sheetContext)
                    SfSheetActionStyle.Danger -> SfDangerButton(sheetContext)
                }.apply {
                    text = action.text
                    setOnClickListener {
                        dismiss()
                        action.onClick()
                    }
                }
                addView(button, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = sheetContext.dimenPx(R.dimen.space_8)
                })
            }
        }
        setContentView(content)
    }
}

data class SfSheetAction(
    val text: String,
    val style: SfSheetActionStyle = SfSheetActionStyle.Secondary,
    val onClick: () -> Unit,
)

enum class SfSheetActionStyle {
    Primary,
    Secondary,
    Danger,
}

internal fun Context.color(colorRes: Int): Int = ContextCompat.getColor(this, colorRes)

internal fun Context.tint(colorRes: Int): ColorStateList = ColorStateList.valueOf(color(colorRes))

internal fun Context.dimenPx(dimenRes: Int): Int = resources.getDimensionPixelSize(dimenRes)
