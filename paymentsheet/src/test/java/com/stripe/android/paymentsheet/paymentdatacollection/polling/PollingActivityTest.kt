package com.stripe.android.paymentsheet.paymentdatacollection.polling

import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.StripeIntentResult
import com.stripe.android.common.ui.performClickWithKeyboard
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.paymentsheet.R
import com.stripe.android.polling.IntentStatusPoller
import com.stripe.android.utils.InjectableActivityScenario
import com.stripe.android.utils.TestUtils
import com.stripe.android.utils.injectableActivityScenario
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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

    private val testDispatcher = UnconfinedTestDispatcher()

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
                .performClickWithKeyboard()

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
                .performClickWithKeyboard()

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

    private fun pollingScenario(
        args: PollingContract.Args = defaultArgs,
        poller: IntentStatusPoller = FakeIntentStatusPoller(),
        timeProvider: TimeProvider = TimeProvider { System.currentTimeMillis() },
        dispatcher: CoroutineDispatcher = testDispatcher,
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
                args.maxAttempts,
                args.ctaText,
            ),
            poller = poller,
            timeProvider = timeProvider,
            dispatcher = dispatcher,
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
        dispatcher: CoroutineDispatcher = testDispatcher,
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ): PollingViewModel {
        return PollingViewModel(args, poller, timeProvider, dispatcher, savedStateHandle)
    }

    private fun waitForActivityFinish() {
        onIdle()
        composeTestRule.waitForIdle()
    }

    private companion object {

        val defaultArgs = PollingContract.Args(
            clientSecret = "client_secret",
            initialDelayInSeconds = 0,
            timeLimitInSeconds = 60,
            maxAttempts = 3,
            statusBarColor = null,
            ctaText = R.string.stripe_upi_polling_message,
        )
    }
}
