package com.stripe.android.paymentelement.confirmation.gpay

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.Checkout
import com.stripe.android.checkout.CheckoutInstances
import com.stripe.android.checkout.CheckoutStateFactory
import com.stripe.android.common.model.CommonConfigurationFactory
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.testing.PaymentConfigurationTestRule
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
class GooglePayIsEmailRequiredProviderTest {

    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()

    @get:Rule
    val paymentConfigRule = PaymentConfigurationTestRule(applicationContext)

    @After
    fun tearDown() {
        CheckoutInstances.clear()
    }

    @Test
    fun `returns billing configuration value when integration metadata is not CheckoutSession`() {
        val result = GooglePayIsEmailRequiredProvider.get(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                integrationMetadata = IntegrationMetadata.IntentFirst(clientSecret = "value"),
            ),
            configuration = configuration(
                email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            ),
        )

        assertThat(result).isTrue()
    }

    @Test
    fun `returns billing configuration value when checkout instance is missing`() {
        val result = GooglePayIsEmailRequiredProvider.get(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                integrationMetadata = IntegrationMetadata.CheckoutSession(
                    id = "cs_xxx",
                    instancesKey = "missing_key",
                ),
            ),
            configuration = configuration(
                email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            ),
        )

        assertThat(result).isTrue()
    }

    @Test
    fun `returns true when checkout session has no customer email and email collection is automatic`() {
        val result = createAndGetIsEmailRequired(
            customerEmail = null,
            email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
        )

        assertThat(result).isTrue()
    }

    @Test
    fun `returns false when checkout session has customer email and email collection is automatic`() {
        val result = createAndGetIsEmailRequired(
            customerEmail = "present",
            email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `returns false when checkout session has no customer email and email collection is never`() {
        val result = createAndGetIsEmailRequired(
            customerEmail = null,
            email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `returns true when checkout session has customer email and email collection is always`() {
        val result = createAndGetIsEmailRequired(
            customerEmail = "present",
            email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
        )

        assertThat(result).isTrue()
    }

    private fun createAndGetIsEmailRequired(
        customerEmail: String?,
        email: PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode,
    ): Boolean {
        Checkout.createWithState(
            context = applicationContext,
            state = CheckoutStateFactory.create(
                key = INSTANCES_KEY,
                checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                    customerEmail = customerEmail,
                ),
            ),
        )

        val metadata = PaymentMethodMetadataFactory.create(
            integrationMetadata = IntegrationMetadata.CheckoutSession(
                id = "cs_xxx",
                instancesKey = INSTANCES_KEY,
            ),
        )

        return GooglePayIsEmailRequiredProvider.get(
            paymentMethodMetadata = metadata,
            configuration = configuration(email),
        )
    }

    private fun configuration(
        email: PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode,
    ) = CommonConfigurationFactory.create(
        billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
            email = email,
        ),
    )

    private companion object {
        const val INSTANCES_KEY = "test_key"
    }
}
