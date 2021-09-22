package com.stripe.android.paymentsheet.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.elements.FormSpec
import com.stripe.android.paymentsheet.elements.afterpayClearpay
import com.stripe.android.paymentsheet.forms.bancontact
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
    val code: String,
    @StringRes val displayNameResource: Int,
    @DrawableRes val iconResource: Int,
    val formSpec: FormSpec?,
    val requiresMandate: Boolean,
    val userRequestedConfirmSaveForFutureSupported: Boolean
) {
    Card(
        "card",
        R.string.stripe_paymentsheet_payment_method_card,
        R.drawable.stripe_ic_paymentsheet_pm_card,
        null,
        requiresMandate = PaymentMethod.Type.Card.requiresMandate,
        userRequestedConfirmSaveForFutureSupported = true
    ),
    Bancontact(
        "bancontact",
        R.string.stripe_paymentsheet_payment_method_bancontact,
        R.drawable.stripe_ic_paymentsheet_pm_bancontact,
        bancontact,
        requiresMandate = PaymentMethod.Type.Bancontact.requiresMandate,
        userRequestedConfirmSaveForFutureSupported = true
    ),
    Sofort(
        "sofort",
        R.string.stripe_paymentsheet_payment_method_sofort,
        R.drawable.stripe_ic_paymentsheet_pm_klarna,
        sofort,
        requiresMandate = PaymentMethod.Type.Sofort.requiresMandate,
        userRequestedConfirmSaveForFutureSupported = true
    ),
    Ideal(
        "ideal",
        R.string.stripe_paymentsheet_payment_method_ideal,
        R.drawable.stripe_ic_paymentsheet_pm_ideal,
        ideal,
        requiresMandate = PaymentMethod.Type.Ideal.requiresMandate,
        userRequestedConfirmSaveForFutureSupported = true
    ),
    SepaDebit(
        "sepa_debit",
        R.string.stripe_paymentsheet_payment_method_sepa_debit,
        R.drawable.stripe_ic_paymentsheet_pm_sepa_debit,
        sepaDebit,
        requiresMandate = PaymentMethod.Type.SepaDebit.requiresMandate,
        userRequestedConfirmSaveForFutureSupported = true
    ),
    Eps(
        "eps",
        R.string.stripe_paymentsheet_payment_method_eps,
        R.drawable.stripe_ic_paymentsheet_pm_eps,
        eps,
        requiresMandate = PaymentMethod.Type.Eps.requiresMandate,
        userRequestedConfirmSaveForFutureSupported = false
    ),
    P24(
        "p24",
        R.string.stripe_paymentsheet_payment_method_p24,
        R.drawable.stripe_ic_paymentsheet_pm_p24,
        p24,
        requiresMandate = PaymentMethod.Type.P24.requiresMandate,
        userRequestedConfirmSaveForFutureSupported = false
    ),
    Giropay(
        "giropay",
        R.string.stripe_paymentsheet_payment_method_giropay,
        R.drawable.stripe_ic_paymentsheet_pm_giropay,
        giropay,
        requiresMandate = PaymentMethod.Type.Giropay.requiresMandate,
        userRequestedConfirmSaveForFutureSupported = false
    ),
    AfterpayClearpay(
        "afterpay_clearpay",
        R.string.stripe_paymentsheet_payment_method_afterpay_clearpay,
        R.drawable.stripe_ic_paymentsheet_pm_afterpay_clearpay,
        afterpayClearpay,
        requiresMandate = PaymentMethod.Type.AfterpayClearpay.requiresMandate,
        userRequestedConfirmSaveForFutureSupported = true
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
        val supportedSavedPaymentMethods = setOf("card")
    }
}
