package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse

internal object IntegrationMetadataFactory {
    fun checkoutSession(response: CheckoutSessionResponse) = IntegrationMetadata.CheckoutSession(
        id = response.id,
        instancesKey = "test_instances_key",
        checkoutSessionResponse = response,
        collectedEmail = null,
    )
}
