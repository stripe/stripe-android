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
import com.stripe.android.paymentsheet.model.PaymentOptionViewState
import com.stripe.android.paymentsheet.ui.PrimaryButtonAnimator

/**
 * "Add" button for [PaymentOptionsActivity].
 */
internal class AddButton @JvmOverloads constructor(
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

    private val mutableCompletedAnimation = MutableLiveData<PaymentOptionViewState.Completed>()
    internal val completedAnimation = mutableCompletedAnimation.distinctUntilChanged()

    init {
        setBackgroundResource(R.drawable.stripe_paymentsheet_buy_button_default_background)

        isClickable = true
        isEnabled = false

        viewBinding.label.text = resources.getString(
            R.string.stripe_paymentsheet_add_button_label,
        )
    }

    fun onReadyState() {
        viewBinding.confirmingIcon.visibility = View.GONE
    }

    fun onProcessingState() {
        viewBinding.lockIcon.visibility = View.GONE
        viewBinding.confirmingIcon.visibility = View.VISIBLE

        viewBinding.label.text = resources.getString(
            R.string.stripe_paymentsheet_pay_button_processing
        )
    }

    fun onCompletedState(state: PaymentOptionViewState.Completed) {
        viewBinding.lockIcon.visibility = View.GONE
        viewBinding.confirmingIcon.visibility = View.VISIBLE

        setBackgroundResource(R.drawable.stripe_paymentsheet_buy_button_confirmed_background)

        animator.fadeOut(viewBinding.label)
        animator.fadeOut(viewBinding.confirmingIcon)

        animateConfirmedIcon(state)
    }

    private fun animateConfirmedIcon(state: PaymentOptionViewState.Completed) {
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

    fun updateState(state: PaymentOptionViewState) {
        when (state) {
            PaymentOptionViewState.Ready -> {
                onReadyState()
            }
            is PaymentOptionViewState.Completed -> {
                onCompletedState(state)
            }
        }
    }

    private companion object {
        private const val ALPHA_ENABLED = 1.0f
        private const val ALPHA_DISABLED = 0.5f
    }
}
