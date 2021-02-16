package com.stripe.android.paymentsheet

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.core.view.isVisible
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import com.stripe.android.R
import com.stripe.android.databinding.PaymentSheetBuyButtonBinding
import com.stripe.android.paymentsheet.model.ViewState
import com.stripe.android.paymentsheet.ui.PrimaryButton
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
) : PrimaryButton(context, attrs, defStyleAttr) {
    private val animator = PrimaryButtonAnimator(context)

    internal val viewBinding = PaymentSheetBuyButtonBinding.inflate(
        LayoutInflater.from(context),
        this
    )

    private val confirmedIcon = viewBinding.confirmedIcon

    private val currencyFormatter = CurrencyFormatter()

    private val _completedAnimation = MutableLiveData<ViewState.Completed>()
    internal val completedAnimation = _completedAnimation.distinctUntilChanged()

    private var viewState: ViewState? = null

    init {
        setBackgroundResource(R.drawable.stripe_paymentsheet_buy_button_default_background)

        isClickable = true
        isEnabled = false
    }

    fun onReadyState(state: ViewState.Ready) {
        viewBinding.confirmingIcon.isVisible = false

        val currency = Currency.getInstance(
            state.currencyCode.toUpperCase(Locale.ROOT)
        )
        viewBinding.label.text = resources.getString(
            R.string.stripe_paymentsheet_pay_button_amount,
            currencyFormatter.format(state.amount, currency)
        )
    }

    fun onConfirmingState() {
        viewBinding.lockIcon.isVisible = false
        viewBinding.confirmingIcon.isVisible = true

        viewBinding.label.text = resources.getString(
            R.string.stripe_paymentsheet_pay_button_processing
        )
    }

    private fun onCompletedState(state: ViewState.Completed) {
        setBackgroundResource(R.drawable.stripe_paymentsheet_buy_button_confirmed_background)

        animator.fadeOut(viewBinding.label)
        animator.fadeOut(viewBinding.confirmingIcon)

        animateConfirmedIcon(state)
    }

    private fun animateConfirmedIcon(state: ViewState.Completed) {
        animator.fadeIn(confirmedIcon, width) {
            _completedAnimation.value = state
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        viewBinding.lockIcon.isVisible = enabled
        updateAlpha()
    }

    fun updateState(viewState: ViewState) {
        this.viewState = viewState
        updateAlpha()

        when (viewState) {
            is ViewState.Ready -> {
                onReadyState(viewState)
            }
            ViewState.Confirming -> {
                onConfirmingState()
            }
            is ViewState.Completed -> {
                onCompletedState(viewState)
            }
        }
    }

    private fun updateAlpha() {
        if ((viewState == null || viewState is ViewState.Ready) && !isEnabled) {
            viewBinding.label.alpha = 0.5f
        } else {
            viewBinding.label.alpha = 1.0f
        }
    }
}
