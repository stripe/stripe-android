package com.stripe.android.checkout

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
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
    fun `get returns null for unknown key`() {
        assertThat(CheckoutInstances["unknown-key"]).isNull()
    }

    @Test
    fun `add and get round-trips single instance`() {
        val checkout = createCheckout(key = "key1")
        CheckoutInstances.clear()

        CheckoutInstances.add("key1", checkout)

        assertThat(CheckoutInstances["key1"]).isSameInstanceAs(checkout)
    }

    @Test
    fun `clear empties the map`() {
        val checkout1 = createCheckout(key = "key1")
        val checkout2 = createCheckout(key = "key2")
        CheckoutInstances.clear()

        CheckoutInstances.add("key1", checkout1)
        CheckoutInstances.add("key2", checkout2)

        CheckoutInstances.clear()

        assertThat(CheckoutInstances["key1"]).isNull()
        assertThat(CheckoutInstances["key2"]).isNull()
    }

    @Test
    fun `multiple keys coexist independently`() {
        val checkout1 = createCheckout(key = "key1")
        val checkout2 = createCheckout(key = "key2")
        CheckoutInstances.clear()

        CheckoutInstances.add("key1", checkout1)
        CheckoutInstances.add("key2", checkout2)

        assertThat(CheckoutInstances["key1"]).isSameInstanceAs(checkout1)
        assertThat(CheckoutInstances["key2"]).isSameInstanceAs(checkout2)
    }

    @Test
    fun `ensureNoMutationInFlight does not throw for unknown key`() {
        CheckoutInstances.ensureNoMutationInFlight("unknown-key")
    }

    @Test
    fun `ensureNoMutationInFlight throws when mutation is in flight`() {
        val checkout = createCheckout(key = "key1")
        val requestArrived = CountDownLatch(1)
        val holdResponse = CountDownLatch(1)

        networkRule.checkoutUpdate { response ->
            requestArrived.countDown()
            holdResponse.await()
            response.setBody("{}")
        }

        runBlocking {
            val job = launch(Dispatchers.IO) {
                checkout.removePromotionCode()
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
    fun `markIntegrationLaunched blocks mutations`() = runTest {
        val checkout = createCheckout(key = "key1")

        CheckoutInstances.markIntegrationLaunched("key1")

        val result = checkout.applyPromotionCode("code")
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `markIntegrationDismissed unblocks mutations`() = runTest {
        val checkout = createCheckout(key = "key1")

        CheckoutInstances.markIntegrationLaunched("key1")
        CheckoutInstances.markIntegrationDismissed("key1")

        networkRule.checkoutUpdate { response ->
            response.setResponseCode(400)
            response.setBody("""{"error": {"message": "error"}}""")
        }

        val result = checkout.applyPromotionCode("code")
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isNotInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `createWithState returns same instance when checkout exists for key`() {
        val checkoutA = createCheckout(key = "key1")
        val stateA = checkoutA.state

        val checkoutB = Checkout.createWithState(
            context = applicationContext,
            state = stateA,
        )

        assertThat(checkoutB).isSameInstanceAs(checkoutA)
    }

    @Test
    fun `createWithState creates fresh instance when registry is empty`() {
        val state = CheckoutStateFactory.create(key = "key1")

        CheckoutInstances.clear()

        val checkout = Checkout.createWithState(
            context = applicationContext,
            state = state,
        )

        assertThat(checkout).isNotNull()
        assertThat(CheckoutInstances["key1"]).isSameInstanceAs(checkout)
    }

    @Test
    fun `live instance wins over provided state`() = runTest {
        val checkout = createCheckout(key = "key1")

        networkRule.checkoutUpdate { response ->
            response.setBody("""{"id": "cs_123", "line_items": [], "status": "open"}""")
        }
        checkout.removePromotionCode()

        val liveSession = checkout.checkoutSession.value

        val staleState = CheckoutStateFactory.create(key = "key1")
        val restored = Checkout.createWithState(
            context = applicationContext,
            state = staleState,
        )

        assertThat(restored).isSameInstanceAs(checkout)
        assertThat(restored.checkoutSession.value).isEqualTo(liveSession)
    }

    @Test
    fun `shared instance emits isLoading and checkoutSession across callers`() {
        val checkoutA = createCheckout(key = "key1")
        val checkoutB = Checkout.createWithState(
            context = applicationContext,
            state = checkoutA.state,
        )

        val requestArrived = CountDownLatch(1)
        val holdResponse = CountDownLatch(1)

        networkRule.checkoutUpdate { response ->
            requestArrived.countDown()
            holdResponse.await()
            response.setBody("""{"id": "cs_123", "line_items": [], "status": "open"}""")
        }

        runBlocking {
            val job = launch(Dispatchers.IO) {
                checkoutB.removePromotionCode()
            }

            assertThat(requestArrived.await(5, TimeUnit.SECONDS)).isTrue()

            // A observes isLoading = true (because same instance)
            assertThat(checkoutA.isLoading.value).isTrue()

            holdResponse.countDown()
            job.join()

            // After completion, A observes isLoading = false
            assertThat(checkoutA.isLoading.value).isFalse()

            // A and B share the same checkoutSession
            assertThat(checkoutA.checkoutSession.value).isEqualTo(checkoutB.checkoutSession.value)
        }
    }

    @Test
    fun `concurrent mutations on shared instance are queued`() {
        val checkoutA = createCheckout(key = "key1")
        val checkoutB = Checkout.createWithState(
            context = applicationContext,
            state = checkoutA.state,
        )

        val firstRequestArrived = CountDownLatch(1)
        val holdFirstResponse = CountDownLatch(1)
        val secondRequestArrived = CountDownLatch(1)

        networkRule.checkoutUpdate { response ->
            firstRequestArrived.countDown()
            holdFirstResponse.await()
            response.setBody("""{"id": "cs_123", "line_items": [], "status": "open"}""")
        }

        networkRule.checkoutUpdate { response ->
            secondRequestArrived.countDown()
            response.setBody("""{"id": "cs_123", "line_items": [], "status": "open"}""")
        }

        runBlocking {
            val jobB = launch(Dispatchers.IO) {
                checkoutB.applyPromotionCode("B_CODE")
            }

            assertThat(firstRequestArrived.await(5, TimeUnit.SECONDS)).isTrue()

            val jobA = launch(Dispatchers.IO) {
                checkoutA.applyPromotionCode("A_CODE")
            }

            // Give A time to reach the mutex
            Thread.sleep(100)

            // Second request should NOT have arrived yet (A is queued behind B)
            assertThat(secondRequestArrived.count).isEqualTo(1)

            // Release B's response
            holdFirstResponse.countDown()
            jobB.join()

            // Now A should proceed
            assertThat(secondRequestArrived.await(5, TimeUnit.SECONDS)).isTrue()
            jobA.join()
        }
    }

    @Test
    fun `getOrCreate returns existing instance without calling factory`() {
        val checkout = createCheckout(key = "key1")

        var factoryCalled = false
        val result = CheckoutInstances.getOrCreate("key1") {
            factoryCalled = true
            createCheckout(key = "key1")
        }

        assertThat(result).isSameInstanceAs(checkout)
        assertThat(factoryCalled).isFalse()
    }

    @Test
    fun `getOrCreate calls factory and returns new instance when none exists`() {
        CheckoutInstances.clear()

        val result = CheckoutInstances.getOrCreate("key1") {
            createCheckout(key = "key1")
        }

        assertThat(result).isNotNull()
        assertThat(CheckoutInstances["key1"]).isSameInstanceAs(result)
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
