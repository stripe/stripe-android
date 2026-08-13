package com.stripe.android.paymentelement.confirmation.gpay

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.CommonConfigurationFactory
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import org.junit.Test

class GooglePayBillingEmailOverrideProviderTest {

    @Test
    fun `returns checkout session email when integration is a checkout session`() {
        val result = billingEmailOverride(
            defaultBillingDetails = PaymentSheet.BillingDetails(email = "checkout@example.com"),
            integrationMetadata = checkoutSessionMetadata(),
        )

        assertThat(result).isEqualTo("checkout@example.com")
    }

    @Test
    fun `returns null when checkout session has no email`() {
        val result = billingEmailOverride(
            defaultBillingDetails = PaymentSheet.BillingDetails(),
            integrationMetadata = checkoutSessionMetadata(),
        )

        assertThat(result).isNull()
    }

    @Test
    fun `returns null outside of a checkout session even when a default email is present`() {
        val result = billingEmailOverride(
            defaultBillingDetails = PaymentSheet.BillingDetails(email = "merchant@example.com"),
            integrationMetadata = IntegrationMetadata.IntentFirst("pi_1234_secret_1234"),
        )

        assertThat(result).isNull()
    }

    private fun billingEmailOverride(
        defaultBillingDetails: PaymentSheet.BillingDetails? = null,
        integrationMetadata: IntegrationMetadata,
    ): String? {
        return GooglePayBillingEmailOverrideProvider.get(
            configuration = CommonConfigurationFactory.create(
                defaultBillingDetails = defaultBillingDetails,
            ),
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                integrationMetadata = integrationMetadata,
            ),
        )
    }

    private fun checkoutSessionMetadata(): IntegrationMetadata.CheckoutSession {
        val checkoutSessionResponse = CheckoutSessionResponseFactory.create()
        return IntegrationMetadata.CheckoutSession(
            id = checkoutSessionResponse.id,
            instancesKey = "checkout_instances_123",
            checkoutSessionResponse = checkoutSessionResponse,
        )
    }
}
