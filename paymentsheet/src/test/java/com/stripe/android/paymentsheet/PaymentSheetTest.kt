package com.stripe.android.paymentsheet

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.Checkout
import com.stripe.android.checkout.CheckoutInstancesTestRule
import com.stripe.android.checkout.InternalState
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.testing.PaymentConfigurationTestRule
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.TimeUnit

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class PaymentSheetTest {

    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()
    private val networkRule = NetworkRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(networkRule)
        .around(PaymentConfigurationTestRule(applicationContext))
        .around(CheckoutInstancesTestRule())

    @Test
    fun `presentWithCheckout throws when checkout mutation is in flight`() = runTest {
        val checkout = createCheckout(key = "test_key")
        networkRule.enqueue(
            host("api.stripe.com"),
            method("POST"),
            path("/v1/payment_pages/cs_test_abc123"),
        ) { response ->
            response.setBodyDelay(5, TimeUnit.SECONDS)
            response.testBodyFromFile("checkout-session-apply-discount.json")
        }
        val deferred = async { checkout.applyPromotionCode("10OFF") }
        testScheduler.advanceUntilIdle()

        val paymentSheet = PaymentSheet(
            paymentSheetLauncher = object : PaymentSheetLauncher {
                override fun present(
                    mode: PaymentElementLoader.InitializationMode,
                    configuration: PaymentSheet.Configuration?,
                ) = Unit
            }
        )
        val error = runCatching {
            paymentSheet.presentWithCheckout(checkout, PaymentSheet.Configuration("Test"))
        }.exceptionOrNull()
        assertThat(error).isInstanceOf(IllegalStateException::class.java)
        assertThat(error).hasMessageThat()
            .isEqualTo("Cannot launch while a checkout session mutation is in flight.")

        deferred.cancel()
    }

    private fun createCheckout(key: String): Checkout {
        val state = Checkout.State(
            InternalState(
                key = key,
                checkoutSessionResponse = CheckoutSessionResponseFactory.create(),
            ),
        )
        return Checkout.createWithState(applicationContext, state)
    }
}
