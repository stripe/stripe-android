package com.stripe.android.link.ui.paymentmethod

import android.content.res.Resources
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.stripe.android.link.R
import com.stripe.android.link.ui.completePaymentButtonLabel
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.StripeIntent
import com.stripe.android.ui.core.elements.FormItemSpec
import com.stripe.android.ui.core.forms.LinkCardForm

/**
 * Represents the Payment Methods that are supported by Link.
 *
 * @param type The Payment Method type. Matches the [ConsumerPaymentDetails] types.
 * @param formSpec Specification of how the payment method data collection UI should look.
 * @param nameResourceId String resource id for the name of this payment method.
 * @param iconResourceId Drawable resource id for the icon representing this payment method.
 * @param primaryButtonStartIconResourceId Drawable resource id for the icon to be displayed at the
 *      start of the primary button when this payment method is being created.
 * @param primaryButtonEndIconResourceId Drawable resource id for the icon to be displayed at the
 *      end of the primary button when this payment method is being created.
 */
internal enum class SupportedPaymentMethod(
    val type: String,
    val formSpec: List<FormItemSpec>,
    @StringRes val nameResourceId: Int,
    @DrawableRes val iconResourceId: Int,
    @DrawableRes val primaryButtonStartIconResourceId: Int? = null,
    @DrawableRes val primaryButtonEndIconResourceId: Int? = null
) {
    Card(
        ConsumerPaymentDetails.Card.type,
        LinkCardForm.items,
        R.string.stripe_paymentsheet_payment_method_card,
        R.drawable.ic_link_card,
        primaryButtonEndIconResourceId = R.drawable.stripe_ic_lock
    ) {
        override fun primaryButtonLabel(
            stripeIntent: StripeIntent,
            resources: Resources
        ) = completePaymentButtonLabel(stripeIntent, resources)
    },
    BankAccount(
        ConsumerPaymentDetails.BankAccount.type,
        emptyList(),
        R.string.stripe_payment_method_bank,
        R.drawable.ic_link_bank,
        primaryButtonStartIconResourceId = R.drawable.ic_link_add
    ) {
        override fun primaryButtonLabel(
            stripeIntent: StripeIntent,
            resources: Resources
        ) = resources.getString(R.string.stripe_add_bank_account)
    };

    val showsForm = formSpec.isNotEmpty()

    /**
     * The label for the primary button when this payment method is being created.
     */
    abstract fun primaryButtonLabel(
        stripeIntent: StripeIntent,
        resources: Resources
    ): String

    internal companion object {
        val allTypes = values().map { it.type }.toSet()
    }
}
