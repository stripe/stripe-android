package com.stripe.android.paymentsheet.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.specifications.FormSpec
import com.stripe.android.paymentsheet.specifications.bancontact
import com.stripe.android.paymentsheet.specifications.card
import com.stripe.android.paymentsheet.specifications.eps
import com.stripe.android.paymentsheet.specifications.giropay
import com.stripe.android.paymentsheet.specifications.ideal
import com.stripe.android.paymentsheet.specifications.p24
import com.stripe.android.paymentsheet.specifications.sepaDebit
import com.stripe.android.paymentsheet.specifications.sofort

/**
 * Enum defining all payment method types for which Payment Sheet can collect payment data.
 *
 * FormSpec is optionally null only because Card is not converted to the compose model.
 */
enum class SupportedPaymentMethod(
    val code: String,
    @StringRes val displayNameResource: Int,
    @DrawableRes val iconResource: Int,
    val formSpec: FormSpec?
) {
    Card(
        "card",
        R.string.stripe_paymentsheet_payment_method_card,
        R.drawable.stripe_ic_paymentsheet_pm_card,
        card
    ),
    Bancontact(
        "bancontact",
        R.string.stripe_paymentsheet_payment_method_bancontact,
        R.drawable.stripe_ic_paymentsheet_pm_bancontact,
        bancontact
    ),
    Sofort(
        "sofort",
        R.string.stripe_paymentsheet_payment_method_sofort,
        R.drawable.stripe_ic_paymentsheet_pm_klarna,
        sofort
    ),
    Ideal(
        "ideal",
        R.string.stripe_paymentsheet_payment_method_ideal,
        R.drawable.stripe_ic_paymentsheet_pm_ideal,
        ideal
    ),
    SepaDebit(
        "sepa_debit",
        R.string.stripe_paymentsheet_payment_method_sepa_debit,
        R.drawable.stripe_ic_paymentsheet_pm_sepa_debit,
        sepaDebit
    ),
    Eps(
        "eps",
        R.string.stripe_paymentsheet_payment_method_eps,
        R.drawable.stripe_ic_paymentsheet_pm_eps,
        eps
    ),
    P24(
        "p24",
        R.string.stripe_paymentsheet_payment_method_p24,
        R.drawable.stripe_ic_paymentsheet_pm_p24,
        p24
    ),
    Giropay(
        "giropay",
        R.string.stripe_paymentsheet_payment_method_giropay,
        R.drawable.stripe_ic_paymentsheet_pm_giropay,
        giropay
    );

    override fun toString(): String {
        return code
    }

    companion object {
        fun fromCode(code: String?) =
            values().firstOrNull { it.code == code }

        /**
         * Defines all types of saved payment method supported on Payment Sheet.
         *
         * These are fetched from the
         * [PaymentMethods API endpoint](https://stripe.com/docs/api/payment_methods/list) for
         * returning customers.
         */
        val supportedSavedPaymentMethods = setOf("card", "sepa_debit")
    }
}
