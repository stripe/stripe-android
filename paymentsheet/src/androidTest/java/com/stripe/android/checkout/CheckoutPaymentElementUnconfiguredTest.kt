package com.stripe.android.checkout

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.rule.IntentsRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.networktesting.AdvancedFraudSignalsTestRule
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.utils.GooglePayRepositoryTestRule
import com.stripe.android.paymentsheet.utils.TestRules
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutPaymentElementUnconfiguredTest {
    // Unexpected analytics (including errors from present()) must also fail these no-op tests.
    private val networkRule = NetworkRule(
        hostsToTrack = listOf(ApiRequest.API_HOST, AnalyticsRequest.HOST),
        validationTimeout = 5.seconds,
    )

    @get:Rule
    val testRules: TestRules = TestRules.create(networkRule = networkRule) {
        around(AdvancedFraudSignalsTestRule())
            .around(GooglePayRepositoryTestRule())
            .around(IntentsRule())
    }

    @Test
    fun testContentIsEmptyWhenPaymentElementIsNotConfigured() = runScenario {
        assertContentIsEmpty()
        assertThat(controller.session.value).isNotNull()
        markTestSucceeded()
    }

    @Test
    fun testPresentDoesNothingWhenPaymentElementIsNotConfigured() = runScenario {
        val session = requireNotNull(controller.session.value)
        val intentsBeforePresent = Intents.getIntents().toList()
        assertContentIsEmpty()

        presentPaymentOptions()
        testRules.compose.waitForIdle()

        assertContentIsEmpty()
        assertThat(Intents.getIntents()).containsExactlyElementsIn(intentsBeforePresent)
        assertThat(controller.session.value).isEqualTo(session)
        assertThat(controller.isUpdating.value).isFalse()
        markTestSucceeded()
    }

    private fun runScenario(block: suspend CheckoutPaymentElementScenario.() -> Unit) {
        runCheckoutPaymentElementScenario(
            networkRule = networkRule,
            setup = { controller ->
                assertThat(controller.configure(DEFAULT_CLIENT_SECRET, CheckoutController.Configuration()))
                    .isInstanceOf(CheckoutController.OperationResult.Succeeded::class.java)
            },
            block = block,
        )
    }

    private fun assertContentIsEmpty() {
        testRules.compose.onNodeWithTag(
            CHECKOUT_PAYMENT_ELEMENT_CONTENT_TEST_TAG,
            useUnmergedTree = true,
        ).assertExists().onChildren().assertCountEquals(0)
    }

    private companion object {
        const val DEFAULT_CLIENT_SECRET = "${DEFAULT_CHECKOUT_SESSION_ID}_secret_example"
    }
}
