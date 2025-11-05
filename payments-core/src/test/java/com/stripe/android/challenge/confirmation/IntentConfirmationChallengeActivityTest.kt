package com.stripe.android.challenge.confirmation

import android.content.Context
import android.content.Intent
import androidx.core.os.BundleCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.utils.InjectableActivityScenario
import com.stripe.android.utils.injectableActivityScenario
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class IntentConfirmationChallengeActivityTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `activity should dismiss with success result when challenge succeeds`() = runTest {
        val bridgeHandler = FakeConfirmationChallengeBridgeHandler()

        val scenario = launchActivityForResult(bridgeHandler)

        // Emit success event
        bridgeHandler.emitEvent(
            ConfirmationChallengeBridgeEvent.Success(clientSecret = "pi_test_secret")
        )
        advanceUntilIdle()

        assertThat(scenario.getResult().resultCode)
            .isEqualTo(IntentConfirmationChallengeActivity.RESULT_COMPLETE)

        val result = extractActivityResult(scenario)
        assertThat(result).isInstanceOf<IntentConfirmationChallengeActivityResult.Success>()
        val successResult = result as IntentConfirmationChallengeActivityResult.Success
        assertThat(successResult.clientSecret).isEqualTo("pi_test_secret")

        // Close channel to complete the flow, then clean up activity
        bridgeHandler.close()
        advanceUntilIdle()
        scenario.close()
    }

    @Test
    fun `activity should dismiss with failed result when challenge fails`() = runTest {
        val testError = Exception("Challenge failed")
        val bridgeHandler = FakeConfirmationChallengeBridgeHandler()

        val scenario = launchActivityForResult(bridgeHandler)

        // Emit error event
        bridgeHandler.emitEvent(
            ConfirmationChallengeBridgeEvent.Error(cause = testError)
        )
        advanceUntilIdle()

        assertThat(scenario.getResult().resultCode)
            .isEqualTo(IntentConfirmationChallengeActivity.RESULT_COMPLETE)

        val result = extractActivityResult(scenario)
        assertThat(result).isInstanceOf<IntentConfirmationChallengeActivityResult.Failed>()
        val failedResult = result as IntentConfirmationChallengeActivityResult.Failed
        assertThat(failedResult.error).isEqualTo(testError)

        // Close channel to complete the flow, then clean up activity
        bridgeHandler.close()
        advanceUntilIdle()
        scenario.close()
    }

    @Test
    fun `createIntent should create proper intent with args`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val intent = IntentConfirmationChallengeActivity.createIntent(context, args)

        val intentArgs = intent.extras?.let {
            BundleCompat.getParcelable(
                it,
                IntentConfirmationChallengeActivity.EXTRA_ARGS,
                IntentConfirmationChallengeArgs::class.java
            )
        }
        assertThat(intentArgs).isEqualTo(args)
        assertThat(intentArgs?.publishableKey).isEqualTo("pk_test_123")
        assertThat(intentArgs?.intent).isEqualTo(PaymentIntentFixtures.PI_SUCCEEDED)
    }

    @Test
    fun `getArgs should return args from SavedStateHandle when present`() {
        val savedStateHandle = SavedStateHandle().apply {
            set(IntentConfirmationChallengeActivity.EXTRA_ARGS, args)
        }

        val retrievedArgs = IntentConfirmationChallengeActivity.getArgs(savedStateHandle)

        assertThat(retrievedArgs).isEqualTo(args)
        assertThat(retrievedArgs?.publishableKey).isEqualTo("pk_test_123")
        assertThat(retrievedArgs?.intent).isEqualTo(PaymentIntentFixtures.PI_SUCCEEDED)
    }

    @Test
    fun `getArgs should return null when args not present in SavedStateHandle`() {
        val savedStateHandle = SavedStateHandle()

        val retrievedArgs = IntentConfirmationChallengeActivity.getArgs(savedStateHandle)

        assertThat(retrievedArgs).isNull()
    }

    private fun launchActivityForResult(
        bridgeHandler: FakeConfirmationChallengeBridgeHandler = FakeConfirmationChallengeBridgeHandler(),
    ) = injectableActivityScenario<IntentConfirmationChallengeActivity> {
        injectActivity {
            viewModelFactory = createTestViewModelFactory(bridgeHandler)
        }
    }.apply {
        launchForResult(createIntent())
    }

    private fun createIntent() = Intent(
        ApplicationProvider.getApplicationContext(),
        IntentConfirmationChallengeActivity::class.java
    ).apply {
        putExtra(IntentConfirmationChallengeActivity.EXTRA_ARGS, args)
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

    private fun createTestViewModelFactory(
        bridgeHandler: ConfirmationChallengeBridgeHandler = FakeConfirmationChallengeBridgeHandler()
    ): ViewModelProvider.Factory {
        return object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return IntentConfirmationChallengeViewModel(
                    bridgeHandler = bridgeHandler
                ) as T
            }
        }
    }

    private class FakeConfirmationChallengeBridgeHandler : ConfirmationChallengeBridgeHandler {
        private val _event = Channel<ConfirmationChallengeBridgeEvent>(Channel.UNLIMITED)
        override val event = _event.receiveAsFlow()

        suspend fun emitEvent(event: ConfirmationChallengeBridgeEvent) {
            _event.send(event)
        }

        fun close() {
            _event.close()
        }

        override fun getInitParams(): String = "{}"
        override fun onReady() {}
        override fun onSuccess(paymentIntentJson: String) {}
        override fun onError(errorMessage: String) {}
        override fun logConsole(logData: String) {}
        override fun ready(message: String) {}
    }

    companion object {
        private val args = IntentConfirmationChallengeArgs(
            publishableKey = "pk_test_123",
            intent = PaymentIntentFixtures.PI_SUCCEEDED
        )
    }
}