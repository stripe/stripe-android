package com.stripe.android.paymentelement.confirmation.gpay

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.checkout.Checkout
import com.stripe.android.checkout.CheckoutInstances
import com.stripe.android.checkout.CheckoutStateFactory
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.testing.PaymentConfigurationTestRule
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
class GooglePayDisplayItemsFactoryTest {

    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()

    @get:Rule
    val paymentConfigRule = PaymentConfigurationTestRule(applicationContext)

    @After
    fun tearDown() {
        CheckoutInstances.clear()
    }

    @Test
    fun `returns empty list when integration metadata is not CheckoutSession`() {
        val metadata = PaymentMethodMetadataFactory.create(
            integrationMetadata = IntegrationMetadata.IntentFirst(clientSecret = "pi_xxx_secret_yyy"),
        )

        val result = GooglePayDisplayItemsFactory.create(metadata)

        assertThat(result).isEmpty()
    }

    @Test
    fun `returns empty list when no checkout instance exists for key`() {
        val metadata = PaymentMethodMetadataFactory.create(
            integrationMetadata = IntegrationMetadata.CheckoutSession(
                id = "cs_xxx",
                instancesKey = "nonexistent_key",
            ),
        )

        val result = GooglePayDisplayItemsFactory.create(metadata)

        assertThat(result).isEmpty()
    }

    @Test
    fun `returns empty list when checkout session has no line items`() {
        val checkout = createCheckoutWithLineItems(emptyList())
        CheckoutInstances.add(INSTANCES_KEY, checkout)

        val metadata = PaymentMethodMetadataFactory.create(
            integrationMetadata = IntegrationMetadata.CheckoutSession(
                id = "cs_xxx",
                instancesKey = INSTANCES_KEY,
            ),
        )

        val result = GooglePayDisplayItemsFactory.create(metadata)

        assertThat(result).isEmpty()
    }

    @Test
    fun `maps checkout session line items to display items`() {
        val lineItems = listOf(
            CheckoutSessionResponse.LineItem(
                id = "li_1",
                name = "Widget",
                quantity = 2,
                unitAmount = 1000L,
                subtotal = 2000L,
                total = 2000L,
            ),
            CheckoutSessionResponse.LineItem(
                id = "li_2",
                name = "Gadget",
                quantity = 1,
                unitAmount = 500L,
                subtotal = 500L,
                total = 450L,
            ),
        )

        val checkout = createCheckoutWithLineItems(lineItems)
        CheckoutInstances.add(INSTANCES_KEY, checkout)

        val metadata = PaymentMethodMetadataFactory.create(
            integrationMetadata = IntegrationMetadata.CheckoutSession(
                id = "cs_xxx",
                instancesKey = INSTANCES_KEY,
            ),
        )

        val result = GooglePayDisplayItemsFactory.create(metadata)

        assertThat(result).hasSize(2)
        assertThat(result[0]).isEqualTo(
            GooglePayJsonFactory.DisplayItem(
                label = "Widget",
                type = GooglePayJsonFactory.DisplayItem.Type.LINE_ITEM,
                price = 2000L,
            )
        )
        assertThat(result[1]).isEqualTo(
            GooglePayJsonFactory.DisplayItem(
                label = "Gadget",
                type = GooglePayJsonFactory.DisplayItem.Type.LINE_ITEM,
                price = 450L,
            )
        )
    }

    private fun createCheckoutWithLineItems(
        lineItems: List<CheckoutSessionResponse.LineItem>,
    ): Checkout {
        return Checkout.createWithState(
            context = applicationContext,
            state = CheckoutStateFactory.create(
                key = INSTANCES_KEY,
                checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                    lineItems = lineItems,
                ),
            ),
        )
    }

    private companion object {
        const val INSTANCES_KEY = "test_key"
    }
}
