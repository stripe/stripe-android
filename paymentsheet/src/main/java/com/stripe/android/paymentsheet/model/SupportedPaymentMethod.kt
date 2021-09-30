package com.stripe.android.paymentsheet.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.elements.PaymentMethodSpec
import com.stripe.android.paymentsheet.forms.afterpayClearpay
import com.stripe.android.paymentsheet.forms.bancontact
import com.stripe.android.paymentsheet.forms.card
import com.stripe.android.paymentsheet.forms.eps
import com.stripe.android.paymentsheet.forms.giropay
import com.stripe.android.paymentsheet.forms.ideal
import com.stripe.android.paymentsheet.forms.p24
import com.stripe.android.paymentsheet.forms.sepaDebit
import com.stripe.android.paymentsheet.forms.sofort

/**
 * Enum defining all payment method types for which Payment Sheet can collect payment data.
 *
 * FormSpec is optionally null only because Card is not converted to the compose model.
 */
internal enum class SupportedPaymentMethod(
    // Mandate, isReusable, and hasDelayedSettlement all hidden in here
    val type: PaymentMethod.Type, // Mandate requirement is hidden in here
    @StringRes val displayNameResource: Int,
    @DrawableRes val iconResource: Int,
    val spec: PaymentMethodSpec,
) {
    Card(
        PaymentMethod.Type.Card,
        R.string.stripe_paymentsheet_payment_method_card,
        R.drawable.stripe_ic_paymentsheet_pm_card,
        card
    ),
    Bancontact(
        PaymentMethod.Type.Bancontact,
        R.string.stripe_paymentsheet_payment_method_bancontact,
        R.drawable.stripe_ic_paymentsheet_pm_bancontact,
        bancontact
    ),
    Sofort(
        PaymentMethod.Type.Sofort,
        R.string.stripe_paymentsheet_payment_method_sofort,
        R.drawable.stripe_ic_paymentsheet_pm_klarna,
        sofort
    ),
    Ideal(
        PaymentMethod.Type.Ideal,
        R.string.stripe_paymentsheet_payment_method_ideal,
        R.drawable.stripe_ic_paymentsheet_pm_ideal,
        ideal
    ),
    SepaDebit(
        PaymentMethod.Type.SepaDebit,
        R.string.stripe_paymentsheet_payment_method_sepa_debit,
        R.drawable.stripe_ic_paymentsheet_pm_sepa_debit,
        sepaDebit
    ),
    Eps(
        PaymentMethod.Type.Eps,
        R.string.stripe_paymentsheet_payment_method_eps,
        R.drawable.stripe_ic_paymentsheet_pm_eps,
        eps
    ),
    P24(
        PaymentMethod.Type.P24,
        R.string.stripe_paymentsheet_payment_method_p24,
        R.drawable.stripe_ic_paymentsheet_pm_p24,
        p24,
    ),
    Giropay(
        PaymentMethod.Type.Giropay,
        R.string.stripe_paymentsheet_payment_method_giropay,
        R.drawable.stripe_ic_paymentsheet_pm_giropay,
        giropay,
    ),
    AfterpayClearpay(
        PaymentMethod.Type.AfterpayClearpay,
        R.string.stripe_paymentsheet_payment_method_afterpay_clearpay,
        R.drawable.stripe_ic_paymentsheet_pm_afterpay_clearpay,
        afterpayClearpay
    );

    override fun toString(): String {
        return type.code
    }

    companion object {
        fun fromCode(code: String?) =
            values().firstOrNull { it.type.code == code }

        /**
         * Defines all types of saved payment method supported on Payment Sheet.
         *
         * These are fetched from the
         * [PaymentMethods API endpoint](https://stripe.com/docs/api/payment_methods/list) for
         * returning customers.
         */
        val supportedSavedPaymentMethods = setOf("card")
    }
}
