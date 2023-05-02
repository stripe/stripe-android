package com.stripe.android.paymentsheet.ui

import android.animation.ObjectAnimator
import android.content.Context
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.core.animation.doOnEnd
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.stripe.android.paymentsheet.R
import com.stripe.android.R as StripeR

internal class PrimaryButtonAnimator(
    private val context: Context
) {
    private val slideAnimationDuration = context.resources
        .getInteger(android.R.integer.config_shortAnimTime)
        .toLong()

    internal fun fadeIn(
        view: View,
        parentWidth: Int,
        onAnimationEnd: () -> Unit
    ) {
        view.startAnimation(
            AnimationUtils.loadAnimation(
                context,
                StripeR.anim.stripe_paymentsheet_transition_fade_in
            ).also { animation ->
                animation.setAnimationListener(
                    object : Animation.AnimationListener {
                        override fun onAnimationStart(p0: Animation?) {
                            view.isVisible = true
                        }

                        override fun onAnimationEnd(p0: Animation?) {
                            view.isVisible = true

                            slideToCenter(view, parentWidth, onAnimationEnd)
                        }

                        override fun onAnimationRepeat(p0: Animation?) {
                        }
                    }
                )
            }
        )
    }

    // slide the view to the horizontal center of the parent view
    private fun slideToCenter(
        view: View,
        parentWidth: Int,
        onAnimationEnd: () -> Unit
    ) {
        val iconCenter = view.left + (view.right - view.left) / 2f
        val targetX = iconCenter - parentWidth / 2f

        ObjectAnimator.ofFloat(
            view,
            "translationX",
            0f,
            -targetX
        ).also { animator ->
            animator.duration = slideAnimationDuration
            animator.doOnEnd {
                delay(view, onAnimationEnd)
            }
        }.start()
    }

    private fun delay(
        view: View,
        onAnimationEnd: () -> Unit
    ) {
        // This is effectively a no-op for ANIMATE_OUT_MILLIS
        ObjectAnimator.ofFloat(
            view,
            "rotation",
            0f,
            0f
        ).also { animator ->
            animator.duration = HOLD_ANIMATION_ON_SLIDE_IN_COMPLETION
            animator.doOnEnd {
                onAnimationEnd()
            }
        }.start()
    }

    internal fun fadeOut(view: View) {
        view.startAnimation(
            AnimationUtils.loadAnimation(
                context,
                StripeR.anim.stripe_paymentsheet_transition_fade_out
            ).also { animation ->
                animation.setAnimationListener(
                    object : Animation.AnimationListener {
                        override fun onAnimationStart(p0: Animation?) {
                            view.isVisible = true
                        }

                        override fun onAnimationEnd(p0: Animation?) {
                            view.isInvisible = true
                        }

                        override fun onAnimationRepeat(p0: Animation?) {
                        }
                    }
                )
            }
        )
    }

    internal companion object {
        // the delay before the payment sheet is dismissed
        const val HOLD_ANIMATION_ON_SLIDE_IN_COMPLETION = 1500L
    }
}
