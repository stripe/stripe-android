package com.stripe.android.checkout

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.testing.PaymentConfigurationTestRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
class CheckoutTest {

    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()
    private val networkRule = NetworkRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(networkRule)
        .around(PaymentConfigurationTestRule(applicationContext))

    @Test
    fun `createWithState produces Checkout with correct checkoutSession id`() = runTest {
        runCreateWithStateScenario { checkout ->
            checkout.checkoutSession.test {
                assertThat(awaitItem().id).isEqualTo("cs_test_abc123")
            }
        }
    }

    @Test
    fun `configure returns Checkout with checkoutSession id from network response`() = runConfigureScenario(
        clientSecret = "cs_test_a1vLTpmgcJO40ZjQpd3GUNHwlwtkT1bejjhpfd0nN05iqoVuJziixjNYIh_secret_example",
        networkSetup = {
            networkRule.enqueue(
                host("api.stripe.com"),
                method("POST"),
                path("/v1/payment_pages/cs_test_a1vLTpmgcJO40ZjQpd3GUNHwlwtkT1bejjhpfd0nN05iqoVuJziixjNYIh/init"),
            ) { response ->
                response.testBodyFromFile("checkout-session-init.json")
            }
        },
    ) { result ->
        val checkout = result.getOrThrow()
        checkout.checkoutSession.test {
            assertThat(awaitItem().id)
                .isEqualTo("cs_test_a1vLTpmgcJO40ZjQpd3GUNHwlwtkT1bejjhpfd0nN05iqoVuJziixjNYIh")
        }
    }

    @Test
    fun `applyPromotionCode updates checkoutSession on success`() = runTest {
        runCreateWithStateScenario { checkout ->
            networkRule.enqueue(
                host("api.stripe.com"),
                method("POST"),
                path("/v1/payment_pages/cs_test_abc123"),
                bodyPart("promotion_code", "10OFF"),
            ) { response ->
                response.testBodyFromFile("checkout-session-apply-discount.json")
            }

            assertThat(checkout.checkoutSession.value.totalSummary).isNull()

            val result = checkout.applyPromotionCode("10OFF")
            assertThat(result.isSuccess).isTrue()

            val totalSummary = checkout.checkoutSession.value.totalSummary
            assertThat(totalSummary).isNotNull()
            assertThat(totalSummary!!.discountAmounts).hasSize(1)
            assertThat(totalSummary.discountAmounts[0].displayName).isEqualTo("10OFF")
        }
    }

    @Test
    fun `applyPromotionCode returns failure on error response`() = runTest {
        runCreateWithStateScenario { checkout ->
            networkRule.enqueue(
                host("api.stripe.com"),
                method("POST"),
                path("/v1/payment_pages/cs_test_abc123"),
            ) { response ->
                response.setResponseCode(400)
                response.setBody("""{"error": {"message": "Invalid promotion code"}}""")
            }

            val initial = checkout.checkoutSession.value

            val result = checkout.applyPromotionCode("INVALID")
            assertThat(result.isFailure).isTrue()

            assertThat(checkout.checkoutSession.value).isEqualTo(initial)
        }
    }

    @Test
    fun `applyPromotionCode trims whitespace from promotion code`() = runTest {
        runCreateWithStateScenario { checkout ->
            networkRule.enqueue(
                host("api.stripe.com"),
                method("POST"),
                path("/v1/payment_pages/cs_test_abc123"),
                bodyPart("promotion_code", "10OFF"),
            ) { response ->
                response.testBodyFromFile("checkout-session-apply-discount.json")
            }

            val result = checkout.applyPromotionCode("  10OFF  ")
            assertThat(result.isSuccess).isTrue()
        }
    }

    @Test
    fun `configure returns failure when network request fails`() = runConfigureScenario(
        clientSecret = "cs_test_abc123_secret_xyz",
        networkSetup = {
            networkRule.enqueue(
                host("api.stripe.com"),
                method("POST"),
                path("/v1/payment_pages/cs_test_abc123/init"),
            ) { response ->
                response.setResponseCode(500)
                response.setBody("""{"error": {"message": "Internal server error"}}""")
            }
        },
    ) { result ->
        assertThat(result.isFailure).isTrue()
    }

    private suspend fun runCreateWithStateScenario(
        checkoutSessionResponse: CheckoutSessionResponse = CheckoutSessionResponse(
            id = "cs_test_abc123",
            amount = 1000L,
            currency = "usd",
        ),
        block: suspend (Checkout) -> Unit,
    ) {
        val state = Checkout.State(
            checkoutSessionResponse = checkoutSessionResponse,
        )
        val checkout = Checkout.createWithState(applicationContext, state)
        block(checkout)
    }

    private fun runConfigureScenario(
        clientSecret: String,
        networkSetup: () -> Unit,
        block: suspend (Result<Checkout>) -> Unit,
    ) = runTest {
        networkSetup()
        val result = Checkout.configure(
            context = applicationContext,
            checkoutSessionClientSecret = clientSecret,
        )
        block(result)
    }
}
