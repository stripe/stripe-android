package com.stripe.android.paymentsheet.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.stripe.android.R
import com.stripe.android.model.PaymentMethod

/**
 * Enum defining all payment methods supported in Payment Sheet.
 */
internal enum class SupportedPaymentMethod(
    val paymentMethodType: PaymentMethod.Type,
    @StringRes val displayNameResource: Int,
    @DrawableRes val iconResource: Int
) {
    Card(
        PaymentMethod.Type.Card,
        R.string.stripe_paymentsheet_payment_method_card,
        R.drawable.stripe_ic_paymentsheet_add_pm_card
    ),
    Ideal(
        PaymentMethod.Type.Ideal,
        R.string.stripe_paymentsheet_payment_method_ideal,
        R.drawable.stripe_ic_paymentsheet_add_pm_ideal
    );

    override fun toString(): String {
        return paymentMethodType.toString()
    }

    companion object {
        internal fun fromCode(code: String?): SupportedPaymentMethod? {
            return values().firstOrNull { it.paymentMethodType.code == code }
        }
    }
}
