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
    val type: PaymentMethod.Type,
    @StringRes val displayNameResource: Int,
    @DrawableRes val iconResource: Int,
    val formSpec: FormSpec?,
    val userRequestedConfirmSaveForFutureSupported: Boolean
) {
    Card(
        PaymentMethod.Type.Card,
        R.string.stripe_paymentsheet_payment_method_card,
        R.drawable.stripe_ic_paymentsheet_pm_card,
        null,
        userRequestedConfirmSaveForFutureSupported = true
    ),
    Bancontact(
        PaymentMethod.Type.Bancontact,
        R.string.stripe_paymentsheet_payment_method_bancontact,
        R.drawable.stripe_ic_paymentsheet_pm_bancontact,
        bancontact,
        userRequestedConfirmSaveForFutureSupported = true
    ),
    Sofort(
        PaymentMethod.Type.Sofort,
        R.string.stripe_paymentsheet_payment_method_sofort,
        R.drawable.stripe_ic_paymentsheet_pm_klarna,
        sofort,
        userRequestedConfirmSaveForFutureSupported = true
    ),
    Ideal(
        PaymentMethod.Type.Ideal,
        R.string.stripe_paymentsheet_payment_method_ideal,
        R.drawable.stripe_ic_paymentsheet_pm_ideal,
        ideal,
        userRequestedConfirmSaveForFutureSupported = true
    ),
    SepaDebit(
        PaymentMethod.Type.SepaDebit,
        R.string.stripe_paymentsheet_payment_method_sepa_debit,
        R.drawable.stripe_ic_paymentsheet_pm_sepa_debit,
        sepaDebit,
        userRequestedConfirmSaveForFutureSupported = true
    ),
    Eps(
        PaymentMethod.Type.Eps,
        R.string.stripe_paymentsheet_payment_method_eps,
        R.drawable.stripe_ic_paymentsheet_pm_eps,
        eps,
        userRequestedConfirmSaveForFutureSupported = false
    ),
    P24(
        PaymentMethod.Type.P24,
        R.string.stripe_paymentsheet_payment_method_p24,
        R.drawable.stripe_ic_paymentsheet_pm_p24,
        p24,
        userRequestedConfirmSaveForFutureSupported = false
    ),
    Giropay(
        PaymentMethod.Type.Giropay,
        R.string.stripe_paymentsheet_payment_method_giropay,
        R.drawable.stripe_ic_paymentsheet_pm_giropay,
        giropay,
        userRequestedConfirmSaveForFutureSupported = false
    ),
    AfterpayClearpay(
        PaymentMethod.Type.AfterpayClearpay,
        R.string.stripe_paymentsheet_payment_method_afterpay_clearpay,
        R.drawable.stripe_ic_paymentsheet_pm_afterpay_clearpay,
        afterpayClearpay,
        userRequestedConfirmSaveForFutureSupported = true
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
