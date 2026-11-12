package com.stripe.android.checkout

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.networktesting.AdvancedFraudSignalsTestRule
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.sheet.EmbeddedSheetActivity
import com.stripe.android.paymentsheet.MainActivity
import com.stripe.android.paymentsheet.utils.GooglePayRepositoryTestRule
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.testing.waitUntilWithIdle
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutPaymentElementUnconfiguredTest {
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
    fun testPresentFailsAndClosesWhenPaymentElementIsNotConfigured() = runScenario {
        val session = requireNotNull(controller.session.value)
        assertContentIsEmpty()

        presentPaymentOptions()
        val sheetIntentMatcher = hasComponent(EmbeddedSheetActivity::class.java.name)
        intended(sheetIntentMatcher)
        val sheetIntent = Intents.getIntents().single { sheetIntentMatcher.matches(it) }
        assertThat(EmbeddedActivityArgs.fromIntent(sheetIntent)?.presentationState)
            .isEqualTo(EmbeddedActivityArgs.PresentationState.Failed)
        testRules.compose.waitUntilWithIdle("host to resume after the failed sheet closes") {
            testRules.compose.runOnIdle {
                ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED)
                    .any { it is MainActivity }
            }
        }

        assertContentIsEmpty()
        assertThat(controller.session.value).isEqualTo(session)
        assertThat(controller.isUpdating.value).isFalse()
        markTestSucceeded()
    }

    private fun runScenario(block: suspend CheckoutPaymentElementScenario.() -> Unit) {
        runCheckoutPaymentElementScenario(
            networkRule = networkRule,
            setup = { controller ->
                networkRule.validateCheckoutInitStartedAndSucceeded()
                assertThat(controller.configure(DEFAULT_CLIENT_SECRET, CheckoutElementsController.Configuration()))
                    .isInstanceOf(CheckoutElementsController.OperationResult.Succeeded::class.java)
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
