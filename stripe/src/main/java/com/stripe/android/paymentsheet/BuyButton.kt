package com.stripe.android.paymentsheet

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import com.stripe.android.R
import com.stripe.android.databinding.PaymentSheetBuyButtonBinding
import com.stripe.android.paymentsheet.model.ViewState
import com.stripe.android.paymentsheet.ui.PrimaryButtonAnimator
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
    private val animator = PrimaryButtonAnimator(context)

    internal val viewBinding = PaymentSheetBuyButtonBinding.inflate(
        LayoutInflater.from(context),
        this
    )

    private val confirmedIcon = viewBinding.confirmedIcon

    private val currencyFormatter = CurrencyFormatter()

    private val mutableCompletedAnimation = MutableLiveData<ViewState.Completed>()
    internal val completedAnimation = mutableCompletedAnimation.distinctUntilChanged()

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

    fun onCompletedState(state: ViewState.Completed) {
        setBackgroundResource(R.drawable.stripe_paymentsheet_buy_button_confirmed_background)

        animator.fadeOut(viewBinding.label)
        animator.fadeOut(viewBinding.confirmingIcon)

        animateConfirmedIcon(state)
    }

    private fun animateConfirmedIcon(state: ViewState.Completed) {
        animator.fadeIn(confirmedIcon, width) {
            mutableCompletedAnimation.value = state
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

    private companion object {
        private const val ALPHA_ENABLED = 1.0f
        private const val ALPHA_DISABLED = 0.5f
    }
}
