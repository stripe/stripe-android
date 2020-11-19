package com.stripe.android.paymentsheet.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.stripe.android.R
import com.stripe.android.databinding.PaymentSheetBuyButtonBinding

/**
 * Buy button for payment sheet.
 */
internal class BuyButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : PrimaryButton(context, attrs, defStyleAttr) {
    private val viewBinding = PaymentSheetBuyButtonBinding.inflate(
        LayoutInflater.from(context),
        this
    )

    override val label: TextView = viewBinding.label
    override val lockIcon: View = viewBinding.lockIcon
    override val confirmingIcon: View = viewBinding.confirmingIcon
    override val confirmedIcon: View = viewBinding.confirmedIcon

    init {
        setBackgroundResource(R.drawable.stripe_paymentsheet_buy_button_default_background)
        isClickable = true
        isEnabled = false
    }
}
