package com.stripe.android.checkout

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.testing.PaymentConfigurationTestRule
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
class CheckoutInstancesTest {

    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(PaymentConfigurationTestRule(applicationContext))

    @After
    fun tearDown() {
        CheckoutInstances.clear()
    }

    @Test
    fun `get returns null for unknown key`() {
        assertThat(CheckoutInstances["unknown-key"]).isNull()
    }

    @Test
    fun `register and get round-trips single instance`() {
        val checkout = createCheckout(key = "key1")

        CheckoutInstances.register("key1", checkout, "test")

        assertThat(CheckoutInstances["key1"]).isSameInstanceAs(checkout)
    }

    @Test
    fun `register same instance twice is a no-op`() {
        val checkout = createCheckout(key = "key1")

        CheckoutInstances.register("key1", checkout, "test")
        CheckoutInstances.register("key1", checkout, "test")

        assertThat(CheckoutInstances["key1"]).isSameInstanceAs(checkout)
    }

    @Test
    fun `register different instance for same key throws`() {
        val checkout1 = createCheckout(key = "key1")
        val checkout2 = createCheckout(key = "key1")

        CheckoutInstances.register("key1", checkout1, "test")

        val error = assertThrows(IllegalStateException::class.java) {
            CheckoutInstances.register("key1", checkout2, "test")
        }
        assertThat(error).hasMessageThat().contains("test")
    }

    @Test
    fun `register same instance with different owner throws`() {
        val checkout = createCheckout(key = "key1")

        CheckoutInstances.register("key1", checkout, "FlowController")

        val error = assertThrows(IllegalStateException::class.java) {
            CheckoutInstances.register("key1", checkout, "EmbeddedPaymentElement")
        }
        assertThat(error).hasMessageThat().contains("FlowController")
    }

    @Test
    fun `unregister removes correct instance`() {
        val checkout = createCheckout(key = "key1")
        CheckoutInstances.register("key1", checkout, "test")

        CheckoutInstances.unregister("key1", checkout)

        assertThat(CheckoutInstances["key1"]).isNull()
    }

    @Test
    fun `unregister with wrong instance is a no-op`() {
        val checkout1 = createCheckout(key = "key1")
        val checkout2 = createCheckout(key = "key1")
        CheckoutInstances.register("key1", checkout1, "test")

        // Attempt to unregister with a different instance - should be ignored.
        CheckoutInstances.unregister("key1", checkout2)

        assertThat(CheckoutInstances["key1"]).isSameInstanceAs(checkout1)
    }

    @Test
    fun `clear empties the map`() {
        val checkout1 = createCheckout(key = "key1")
        val checkout2 = createCheckout(key = "key2")

        CheckoutInstances.register("key1", checkout1, "test")
        CheckoutInstances.register("key2", checkout2, "test")

        CheckoutInstances.clear()

        assertThat(CheckoutInstances["key1"]).isNull()
        assertThat(CheckoutInstances["key2"]).isNull()
    }

    @Test
    fun `multiple keys coexist independently`() {
        val checkout1 = createCheckout(key = "key1")
        val checkout2 = createCheckout(key = "key2")

        CheckoutInstances.register("key1", checkout1, "test")
        CheckoutInstances.register("key2", checkout2, "test")

        assertThat(CheckoutInstances["key1"]).isSameInstanceAs(checkout1)
        assertThat(CheckoutInstances["key2"]).isSameInstanceAs(checkout2)
    }

    @Test
    fun `unregister after re-register with same instance leaves key absent`() {
        val checkout = createCheckout(key = "key1")

        CheckoutInstances.register("key1", checkout, "test")
        CheckoutInstances.register("key1", checkout, "test") // idempotent
        CheckoutInstances.unregister("key1", checkout)

        assertThat(CheckoutInstances["key1"]).isNull()
    }

    private fun createCheckout(key: String): Checkout {
        return Checkout.createWithState(
            context = applicationContext,
            state = CheckoutStateFactory.create(
                key = key,
            ),
        )
    }
}
