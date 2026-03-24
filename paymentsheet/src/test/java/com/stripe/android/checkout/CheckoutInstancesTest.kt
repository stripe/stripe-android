package com.stripe.android.checkout

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkouttesting.checkoutInit
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.testing.PaymentConfigurationTestRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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
        val checkout = Checkout.createWithState(applicationContext, CheckoutStateFactory.create(key = "key1"))
        CheckoutInstances.clear()

        CheckoutInstances.add("key1", checkout)

        assertThat(CheckoutInstances["key1"]).containsExactly(checkout)
    }

    @Test
    fun `add multiple instances with same key returns all`() {
        val checkout1 = Checkout.createWithState(applicationContext, CheckoutStateFactory.create(key = "key1"))
        val checkout2 = Checkout.createWithState(applicationContext, CheckoutStateFactory.create(key = "key1"))
        CheckoutInstances.clear()

        CheckoutInstances.add("key1", checkout1)
        CheckoutInstances.add("key1", checkout2)

        assertThat(CheckoutInstances["key1"]).containsExactly(checkout1, checkout2)
    }

    @Test
    fun `remove clears all instances for a key`() {
        val checkout = Checkout.createWithState(applicationContext, CheckoutStateFactory.create(key = "key1"))
        CheckoutInstances.clear()

        CheckoutInstances.add("key1", checkout)
        assertThat(CheckoutInstances["key1"]).isNotEmpty()

        CheckoutInstances.remove("key1")
        assertThat(CheckoutInstances["key1"]).isEmpty()
    }

    @Test
    fun `clear empties the map`() {
        val checkout1 = Checkout.createWithState(applicationContext, CheckoutStateFactory.create(key = "key1"))
        val checkout2 = Checkout.createWithState(applicationContext, CheckoutStateFactory.create(key = "key2"))
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
    fun `ensureNoMutationInFlight throws when mutation is in flight`() {
        val checkout = Checkout.createWithState(applicationContext, CheckoutStateFactory.create(key = "key1"))
        val requestArrived = CountDownLatch(1)
        val holdResponse = CountDownLatch(1)

        networkRule.checkoutInit { response ->
            requestArrived.countDown()
            holdResponse.await()
            response.setBody("{}")
        }

        runBlocking {
            val job = launch(Dispatchers.IO) {
                checkout.refresh()
            }

            assertThat(requestArrived.await(5, TimeUnit.SECONDS)).isTrue()

            val error = assertThrows(IllegalStateException::class.java) {
                CheckoutInstances.ensureNoMutationInFlight("key1")
            }
            assertThat(error).hasMessageThat()
                .isEqualTo("Cannot launch while a checkout session mutation is in flight.")

            holdResponse.countDown()
            job.join()
        }
    }

    @Test
    fun `markIntegrationLaunched marks all instances for a key`() = runTest {
        val checkout1 = Checkout.createWithState(applicationContext, CheckoutStateFactory.create(key = "key1"))
        val checkout2 = Checkout.createWithState(applicationContext, CheckoutStateFactory.create(key = "key1"))
        CheckoutInstances.clear()
        CheckoutInstances.add("key1", checkout1)
        CheckoutInstances.add("key1", checkout2)

        CheckoutInstances.markIntegrationLaunched("key1")

        // Both instances should return failure because integrationLaunched is set.
        val result1 = checkout1.applyPromotionCode("code")
        assertThat(result1.isFailure).isTrue()
        assertThat(result1.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
        val result2 = checkout2.applyPromotionCode("code")
        assertThat(result2.isFailure).isTrue()
        assertThat(result2.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `markIntegrationDismissed clears the flag for all instances`() = runTest {
        val checkout1 = Checkout.createWithState(applicationContext, CheckoutStateFactory.create(key = "key1"))
        val checkout2 = Checkout.createWithState(applicationContext, CheckoutStateFactory.create(key = "key1"))
        CheckoutInstances.clear()
        CheckoutInstances.add("key1", checkout1)
        CheckoutInstances.add("key1", checkout2)

        CheckoutInstances.markIntegrationLaunched("key1")
        CheckoutInstances.markIntegrationDismissed("key1")

        // After dismissing, mutations should not throw the integrationLaunched error.
        // They will fail for other reasons (network), but they won't throw IllegalStateException.
        networkRule.checkoutUpdate { response ->
            response.setResponseCode(400)
            response.setBody("""{"error": {"message": "error"}}""")
        }
        networkRule.checkoutUpdate { response ->
            response.setResponseCode(400)
            response.setBody("""{"error": {"message": "error"}}""")
        }

        val result1 = checkout1.applyPromotionCode("code")
        assertThat(result1.isFailure).isTrue()
        val result2 = checkout2.applyPromotionCode("code")
        assertThat(result2.isFailure).isTrue()
    }

    @Test
    fun `multiple keys coexist independently`() {
        val checkout1 = Checkout.createWithState(applicationContext, CheckoutStateFactory.create(key = "key1"))
        val checkout2 = Checkout.createWithState(applicationContext, CheckoutStateFactory.create(key = "key2"))
        CheckoutInstances.clear()

        CheckoutInstances.add("key1", checkout1)
        CheckoutInstances.add("key2", checkout2)

        assertThat(CheckoutInstances["key1"]).containsExactly(checkout1)
        assertThat(CheckoutInstances["key2"]).containsExactly(checkout2)
    }
}
