package com.stripe.android.checkout

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.networktesting.NetworkRule
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
    fun `createWithState with different id produces Checkout with that id`() = runTest {
        runCreateWithStateScenario(
            checkoutSessionResponse = CheckoutSessionResponse(
                id = "cs_test_other",
                amount = 2000L,
                currency = "eur",
            ),
        ) { checkout ->
            checkout.checkoutSession.test {
                assertThat(awaitItem().id).isEqualTo("cs_test_other")
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
            checkoutSessionClientSecret = "cs_test_abc123_secret_xyz",
            checkoutSessionResponse = checkoutSessionResponse,
        )
        val checkout = Checkout.createWithState(state)
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
