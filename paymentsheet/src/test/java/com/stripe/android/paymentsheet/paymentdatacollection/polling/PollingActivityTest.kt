package com.stripe.android.paymentsheet.paymentdatacollection.polling

import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.StripeIntentResult
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.paymentsheet.R
import com.stripe.android.polling.IntentStatusPoller
import com.stripe.android.utils.InjectableActivityScenario
import com.stripe.android.utils.TestUtils
import com.stripe.android.utils.injectableActivityScenario
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
internal class PollingActivityTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun `Displays loading screen when activity is opened`() {
        val scenario = pollingScenario()

        scenario.onActivity {
            composeTestRule
                .onNodeWithText("Approve payment")
                .assertIsDisplayed()
        }
    }

    @Test
    fun `Displays qr code screen when activity is opened and qr code url available`() {
        val scenario = pollingScenario(
            args = defaultArgs.copy(qrCodeUrl = "valid_url")
        )

        scenario.onActivity {
            assertQrCodeWebViewIsDisplayed()
        }
    }

    @Test
    fun `Closes activity with correct result if polling finishes`() {
        val fakePoller = FakeIntentStatusPoller()
        val scenario = pollingScenario(poller = fakePoller)

        scenario.onActivity {
            fakePoller.emitNextPollResult(StripeIntent.Status.Succeeded)
            waitForActivityFinish()

            val result = PaymentFlowResult.Unvalidated.fromIntent(scenario.getResult().resultData)
            assertThat(result.flowOutcome).isEqualTo(StripeIntentResult.Outcome.SUCCEEDED)
        }
    }

    @Test
    fun `Closes activity with correct result if polling is canceled`() {
        val scenario = pollingScenario()

        scenario.onActivity {
            composeTestRule
                .onNodeWithText("Cancel and pay another way")
                .performClick()

            waitForActivityFinish()

            val result = PaymentFlowResult.Unvalidated.fromIntent(scenario.getResult().resultData)
            assertThat(result.flowOutcome).isEqualTo(StripeIntentResult.Outcome.CANCELED)
        }
    }

    @Test
    fun `Closes activity with correct result if user presses up button after failure`() {
        val fakePoller = FakeIntentStatusPoller()
        val scenario = pollingScenario(poller = fakePoller)

        scenario.onActivity {
            fakePoller.emitNextPollResult(StripeIntent.Status.RequiresPaymentMethod)

            composeTestRule
                .onNodeWithText("Payment failed")
                .assertIsDisplayed()

            composeTestRule
                .onNodeWithContentDescription("Back")
                .performClick()

            waitForActivityFinish()

            val result = PaymentFlowResult.Unvalidated.fromIntent(scenario.getResult().resultData)
            assertThat(result.flowOutcome).isEqualTo(StripeIntentResult.Outcome.CANCELED)
        }
    }

    @Test
    fun `Closes activity with correct result if user presses system back after failure`() {
        val fakePoller = FakeIntentStatusPoller()
        val scenario = pollingScenario(poller = fakePoller)

        scenario.onActivity {
            fakePoller.emitNextPollResult(StripeIntent.Status.RequiresPaymentMethod)

            composeTestRule
                .onNodeWithText("Payment failed")
                .assertIsDisplayed()

            pressBack()
            waitForActivityFinish()

            val result = PaymentFlowResult.Unvalidated.fromIntent(scenario.getResult().resultData)
            assertThat(result.flowOutcome).isEqualTo(StripeIntentResult.Outcome.CANCELED)
        }
    }

    @Test
    fun `Ignores back presses while polling`() {
        val scenario = pollingScenario()

        scenario.onActivity { activity ->
            composeTestRule
                .onNodeWithText("Approve payment")
                .assertIsDisplayed()

            pressBack()
            waitForActivityFinish()

            assertThat(activity.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        }
    }

    @Test
    fun `Passes stripeAccountId through to result when polling succeeds`() {
        val fakePoller = FakeIntentStatusPoller()
        val testStripeAccountId = "acct_test_123"
        val argsWithStripeAccountId = defaultArgs.copy(stripeAccountId = testStripeAccountId)
        val scenario = pollingScenario(args = argsWithStripeAccountId, poller = fakePoller)

        scenario.onActivity {
            fakePoller.emitNextPollResult(StripeIntent.Status.Succeeded)
            waitForActivityFinish()

            val result = PaymentFlowResult.Unvalidated.fromIntent(scenario.getResult().resultData)
            assertThat(result.flowOutcome).isEqualTo(StripeIntentResult.Outcome.SUCCEEDED)
            assertThat(result.stripeAccountId).isEqualTo(testStripeAccountId)
        }
    }

    @Test
    fun `Passes stripeAccountId through to result when polling is canceled`() {
        val testStripeAccountId = "acct_test_456"
        val argsWithStripeAccountId = defaultArgs.copy(stripeAccountId = testStripeAccountId)
        val scenario = pollingScenario(args = argsWithStripeAccountId)

        scenario.onActivity {
            composeTestRule
                .onNodeWithText("Cancel and pay another way")
                .performClick()

            waitForActivityFinish()

            val result = PaymentFlowResult.Unvalidated.fromIntent(scenario.getResult().resultData)
            assertThat(result.flowOutcome).isEqualTo(StripeIntentResult.Outcome.CANCELED)
            assertThat(result.stripeAccountId).isEqualTo(testStripeAccountId)
        }
    }

    @Test
    fun `Preserves QR code display state across configuration change`() {
        val scenario = pollingScenario(
            args = defaultArgs.copy(qrCodeUrl = "test_qr_url")
        )

        scenario.onActivity {
            // Verify QR code is displayed initially
            assertQrCodeWebViewIsDisplayed()

            // Simulate configuration change
            scenario.recreate()

            // Verify QR code is still displayed after configuration change
            assertQrCodeWebViewIsDisplayed()
        }
    }

    @Test
    fun `Doesn't re-display QR code after configuration change`() {
        val scenario = pollingScenario(
            args = defaultArgs.copy(qrCodeUrl = "test_qr_url")
        )

        scenario.onActivity {
            assertQrCodeWebViewIsDisplayed()

            // Close the QR code web view.
            onView(withContentDescription("Close")).perform(click())

            assertQrCodeWebViewIsNotDisplayed()

            scenario.recreate()

            assertQrCodeWebViewIsNotDisplayed()
        }
    }

    private fun pollingScenario(
        args: PollingContract.Args = defaultArgs,
        poller: IntentStatusPoller = FakeIntentStatusPoller(),
        timeProvider: TimeProvider = TimeProvider { System.currentTimeMillis() },
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ): InjectableActivityScenario<PollingActivity> {
        val contract = PollingContract()
        val intent = contract.createIntent(
            context = ApplicationProvider.getApplicationContext(),
            input = args,
        )

        val viewModel = createViewModel(
            args = PollingViewModel.Args(
                args.clientSecret,
                args.timeLimitInSeconds.seconds,
                args.initialDelayInSeconds.seconds,
                args.pollingStrategy,
                args.ctaText,
                args.stripeAccountId,
                args.qrCodeUrl,
            ),
            poller = poller,
            timeProvider = timeProvider,
            savedStateHandle = savedStateHandle
        )

        val scenario = injectableActivityScenario<PollingActivity> {
            injectActivity {
                viewModelFactory = TestUtils.viewModelFactoryFor(viewModel)
            }
        }

        scenario.launchForResult(intent)

        return scenario
    }

    private fun createViewModel(
        args: PollingViewModel.Args,
        poller: IntentStatusPoller = FakeIntentStatusPoller(),
        timeProvider: TimeProvider = TimeProvider { System.currentTimeMillis() },
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ): PollingViewModel {
        return PollingViewModel(args, poller, timeProvider, savedStateHandle)
    }

    private fun waitForActivityFinish() {
        onIdle()
        composeTestRule.waitForIdle()
    }

    private fun assertQrCodeWebViewIsDisplayed() {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithTag(QR_CODE_WEB_VIEW_TEST_TAG)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun assertQrCodeWebViewIsNotDisplayed() {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithTag(QR_CODE_WEB_VIEW_TEST_TAG)
                .fetchSemanticsNodes()
                .isEmpty()
        }
    }

    private companion object {

        val defaultArgs = PollingContract.Args(
            clientSecret = "client_secret",
            statusBarColor = null,
            timeLimitInSeconds = 60,
            initialDelayInSeconds = 0,
            pollingStrategy = IntentStatusPoller.PollingStrategy.ExponentialBackoff(maxAttempts = 3),
            ctaText = R.string.stripe_upi_polling_message,
            stripeAccountId = null,
            qrCodeUrl = null,
        )
    }
}
