package com.stripe.android.paymentsheet.model

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.specifications.bancontact
import com.stripe.android.paymentsheet.specifications.sofort
import kotlinx.parcelize.Parcelize

/**
 * Enum defining all payment methods supported in Payment Sheet.
 */
@Parcelize
enum class SupportedPaymentMethod(
    val code: String,
    @StringRes val displayNameResource: Int,
    @DrawableRes val iconResource: Int
) : Parcelable {
    Card(
        "card",
        R.string.stripe_paymentsheet_payment_method_card,
        R.drawable.stripe_ic_paymentsheet_pm_card
    ),
    Bancontact(
        "bancontact",
        R.string.stripe_paymentsheet_payment_method_bancontact,
        R.drawable.stripe_ic_paymentsheet_pm_bancontact
    ),
    Sofort(
        "sofort",
        R.string.stripe_paymentsheet_payment_method_sofort,
        R.drawable.stripe_ic_paymentsheet_pm_sofort
    );

    fun getFormSpec() = when (this) {
        Bancontact -> bancontact
        Sofort -> sofort
        else -> null
    }

    override fun toString(): String {
        return code
    }

    companion object {
        fun fromCode(code: String?) =
            values().firstOrNull { it.code == code }
    }
}
