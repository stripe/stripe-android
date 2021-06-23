package com.stripe.android.paymentsheet.model

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.stripe.android.R
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.specifications.FormType
import kotlinx.parcelize.Parcelize

/**
 * Enum defining all payment methods supported in Payment Sheet.
 */
@Parcelize
internal enum class SupportedPaymentMethod(
    val paymentMethodType: PaymentMethod.Type,
    @StringRes val displayNameResource: Int,
    @DrawableRes val iconResource: Int,
    val formType: FormType? = null
) : Parcelable {
    Card(
        PaymentMethod.Type.Card,
        R.string.stripe_paymentsheet_payment_method_card,
        R.drawable.stripe_ic_paymentsheet_pm_card
    ),
    Bancontact(
        PaymentMethod.Type.Bancontact,
        R.string.stripe_paymentsheet_payment_method_bancontact,
        R.drawable.stripe_ic_paymentsheet_pm_bancontact,
        FormType.Bancontact
    ),
    Sofort(
        PaymentMethod.Type.Sofort,
        R.string.stripe_paymentsheet_payment_method_sofort,
        R.drawable.stripe_ic_paymentsheet_pm_sofort,
        FormType.Sofort
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
