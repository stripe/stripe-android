package com.stripe.android.checkout

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.testing.PaymentConfigurationTestRule
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
class CheckoutInstancesTest {

    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()
    private val networkRule = NetworkRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(networkRule)
        .around(PaymentConfigurationTestRule(applicationContext))

    @After
    fun tearDown() {
        CheckoutInstances.clear()
    }

    @Test
    fun `get returns empty list for unknown key`() {
        assertThat(CheckoutInstances["unknown-key"]).isEmpty()
    }

    @Test
    fun `add and get round-trips single instance`() {
        val checkout = createCheckout(key = "key1")
        CheckoutInstances.clear()

        CheckoutInstances.add("key1", checkout)

        assertThat(CheckoutInstances["key1"]).containsExactly(checkout)
    }

    @Test
    fun `add multiple instances with same key returns all`() {
        val checkout1 = createCheckout(key = "key1")
        val checkout2 = createCheckout(key = "key1")
        CheckoutInstances.clear()

        CheckoutInstances.add("key1", checkout1)
        CheckoutInstances.add("key1", checkout2)

        assertThat(CheckoutInstances["key1"]).containsExactly(checkout1, checkout2)
    }

    @Test
    fun `remove clears all instances for a key`() {
        val checkout = createCheckout(key = "key1")
        CheckoutInstances.clear()

        CheckoutInstances.add("key1", checkout)
        assertThat(CheckoutInstances["key1"]).isNotEmpty()

        CheckoutInstances.remove("key1")
        assertThat(CheckoutInstances["key1"]).isEmpty()
    }

    @Test
    fun `clear empties the map`() {
        val checkout1 = createCheckout(key = "key1")
        val checkout2 = createCheckout(key = "key2")
        CheckoutInstances.clear()

        CheckoutInstances.add("key1", checkout1)
        CheckoutInstances.add("key2", checkout2)

        CheckoutInstances.clear()

        assertThat(CheckoutInstances["key1"]).isEmpty()
        assertThat(CheckoutInstances["key2"]).isEmpty()
    }

    @Test
    fun `ensureNoMutationInFlight does not throw for unknown key`() {
        CheckoutInstances.ensureNoMutationInFlight("unknown-key")
    }

    @Test
    fun `ensureNoMutationInFlight does not throw when no mutation in flight`() {
        createCheckout(key = "key1")
        CheckoutInstances.ensureNoMutationInFlight("key1")
    }

    @Test
    fun `multiple keys coexist independently`() {
        val checkout1 = createCheckout(key = "key1")
        val checkout2 = createCheckout(key = "key2")
        CheckoutInstances.clear()

        CheckoutInstances.add("key1", checkout1)
        CheckoutInstances.add("key2", checkout2)

        assertThat(CheckoutInstances["key1"]).containsExactly(checkout1)
        assertThat(CheckoutInstances["key2"]).containsExactly(checkout2)
    }

    private fun createCheckout(key: String): Checkout {
        val state = InternalState(
            key = key,
            checkoutSessionResponse = CheckoutSessionResponse(
                id = "cs_test_abc123",
                amount = 1000L,
                currency = "usd",
                mode = CheckoutSessionResponse.Mode.PAYMENT,
                customerEmail = null,
                elementsSession = null,
                paymentIntent = null,
                setupIntent = null,
                customer = null,
                savedPaymentMethodsOfferSave = null,
                totalSummary = null,
                lineItems = emptyList(),
                shippingOptions = emptyList(),
            ),
        )
        val checkout = Checkout.createWithState(applicationContext, Checkout.State(state))
        return checkout
    }
}
