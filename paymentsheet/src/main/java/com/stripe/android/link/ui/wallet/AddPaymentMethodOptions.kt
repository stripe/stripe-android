package com.stripe.android.link.ui.wallet

import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.LinkPaymentMethodFilter
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.model.supportedPaymentMethodTypes
import com.stripe.android.model.ConsumerPaymentDetails
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class AddPaymentMethodOptions @AssistedInject constructor(
    @Assisted private val linkAccount: LinkAccount,
    private val configuration: LinkConfiguration,
    private val linkLaunchMode: LinkLaunchMode,
) {
    val values: List<AddPaymentMethodOption> = run {
        val stripeIntent = configuration.stripeIntent
        val supportedPaymentMethodTypes = stripeIntent.supportedPaymentMethodTypes(linkAccount)
        val paymentMethodFilter = (linkLaunchMode as? LinkLaunchMode.PaymentMethodSelection)?.paymentMethodFilter

        buildList {
            @Suppress("ComplexCondition")
            if (
                linkAccount.consumerPublishableKey != null &&
                configuration.financialConnectionsAvailability != null &&
                supportedPaymentMethodTypes.contains(ConsumerPaymentDetails.BankAccount.TYPE) &&
                (paymentMethodFilter == null || paymentMethodFilter == LinkPaymentMethodFilter.BankAccount)
            ) {
                add(AddPaymentMethodOption.Bank(configuration.financialConnectionsAvailability))
            }
            if (supportedPaymentMethodTypes.contains(ConsumerPaymentDetails.Card.TYPE) &&
                (paymentMethodFilter == null || paymentMethodFilter == LinkPaymentMethodFilter.Card)
            ) {
                add(AddPaymentMethodOption.Card)
            }
        }
    }

    val default: AddPaymentMethodOption?
        get() = when {
            values.size > 1 ->
                // Default to previous behavior.
                AddPaymentMethodOption.Card
            else ->
                values.firstOrNull()
        }

    @AssistedFactory
    interface Factory {
        fun create(linkAccount: LinkAccount): AddPaymentMethodOptions
    }
}
