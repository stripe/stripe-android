package com.stripe.android.checkout

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.testing.PaymentConfigurationTestRule
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.After
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
    fun `different keys produce different instances`() {
        val checkout1 = createCheckout(key = "key1")
        val checkout2 = createCheckout(key = "key2")

        assertThat(checkout1).isNotSameInstanceAs(checkout2)
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
    fun `shared instance emits isLoading and checkoutSession across callers`() = runTest {
        val checkoutA = createCheckout(key = "key1")
        val checkoutB = Checkout.createWithState(
            context = applicationContext,
            state = checkoutA.state,
        )

        val holdResponse = CountDownLatch(1)

        networkRule.checkoutUpdate { response ->
            holdResponse.await(10, TimeUnit.SECONDS)
            response.setBody("""{"id": "cs_123", "line_items": [], "status": "open"}""")
        }

        checkoutA.isLoading.test {
            assertThat(awaitItem()).isFalse()

            val job = async { checkoutB.removePromotionCode() }
            testScheduler.advanceUntilIdle()

            assertThat(awaitItem()).isTrue()

            holdResponse.countDown()
            job.await()

            assertThat(awaitItem()).isFalse()
        }

        assertThat(checkoutA.checkoutSession.value).isEqualTo(checkoutB.checkoutSession.value)
    }

    @Test
    fun `concurrent mutations on shared instance are queued`() = runTest {
        val checkoutA = createCheckout(key = "key1")
        val checkoutB = Checkout.createWithState(
            context = applicationContext,
            state = checkoutA.state,
        )

        val holdFirstResponse = CountDownLatch(1)
        val secondRequestArrived = CountDownLatch(1)

        networkRule.checkoutUpdate { response ->
            holdFirstResponse.await(10, TimeUnit.SECONDS)
            response.setBody("""{"id": "cs_123", "line_items": [], "status": "open"}""")
        }

        networkRule.checkoutUpdate { response ->
            secondRequestArrived.countDown()
            response.setBody("""{"id": "cs_123", "line_items": [], "status": "open"}""")
        }

        val jobB = async { checkoutB.applyPromotionCode("B_CODE") }
        testScheduler.advanceUntilIdle()

        val jobA = async { checkoutA.applyPromotionCode("A_CODE") }
        testScheduler.advanceUntilIdle()

        // A's request should not arrive while B holds the mutex
        assertThat(secondRequestArrived.count).isEqualTo(1)

        holdFirstResponse.countDown()
        jobB.await()
        jobA.await()

        assertThat(secondRequestArrived.count).isEqualTo(0)
    }

    @Test
    fun `getOrCreate returns existing instance without calling factory`() {
        val checkout = createCheckout(key = "key1")

        val result = CheckoutInstances.getOrCreate("key1") {
            error("factory should not be called")
        }

        assertThat(result).isSameInstanceAs(checkout)
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
