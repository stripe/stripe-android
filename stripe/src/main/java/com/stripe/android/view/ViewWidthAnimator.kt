package com.stripe.android.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.View

internal class ViewWidthAnimator(
    private val view: View,
    private val duration: Long
) {
    /**
     * @param startWidth the starting width of the View
     * @param endWidth the ending width of the View after animation completes
     * @param onAnimationEnd callback to invoke when animation completes
     */
    fun animate(startWidth: Int, endWidth: Int, onAnimationEnd: () -> Unit) {
        ValueAnimator.ofInt(startWidth, endWidth)
            .setDuration(duration).also {
                it.addUpdateListener { valueAnimator ->
                    val width = valueAnimator.animatedValue as Int
                    view.layoutParams = view.layoutParams.also { layoutParams ->
                        layoutParams.width = width
                    }
                }

                it.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        super.onAnimationEnd(animation)

                        view.layoutParams = view.layoutParams.also { layoutParams ->
                            layoutParams.width = endWidth
                        }

                        onAnimationEnd()
                    }
                })
            }.start()
    }
}
