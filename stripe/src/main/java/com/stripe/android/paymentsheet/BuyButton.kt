package com.stripe.android.paymentsheet

import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import androidx.core.animation.doOnEnd
import com.stripe.android.R
import com.stripe.android.databinding.PaymentSheetBuyButtonBinding
import com.stripe.android.paymentsheet.model.ViewState
import java.util.Currency
import java.util.Locale

/**
 * Buy button for PaymentSheet.
 */
internal class BuyButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    internal val viewBinding = PaymentSheetBuyButtonBinding.inflate(
        LayoutInflater.from(context),
        this
    )

    private val confirmedIcon = viewBinding.confirmedIcon

    private val currencyFormatter = CurrencyFormatter()

    init {
        setBackgroundResource(R.drawable.stripe_paymentsheet_buy_button_default_background)

        isClickable = true
        isEnabled = false
    }

    fun onReadyState(state: ViewState.Ready) {
        viewBinding.confirmingIcon.visibility = View.GONE

        val currency = Currency.getInstance(
            state.currencyCode.toUpperCase(Locale.ROOT)
        )
        viewBinding.label.text = resources.getString(
            R.string.stripe_paymentsheet_pay_button_amount,
            currencyFormatter.format(state.amount, currency)
        )
    }

    fun onConfirmingState() {
        viewBinding.lockIcon.visibility = View.GONE
        viewBinding.confirmingIcon.visibility = View.VISIBLE

        viewBinding.label.text = resources.getString(
            R.string.stripe_paymentsheet_pay_button_processing
        )
    }

    fun onCompletedState(
        onAnimationEnd: () -> Unit
    ) {
        setBackgroundResource(R.drawable.stripe_paymentsheet_buy_button_confirmed_background)

        fadeOut(viewBinding.label)
        fadeOut(viewBinding.confirmingIcon)

        animateConfirmedIcon(onAnimationEnd)
    }

    private fun animateConfirmedIcon(
        onAnimationEnd: () -> Unit
    ) {
        val iconCenter = confirmedIcon.left + (confirmedIcon.right - confirmedIcon.left) / 2f
        val targetX = iconCenter - width / 2f

        fadeIn(viewBinding.confirmedIcon) {

            // slide the icon to the horizontal center of the view
            ObjectAnimator.ofFloat(
                confirmedIcon,
                "translationX",
                0f,
                -targetX
            ).also { animator ->
                animator.duration =
                    resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
                animator.doOnEnd { onAnimationEnd() }
            }.start()
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)

        viewBinding.label.alpha = if (enabled) {
            ALPHA_ENABLED
        } else {
            ALPHA_DISABLED
        }

        viewBinding.lockIcon.visibility = if (enabled) {
            View.VISIBLE
        } else {
            View.GONE
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
