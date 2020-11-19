package com.stripe.android.paymentsheet.ui

import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.animation.doOnEnd
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import com.stripe.android.R
import com.stripe.android.paymentsheet.CurrencyFormatter
import com.stripe.android.paymentsheet.model.ViewState
import java.util.Currency
import java.util.Locale

/**
 * The primary call to action button.
 */
internal abstract class PrimaryButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    abstract val label: TextView
    abstract val lockIcon: View
    abstract val confirmingIcon: View
    abstract val confirmedIcon: View

    private val currencyFormatter = CurrencyFormatter()

    private val mutableCompletedAnimation = MutableLiveData<ViewState.Completed>()
    internal val completedAnimation = mutableCompletedAnimation.distinctUntilChanged()

    fun onReadyState(state: ViewState.Ready) {
        confirmingIcon.visibility = View.GONE

        val currency = Currency.getInstance(
            state.currencyCode.toUpperCase(Locale.ROOT)
        )
        label.text = resources.getString(
            R.string.stripe_paymentsheet_pay_button_amount,
            currencyFormatter.format(state.amount, currency)
        )
    }

    fun onConfirmingState() {
        lockIcon.visibility = View.GONE
        confirmingIcon.visibility = View.VISIBLE

        label.text = resources.getString(
            R.string.stripe_paymentsheet_pay_button_processing
        )
    }

    fun onCompletedState(state: ViewState.Completed) {
        setBackgroundResource(R.drawable.stripe_paymentsheet_buy_button_confirmed_background)

        fadeOut(label)
        fadeOut(confirmingIcon)

        animateConfirmedIcon(state)
    }

    private fun animateConfirmedIcon(state: ViewState.Completed) {
        val iconCenter = confirmedIcon.left + (confirmedIcon.right - confirmedIcon.left) / 2f
        val targetX = iconCenter - width / 2f

        fadeIn(confirmedIcon) {

            // slide the icon to the horizontal center of the view
            ObjectAnimator.ofFloat(
                confirmedIcon,
                "translationX",
                0f,
                -targetX
            ).also { animator ->
                animator.duration =
                    resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
                animator.doOnEnd {
                    mutableCompletedAnimation.value = state
                }
            }.start()
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)

        label.alpha = if (enabled) {
            ALPHA_ENABLED
        } else {
            ALPHA_DISABLED
        }

        lockIcon.visibility = if (enabled) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    fun updateState(state: ViewState) {
        when (state) {
            is ViewState.Ready -> {
                onReadyState(state)
            }
            ViewState.Confirming -> {
                onConfirmingState()
            }
            is ViewState.Completed -> {
                onCompletedState(state)
            }
        }
    }

    private fun fadeIn(view: View, onAnimationEnd: () -> Unit) {
        view.startAnimation(
            AnimationUtils.loadAnimation(
                context,
                R.anim.stripe_paymentsheet_transition_fade_in
            ).also { animation ->
                animation.setAnimationListener(
                    object : Animation.AnimationListener {
                        override fun onAnimationStart(p0: Animation?) {
                            view.visibility = View.VISIBLE
                        }

                        override fun onAnimationEnd(p0: Animation?) {
                            view.visibility = View.VISIBLE
                            onAnimationEnd()
                        }

                        override fun onAnimationRepeat(p0: Animation?) {
                        }
                    }
                )
            }
        )
    }

    private fun fadeOut(view: View) {
        view.startAnimation(
            AnimationUtils.loadAnimation(
                context,
                R.anim.stripe_paymentsheet_transition_fade_out
            ).also { animation ->
                animation.setAnimationListener(
                    object : Animation.AnimationListener {
                        override fun onAnimationStart(p0: Animation?) {
                            view.visibility = View.VISIBLE
                        }

                        override fun onAnimationEnd(p0: Animation?) {
                            view.visibility = View.INVISIBLE
                        }

                        override fun onAnimationRepeat(p0: Animation?) {
                        }
                    }
                )
            }
        )
    }

    private companion object {
        private const val ALPHA_ENABLED = 1.0f
        private const val ALPHA_DISABLED = 0.5f
    }
}
