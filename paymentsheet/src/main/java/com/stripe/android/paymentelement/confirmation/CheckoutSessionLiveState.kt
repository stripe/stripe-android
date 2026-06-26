package com.stripe.android.paymentelement.confirmation

import com.stripe.android.checkout.CheckoutInstances
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet

@OptIn(CheckoutSessionPreview::class)
internal data class CheckoutSessionLiveState(
    val amount: Long,
    val currency: String,
    val customerEmail: String?,
)

@OptIn(CheckoutSessionPreview::class)
internal fun PaymentMethodMetadata.currentCheckoutSessionLiveState(): CheckoutSessionLiveState? {
    val checkoutSession = integrationMetadata as? IntegrationMetadata.CheckoutSession ?: return null
    return checkoutSession.currentCheckoutSessionLiveState()
}

internal fun PaymentMethodMetadata.requiresGooglePayEmailCollection(): Boolean {
    return when (billingDetailsCollectionConfiguration.email) {
        PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always -> true
        PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never -> false
        PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic -> {
            currentCheckoutSessionLiveState()?.customerEmail == null
        }
    }
}

@OptIn(CheckoutSessionPreview::class)
internal fun IntegrationMetadata.CheckoutSession.currentCheckoutSessionLiveState(): CheckoutSessionLiveState? {
    val response = CheckoutInstances[instancesKey]?.internalState?.checkoutSessionResponse ?: return null
    return CheckoutSessionLiveState(
        amount = response.amount,
        currency = response.currency,
        customerEmail = response.customerEmail,
    )
}
