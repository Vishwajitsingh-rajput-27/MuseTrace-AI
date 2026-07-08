package com.vishwajitrajput.musetraceai.core.ui

import android.content.Context
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.vishwajitrajput.musetraceai.R

fun Context.menuButton(text: String, onClick: () -> Unit): SfGhostButton =
    SfGhostButton(this).apply {
        this.text = text
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply { bottomMargin = resources.getDimensionPixelSize(R.dimen.space_8) }
    }

fun Context.infoCard(title: String, body: String): SfInfoCard =
    SfInfoCard(this).apply {
        val padding = resources.getDimensionPixelSize(R.dimen.space_16)
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }
        container.addView(TextView(context).apply {
            text = title
            setTextColor(context.color(R.color.mt_text))
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        container.addView(TextView(context).apply {
            text = body
            setTextColor(context.color(R.color.mt_text_secondary))
            textSize = 14f
        })
        addView(container)
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply { bottomMargin = resources.getDimensionPixelSize(R.dimen.space_12) }
    }

fun Context.warningCard(title: String, body: String): SfWarningCard =
    SfWarningCard(this).apply {
        val padding = resources.getDimensionPixelSize(R.dimen.space_16)
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }
        container.addView(TextView(context).apply {
            text = title
            setTextColor(context.color(R.color.mt_text))
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        container.addView(TextView(context).apply {
            text = body
            setTextColor(context.color(R.color.mt_text_secondary))
            textSize = 14f
        })
        addView(container)
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply { bottomMargin = resources.getDimensionPixelSize(R.dimen.space_12) }
    }
