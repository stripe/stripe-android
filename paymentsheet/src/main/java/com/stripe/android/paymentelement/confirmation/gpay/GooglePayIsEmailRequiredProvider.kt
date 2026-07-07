package com.stripe.android.paymentelement.confirmation.gpay

import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet

@OptIn(CheckoutSessionPreview::class)
internal object GooglePayIsEmailRequiredProvider {

    fun get(configuration: CommonConfiguration): Boolean {
        val checkoutSessionIsMissingEmail = configuration.defaultBillingDetails?.email == null

        return configuration.billingDetailsCollectionConfiguration.collectsEmail ||
            (
                checkoutSessionIsMissingEmail &&
                    configuration.billingDetailsCollectionConfiguration.email !=
                    PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never
                )
    }
}
