package com.stripe.android.paymentsheet

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.stripe.android.R
import com.stripe.android.databinding.PaymentSheetBuyButtonBinding

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

    init {
        setBackgroundResource(R.drawable.stripe_paymentsheet_buy_button_default_background)

        isClickable = true
        isEnabled = true
    }

    fun updateText(text: String) {
        viewBinding.label.text = text
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

    private companion object {
        private const val ALPHA_ENABLED = 1.0f
        private const val ALPHA_DISABLED = 0.5f
    }
}
