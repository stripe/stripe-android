package com.stripe.android.paymentelement.confirmation.gpay

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.CommonConfigurationFactory
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentsheet.PaymentSheet
import org.junit.Test

class GooglePayIsEmailRequiredProviderTest {

    @Test
    fun `returns true when email collection is always`() {
        val result = isEmailRequired(
            email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            defaultBillingDetails = PaymentSheet.BillingDetails(email = "present@example.com"),
            integrationMetadata = IntegrationMetadata.IntentFirst("pi_1234_secret_1234"),
        )

        assertThat(result).isTrue()
    }

    @Test
    fun `returns true when checkout session billing details are missing and email collection is automatic`() {
        val result = isEmailRequired(
            email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
            integrationMetadata = checkoutSessionMetadata(),
        )

        assertThat(result).isTrue()
    }

    @Test
    fun `returns true when checkout session billing details email is missing and email collection is automatic`() {
        val result = isEmailRequired(
            email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
            defaultBillingDetails = PaymentSheet.BillingDetails(),
            integrationMetadata = checkoutSessionMetadata(),
        )

        assertThat(result).isTrue()
    }

    @Test
    fun `returns false when checkout session billing details email is present and email collection is automatic`() {
        val result = isEmailRequired(
            email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
            defaultBillingDetails = PaymentSheet.BillingDetails(email = "present@example.com"),
            integrationMetadata = checkoutSessionMetadata(),
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `returns false when checkout session billing details email is missing and email collection is never`() {
        val result = isEmailRequired(
            email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
            defaultBillingDetails = PaymentSheet.BillingDetails(),
            integrationMetadata = checkoutSessionMetadata(),
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `returns false when checkout session billing details email is present and email collection is never`() {
        val result = isEmailRequired(
            email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
            defaultBillingDetails = PaymentSheet.BillingDetails(email = "present@example.com"),
            integrationMetadata = checkoutSessionMetadata(),
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `returns false when non checkout session billing details are missing and email collection is automatic`() {
        val result = isEmailRequired(
            email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
            integrationMetadata = IntegrationMetadata.IntentFirst("pi_1234_secret_1234"),
        )

        assertThat(result).isFalse()
    }

    private fun isEmailRequired(
        email: PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode,
        defaultBillingDetails: PaymentSheet.BillingDetails? = null,
        integrationMetadata: IntegrationMetadata,
    ): Boolean {
        return GooglePayIsEmailRequiredProvider.get(
            configuration = configuration(
                email = email,
                defaultBillingDetails = defaultBillingDetails,
            ),
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                integrationMetadata = integrationMetadata,
            ),
        )
    }

    private fun checkoutSessionMetadata(): IntegrationMetadata.CheckoutSession {
        return IntegrationMetadata.CheckoutSession(
            id = "cs_test_123",
            instancesKey = "checkout_instances_123",
        )
    }

    private fun configuration(
        email: PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode,
        defaultBillingDetails: PaymentSheet.BillingDetails? = null,
    ) = CommonConfigurationFactory.create(
        defaultBillingDetails = defaultBillingDetails,
        billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
            email = email,
        ),
    )
}
