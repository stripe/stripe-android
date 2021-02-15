package com.stripe.android.paymentsheet

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.stripe.android.R
import com.stripe.android.databinding.PaymentSheetAddButtonBinding
import com.stripe.android.paymentsheet.ui.PrimaryButton

/**
 * "Add" button for [PaymentOptionsActivity].
 */
internal class AddButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : PrimaryButton(context, attrs, defStyleAttr) {
    internal val viewBinding = PaymentSheetAddButtonBinding.inflate(
        LayoutInflater.from(context),
        this
    )

    init {
        setBackgroundResource(R.drawable.stripe_paymentsheet_buy_button_default_background)

        isClickable = true
        isEnabled = false
        isGone = true
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        viewBinding.lockIcon.isVisible = enabled

        viewBinding.label.alpha = if (isEnabled) {
            1.0f
        } else {
            0.5f
        }
    }
}
