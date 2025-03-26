package com.stripe.android.connect.webview

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.core.widget.ImageViewCompat
import com.stripe.android.connect.R
import com.stripe.android.connect.util.AndroidClock
import com.stripe.android.connect.util.Clock

/**
 * A spinner that mimics the loading spinner used in web components.
 */
internal class StripeWebViewSpinner @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    clock: Clock = AndroidClock(),
) : FrameLayout(context, attrs, defStyleAttr) {

    private val image: ImageView
    private val animator: ValueAnimator

    init {
        LayoutInflater.from(context).inflate(R.layout.stripe_web_view_spinner, this, true)
        image = findViewById(R.id.image)
        animator = ValueAnimator.ofFloat(0f, 1f)
            .apply {
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.RESTART
                addUpdateListener { _ ->
                    // Rotation is deterministic based on the current time in order to sync with
                    // the web spinner for a more seamless transition.
                    val rotationPercent =
                        (clock.millis() % MILLIS_PER_ROTATION) / MILLIS_PER_ROTATION.toFloat()
                    image.rotation = MAX_DEGREES * rotationPercent
                }
            }
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        updateAnimatorBasedOnState()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateAnimatorBasedOnState()
    }

    override fun onDetachedFromWindow() {
        updateAnimatorBasedOnState()
        super.onDetachedFromWindow()
    }

    fun setColor(@ColorInt color: Int) {
        ImageViewCompat.setImageTintList(image, ColorStateList.valueOf(color))
    }

    private fun updateAnimatorBasedOnState() {
        if (isAttachedToWindow && visibility == View.VISIBLE) {
            animator.start()
        } else {
            animator.cancel()
        }
    }

    companion object {
        private const val MAX_DEGREES = 360f
        private const val MILLIS_PER_ROTATION = 700L
    }
}
