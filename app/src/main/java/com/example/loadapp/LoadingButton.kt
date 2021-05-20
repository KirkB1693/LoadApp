package com.example.loadapp

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.withStyledAttributes
import kotlin.math.roundToInt
import kotlin.properties.Delegates

class LoadingButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var widthSize = 0
    private var heightSize = 0

    // Allocate objects needed for canvas drawing here.
    private val backgroundRect = RectF()
    private val ovalLocation = RectF()

    // Initialize drawing measurements.
    private val cornerRadius = (7f * resources.displayMetrics.density).roundToInt()

    private val valueAnimatorForWidth = ValueAnimator()
    private val valueAnimatorForAngle = ValueAnimator()

    private var animatedWidth: Float = 0f
    private var animatedAngle: Float = 0f
    private var animatedStartWidth: Float = 0f
    private var animatedStartAngle: Float = 0f
    private var oldPercentage: Float = 0f

    private var displayedText = resources.getString(R.string.button_name)

    private var loadingPercentage = 0f

    private var buttonState: ButtonState by Delegates.observable(ButtonState.Completed) { _, _, new ->
        when (new) {
            ButtonState.Clicked -> startButtonAnimation()
            ButtonState.Loading -> continueButtonAnimation()
            ButtonState.Completed -> stopButtonAnimation()
        }
    }

    private fun stopButtonAnimation() {
        displayedText = resources.getString(R.string.button_name)
        invalidate()
    }

    private fun continueButtonAnimation() {
        displayedText = resources.getString(R.string.button_loading)
        // set a small percentage just to show user something is happening even if current download percentage is less
        setLoadingPercentage(.03f)
    }

    private fun animateCircle(progress: Float) {
        valueAnimatorForAngle.apply {
            setFloatValues(animatedStartAngle, 360f)
            interpolator = DecelerateInterpolator()
        }

        valueAnimatorForAngle.addUpdateListener {
            val interpolation = it.animatedFraction
            animatedAngle = 360f * interpolation * progress
            invalidate()
        }

        valueAnimatorForAngle.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                super.onAnimationEnd(animation)
                animatedStartAngle = 360f * progress
            }
        })

    }

    private fun animateLoading(progress: Float) {
        valueAnimatorForWidth.apply {
            setFloatValues(animatedStartWidth, width.toFloat())
            interpolator = DecelerateInterpolator()
            invalidate()
        }

        valueAnimatorForWidth.addUpdateListener {
            val interpolation = it.animatedFraction
            animatedWidth = width * interpolation * progress
        }

        valueAnimatorForWidth.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                super.onAnimationEnd(animation)
                animatedStartWidth = width * progress
            }
        })

    }

    private fun startButtonAnimation() {
        displayedText = resources.getString(R.string.button_loading)
        animatedStartWidth = 0f
        animatedStartAngle = 0f
        animatedWidth = 0f
        animatedAngle = 0f
        loadingPercentage = 0f
        oldPercentage = 0f
        invalidate()
    }

    private var buttonBackgroundColor = 0
    private var buttonLoadingColor = 0
    private var buttonLoadingCircleColor = 0
    private var buttonTextColor = 0

    init {
        isClickable = true
        context.withStyledAttributes(attrs, R.styleable.LoadingButton) {
            buttonBackgroundColor = getColor(R.styleable.LoadingButton_buttonColor, 0)
            buttonLoadingColor = getColor(R.styleable.LoadingButton_loadingColor, 0)
            buttonLoadingCircleColor = getColor(R.styleable.LoadingButton_circleColor, 0)
            buttonTextColor = getColor(R.styleable.LoadingButton_buttonTextColor, 0)
        }
        buttonState = ButtonState.Completed
    }

    // Set up paints for canvas drawing.
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = buttonBackgroundColor
    }
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = buttonTextColor
        // Set the number text size to be 24sp.
        // Translate 24sp
        textSize = ((24f * resources.displayMetrics.scaledDensity).roundToInt()).toFloat()
    }
    private val loadingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = buttonLoadingColor
    }
    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = buttonLoadingCircleColor
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (buttonState == ButtonState.Completed) {
            drawBaseButton(canvas)
        } else {
            drawLoadingBackground(canvas)
            drawLoadingAnimation(canvas)
            // draw text after animation so text stays visible
            drawText(canvas)
            drawLoadingCircle(canvas)
        }

    }

    private fun drawLoadingBackground(canvas: Canvas) {
        // Draw the background.
        backgroundRect.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(
            backgroundRect, cornerRadius.toFloat(),
            cornerRadius.toFloat(), backgroundPaint
        )
    }

    private fun drawLoadingAnimation(canvas: Canvas) {
        canvas.drawRoundRect(0f, 0f, animatedStartWidth, height.toFloat(),
            cornerRadius.toFloat(), cornerRadius.toFloat(), loadingPaint)
        if (animatedStartWidth >= cornerRadius.toFloat()) {
            canvas.drawRoundRect(
                animatedStartWidth - cornerRadius.toFloat(), 0f, animatedWidth, height.toFloat(),
                cornerRadius.toFloat(), cornerRadius.toFloat(), loadingPaint
            )
        } else {
            canvas.drawRoundRect(
                animatedStartWidth , 0f, animatedWidth, height.toFloat(),
                cornerRadius.toFloat(), cornerRadius.toFloat(), loadingPaint
            )
        }

    }

    private fun drawText(canvas: Canvas) {
        // Calculate horizontal center.
        val centerX = width * 0.5f

        // Calculate the baseline for the text
        val baselineY = (height * 0.6f).roundToInt()

        // Measure the width of text to display.
        val textWidth = textPaint.measureText(displayedText)
        // Figure out an x-coordinate that will center the text in the canvas.
        val textX = (centerX - textWidth * 0.5f).roundToInt()
        // Draw.
        canvas.drawText(displayedText, textX.toFloat(), baselineY.toFloat(), textPaint)

        ovalLocation.set((centerX + textWidth * 0.5f + 4), (heightSize * 0.2).toFloat(), (centerX + textWidth * 0.5f + 4)+(heightSize * 0.6).toFloat(), (heightSize * 0.8).toFloat())
    }

    private fun drawLoadingCircle(canvas: Canvas) {
        //draw Arc
        canvas.drawArc(
            ovalLocation,
            0f,
            animatedStartAngle,
            true,
            circlePaint
        )
        //draw Arc
        canvas.drawArc(
            ovalLocation,
            animatedStartAngle,
            animatedAngle-animatedStartAngle,
            true,
            circlePaint
        )
    }

    private fun drawBaseButton(canvas: Canvas) {
        // Draw the background.
        backgroundRect.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(
            backgroundRect, cornerRadius.toFloat(),
            cornerRadius.toFloat(), backgroundPaint
        )

        drawText(canvas)
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

    fun setCustomButtonState(state: ButtonState) {
        buttonState = state
    }

    fun setLoadingPercentage(percentage: Float) {
        var adjustedPercentage = 0.04f
        if (percentage > 0.04f) {
            adjustedPercentage = percentage
        }
        setLoadingPercentage(adjustedPercentage, true)
    }

    private fun setLoadingPercentage(percentage: Float, animate: Boolean) {

        if (animate) {
            if (percentage > oldPercentage) {
                animateLoading(percentage)
                animateCircle(percentage)
                oldPercentage = percentage
                val set = AnimatorSet()
                set.playTogether(valueAnimatorForAngle, valueAnimatorForWidth)
                set.duration = 500L
                if (!set.isStarted) {
                    set.start()
                }
            }
        } else {
            loadingPercentage = percentage
            postInvalidate()
        }
    }

}