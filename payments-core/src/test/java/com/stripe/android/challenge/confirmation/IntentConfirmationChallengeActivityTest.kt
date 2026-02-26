package com.stripe.android.challenge.confirmation

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.core.os.BundleCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.isInstanceOf
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.testing.AbsFakeStripeRepository
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.utils.InjectableActivityScenario
import com.stripe.android.utils.injectableActivityScenario
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class IntentConfirmationChallengeActivityTest {
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(testDispatcher)

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `createIntent creates correct intent with args`() {
        val args = createTestArgs()

        val intent = IntentConfirmationChallengeActivity.createIntent(context, args)

        assertThat(intent.component?.className).isEqualTo(IntentConfirmationChallengeActivity::class.java.name)
        val intentArgs = intent.extras?.let {
            BundleCompat.getParcelable(
                it,
                IntentConfirmationChallengeActivity.EXTRA_ARGS,
                IntentConfirmationChallengeArgs::class.java
            )
        }
        assertThat(intentArgs?.publishableKey).isEqualTo(args.publishableKey)
        assertThat(intentArgs?.intent).isEqualTo(args.intent)
    }

    @Test
    fun `getArgs returns correct args from SavedStateHandle`() {
        val args = createTestArgs()
        val savedStateHandle = SavedStateHandle()
        savedStateHandle[IntentConfirmationChallengeActivity.EXTRA_ARGS] = args

        val retrievedArgs = IntentConfirmationChallengeActivity.getArgs(savedStateHandle)

        assertThat(retrievedArgs).isEqualTo(args)
    }

    @Test
    fun `getArgs returns null when no args in SavedStateHandle`() {
        val savedStateHandle = SavedStateHandle()

        val retrievedArgs = IntentConfirmationChallengeActivity.getArgs(savedStateHandle)

        assertThat(retrievedArgs).isNull()
    }

    @Test
    fun `activity finishes gracefully when required args are missing`() = runTest {
        ActivityScenario.launchActivityForResult<IntentConfirmationChallengeActivity>(
            Intent(
                ApplicationProvider.getApplicationContext(),
                IntentConfirmationChallengeActivity::class.java
            )
        ).use { scenario ->
            advanceUntilIdle()

            // Activity should finish gracefully without crashing
            assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
        }
    }

    @Test
    fun `finishes with Success result when bridge handler emits Success event`() = runTest {
        val clientSecret = "pi_test_secret_123"
        val bridgeHandler = FakeConfirmationChallengeBridgeHandler()
            .apply {
                emitEvent(ConfirmationChallengeBridgeEvent.Success(clientSecret = clientSecret))
            }

        val scenario = launchActivityWithBridgeHandler(bridgeHandler)

        advanceUntilIdle()

        assertThat(scenario.getResult().resultCode).isEqualTo(IntentConfirmationChallengeActivity.RESULT_COMPLETE)

        val result = extractActivityResult(scenario)
        assertThat(result).isInstanceOf<IntentConfirmationChallengeActivityResult.Success>()
        val successResult = result as IntentConfirmationChallengeActivityResult.Success
        assertThat(successResult.clientSecret).isEqualTo(clientSecret)

        scenario.close()
    }

    @Test
    fun `finishes with Failed result when bridge handler emits Error event`() = runTest {
        val error = BridgeException(RuntimeException("Confirmation challenge failed"))
        val bridgeHandler = FakeConfirmationChallengeBridgeHandler()
            .apply {
                emitEvent(ConfirmationChallengeBridgeEvent.Error(error = error))
            }

        val scenario = launchActivityWithBridgeHandler(bridgeHandler)

        advanceUntilIdle()

        assertThat(scenario.getResult().resultCode).isEqualTo(IntentConfirmationChallengeActivity.RESULT_COMPLETE)

        val result = extractActivityResult(scenario)
        assertThat(result).isInstanceOf<IntentConfirmationChallengeActivityResult.Failed>()
        val failedResult = result as IntentConfirmationChallengeActivityResult.Failed
        assertThat(failedResult.error).isEqualTo(error)

        scenario.close()
    }

    @Test
    fun `finishes with Canceled result when close is clicked`() = runTest {
        val bridgeHandler = FakeConfirmationChallengeBridgeHandler()

        val scenario = launchActivityWithBridgeHandler(bridgeHandler)

        // Emit Ready to ensure the UI is loaded
        bridgeHandler.emitEvent(ConfirmationChallengeBridgeEvent.Ready)
        advanceUntilIdle()

        // Click the close button
        composeTestRule
            .onNodeWithTag(INTENT_CONFIRMATION_CHALLENGE_CLOSE_BUTTON_TAG)
            .performClick()
        advanceUntilIdle()

        assertThat(scenario.getResult().resultCode).isEqualTo(IntentConfirmationChallengeActivity.RESULT_COMPLETE)

        val result = extractActivityResult(scenario)
        assertThat(result).isInstanceOf<IntentConfirmationChallengeActivityResult.Canceled>()

        scenario.close()
    }

    @Test
    fun `progress indicator resets on configuration change`() = runTest {
        val bridgeHandler = FakeConfirmationChallengeBridgeHandler()

        val scenario = launchActivityWithBridgeHandler(bridgeHandler)

        // Progress indicator should be initially visible
        composeTestRule
            .onNodeWithTag(
                INTENT_CONFIRMATION_CHALLENGE_LOADER_TAG,
                useUnmergedTree = true
            )
            .assertIsDisplayed()

        // Emit Ready event to hide progress indicator
        bridgeHandler.emitEvent(ConfirmationChallengeBridgeEvent.Ready)
        advanceUntilIdle()

        // Progress indicator should now be hidden
        composeTestRule
            .onNodeWithTag(
                INTENT_CONFIRMATION_CHALLENGE_LOADER_TAG,
                useUnmergedTree = true
            )
            .assertIsNotDisplayed()

        // Recreate activity to simulate configuration change
        scenario.recreate()

        // Progress indicator should be visible again after recreation
        composeTestRule
            .onNodeWithTag(
                INTENT_CONFIRMATION_CHALLENGE_LOADER_TAG,
                useUnmergedTree = true
            )
            .assertIsDisplayed()

        scenario.close()
    }

    @Test
    fun `analytics start event is fired when activity starts`() = runTest {
        val bridgeHandler = FakeConfirmationChallengeBridgeHandler()
        val analyticsReporter = FakeIntentConfirmationChallengeAnalyticsEventReporter()

        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return IntentConfirmationChallengeViewModel(
                    args = createTestArgs(),
                    bridgeHandler = bridgeHandler,
                    workContext = testDispatcher,
                    analyticsEventReporter = analyticsReporter,
                    userAgent = "fake-user-agent",
                    stripeRepository = object : AbsFakeStripeRepository() {},
                    errorReporter = FakeErrorReporter(),
                    requestOptions = ApiRequest.Options("")
                ) as T
            }
        }

        val scenario = injectableActivityScenario<IntentConfirmationChallengeActivity> {
            injectActivity {
                viewModelFactory = factory
            }
        }.apply {
            launchForResult(createIntent())
        }

        advanceUntilIdle()

        assertThat(analyticsReporter.calls.first()).isEqualTo(
            FakeIntentConfirmationChallengeAnalyticsEventReporter.Call.Start
        )

        scenario.close()
    }

    private fun createTestArgs(): IntentConfirmationChallengeArgs {
        return IntentConfirmationChallengeArgs(
            publishableKey = "pk_test_123",
            intent = PaymentIntentFixtures.PI_SUCCEEDED,
            productUsage = listOf("PaymentSheet")
        )
    }

    private fun createTestViewModelFactory(
        bridgeHandler: ConfirmationChallengeBridgeHandler
    ): ViewModelProvider.Factory {
        return object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return IntentConfirmationChallengeViewModel(
                    args = createTestArgs(),
                    bridgeHandler = bridgeHandler,
                    workContext = testDispatcher,
                    analyticsEventReporter = FakeIntentConfirmationChallengeAnalyticsEventReporter(),
                    userAgent = "fake-user-agent",
                    stripeRepository = object : AbsFakeStripeRepository() {},
                    errorReporter = FakeErrorReporter(),
                    requestOptions = ApiRequest.Options(""),
                ) as T
            }
        }
    }

    private fun launchActivityWithBridgeHandler(
        bridgeHandler: ConfirmationChallengeBridgeHandler
    ) = injectableActivityScenario<IntentConfirmationChallengeActivity> {
        injectActivity {
            viewModelFactory = createTestViewModelFactory(bridgeHandler)
        }
    }.apply {
        launchForResult(createIntent())
    }

    private fun createIntent(): Intent {
        val args = createTestArgs()
        return Intent(
            ApplicationProvider.getApplicationContext(),
            IntentConfirmationChallengeActivity::class.java
        ).apply {
            putExtra(IntentConfirmationChallengeActivity.EXTRA_ARGS, args)
        }
    }

    private fun extractActivityResult(
        scenario: InjectableActivityScenario<IntentConfirmationChallengeActivity>
    ): IntentConfirmationChallengeActivityResult? {
        return scenario.getResult().resultData?.extras?.let {
            BundleCompat.getParcelable(
                it,
                IntentConfirmationChallengeActivityContract.EXTRA_RESULT,
                IntentConfirmationChallengeActivityResult::class.java
            )
        }
    }
}
