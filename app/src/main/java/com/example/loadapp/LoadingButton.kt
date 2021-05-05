package com.example.loadapp

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import kotlin.properties.Delegates

class LoadingButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var widthSize = 0
    private var heightSize = 0

    // Set up paints for canvas drawing.
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.colorPrimary)
    }
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.white)
        // Set the number text size to be 24sp.
        // Translate 24sp
        textSize = Math.round(24f * resources.displayMetrics.scaledDensity).toFloat()
    }

    // Allocate objects needed for canvas drawing here.
    private val backgroundRect = RectF();

    // Initialize drawing measurements.
    private val cornerRadius = Math.round(7f * resources.displayMetrics.density);

    private val valueAnimator = ValueAnimator.ofFloat(0f, -(width.toFloat()))

    private var buttonState: ButtonState by Delegates.observable<ButtonState>(ButtonState.Completed) { p, old, new ->
        when (new) {
            ButtonState.Clicked -> startButtonAnimation()
            ButtonState.Loading -> continueButtonAnimation()
            ButtonState.Completed -> stopButtonAnimation()
        }
    }

    private fun stopButtonAnimation() {
        TODO("Not yet implemented")
    }

    private fun continueButtonAnimation() {
        TODO("Not yet implemented")
    }

    private fun startButtonAnimation() {
        TODO("Not yet implemented")
    }

    private val displayedText = resources.getString(R.string.button_name)

    private val path = Path()

    init {
        isClickable = true
    }


    override fun performClick(): Boolean {
        if (super.performClick()) return true
        invalidate()
        return true
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Grab canvas dimensions.
        val canvasWidth = width
        val canvasHeight = height

        // Calculate horizontal center.
        val centerX = canvasWidth * 0.5f

        // Calculate the baseline for the text
        val baselineY = Math.round(canvasHeight * 0.6f);

        // Draw the background.
        backgroundRect.set(0f, 0f, canvasWidth.toFloat(), canvasHeight.toFloat());
        canvas.drawRoundRect(backgroundRect, cornerRadius.toFloat(),
            cornerRadius.toFloat(), backgroundPaint);

        // Draw text.

        // Measure the width of text to display.
        val textWidth = textPaint.measureText(displayedText);
        // Figure out an x-coordinate that will center the text in the canvas.
        val textX = Math.round(centerX - textWidth * 0.5f);
        // Draw.
        canvas.drawText(displayedText, textX.toFloat(), baselineY.toFloat(), textPaint);
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minw: Int = paddingLeft + paddingRight + suggestedMinimumWidth
        val w: Int = resolveSizeAndState(minw, widthMeasureSpec, 1)
        val h: Int = resolveSizeAndState(
            MeasureSpec.getSize(w),
            heightMeasureSpec,
            0
        )
        widthSize = w
        heightSize = h
        setMeasuredDimension(w, h)
    }

}