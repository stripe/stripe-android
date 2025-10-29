package com.stripe.android.challenge.confirmation

import androidx.activity.result.ActivityResultCallback
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.StripeIntentResult
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.testing.DummyActivityResultCaller
import com.stripe.android.view.AuthActivityStarterHost
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class IntentConfirmationChallengeNextActionHandlerTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
    private val productUsageTokens = setOf("TestToken")

    private val host = mock<AuthActivityStarterHost> {
        on { lifecycleOwner } doReturn TestLifecycleOwner(initialState = Lifecycle.State.RESUMED)
    }

    @Test
    fun `performNextActionOnResumed uses Modern starter when launcher is set`() = runTest {
        DummyActivityResultCaller.test {
            val handler = IntentConfirmationChallengeNextActionHandler(
                publishableKeyProvider = { publishableKey },
                productUsageTokens = productUsageTokens,
                uiContext = testDispatcher
            )

            val resultCallback = mutableListOf<PaymentFlowResult.Unvalidated>()
            handler.onNewActivityResultCaller(
                activityResultCaller = activityResultCaller,
                activityResultCallback = { result -> resultCallback.add(result) }
            )

            // Wait for registration to complete
            awaitRegisterCall()
            awaitNextRegisteredLauncher()

            val paymentIntent = createTestPaymentIntent()

            handler.performNextAction(
                host,
                paymentIntent,
                REQUEST_OPTIONS
            )

            val launchArgs = awaitLaunchCall() as IntentConfirmationChallengeActivityContract.Args
            assertThat(launchArgs.publishableKey).isEqualTo(publishableKey)
            assertThat(launchArgs.productUsage).isEqualTo(productUsageTokens)
            assertThat(launchArgs.intent).isEqualTo(paymentIntent)
        }
    }

    @Test
    fun `performNextActionOnResumed uses Legacy starter when launcher is null`() = runTest {
        val handler = IntentConfirmationChallengeNextActionHandler(
            publishableKeyProvider = { publishableKey },
            productUsageTokens = productUsageTokens,
            uiContext = testDispatcher
        )

        // Ensure launcher is null (default state)
        handler.intentConfirmationChallengeActivityContractNextActionLauncher = null

        val paymentIntent = createTestPaymentIntent()

        handler.performNextAction(
            host,
            paymentIntent,
            REQUEST_OPTIONS
        )

        verify(host).startActivityForResult(
            target = eq(IntentConfirmationChallengeActivity::class.java),
            extras = any(),
            requestCode = any()
        )
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `onNewActivityResultCaller registers for activity result and handles Success`() = runTest {
        DummyActivityResultCaller.test {
            val handler = IntentConfirmationChallengeNextActionHandler(
                publishableKeyProvider = { publishableKey },
                productUsageTokens = productUsageTokens,
                uiContext = testDispatcher
            )

            val resultCallback = mutableListOf<PaymentFlowResult.Unvalidated>()
            handler.onNewActivityResultCaller(
                activityResultCaller = activityResultCaller,
                activityResultCallback = { result -> resultCallback.add(result) }
            )

            val registerCall = awaitRegisterCall()
            awaitNextRegisteredLauncher()
            assertThat(registerCall.contract).isInstanceOf(IntentConfirmationChallengeActivityContract::class.java)

            // Simulate successful result
            val successResult = IntentConfirmationChallengeActivityResult.Success(
                clientSecret = "pi_test_secret"
            )
            val callback = registerCall.callback as ActivityResultCallback<IntentConfirmationChallengeActivityResult>
            callback.onActivityResult(successResult)

            assertThat(resultCallback).hasSize(1)
            val capturedResult = resultCallback[0]
            assertThat(capturedResult.clientSecret).isEqualTo("pi_test_secret")
            assertThat(capturedResult.flowOutcome).isEqualTo(StripeIntentResult.Outcome.UNKNOWN)
            assertThat(capturedResult.exception).isNull()
        }
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `onNewActivityResultCaller registers for activity result and handles Failed`() = runTest {
        DummyActivityResultCaller.test {
            val handler = IntentConfirmationChallengeNextActionHandler(
                publishableKeyProvider = { publishableKey },
                productUsageTokens = productUsageTokens,
                uiContext = testDispatcher
            )

            val resultCallback = mutableListOf<PaymentFlowResult.Unvalidated>()
            handler.onNewActivityResultCaller(
                activityResultCaller = activityResultCaller,
                activityResultCallback = { result -> resultCallback.add(result) }
            )

            val registerCall = awaitRegisterCall()
            awaitNextRegisteredLauncher()
            assertThat(registerCall.contract).isInstanceOf(IntentConfirmationChallengeActivityContract::class.java)

            // Simulate failed result
            val testError = Throwable("Test error")
            val failedResult = IntentConfirmationChallengeActivityResult.Failed(
                error = testError
            )
            val callback = registerCall.callback as ActivityResultCallback<IntentConfirmationChallengeActivityResult>
            callback.onActivityResult(failedResult)

            assertThat(resultCallback).hasSize(1)
            val capturedResult = resultCallback[0]
            assertThat(capturedResult.flowOutcome).isEqualTo(StripeIntentResult.Outcome.FAILED)
            assertThat(capturedResult.exception).isInstanceOf(StripeException::class.java)
            assertThat(capturedResult.exception?.message).isEqualTo("Test error")
        }
    }

    @Test
    fun `onNewActivityResultCaller sets the launcher on handler`() = runTest {
        DummyActivityResultCaller.test {
            val handler = IntentConfirmationChallengeNextActionHandler(
                publishableKeyProvider = { publishableKey },
                productUsageTokens = productUsageTokens,
                uiContext = testDispatcher
            )

            assertThat(handler.intentConfirmationChallengeActivityContractNextActionLauncher).isNull()

            handler.onNewActivityResultCaller(
                activityResultCaller = activityResultCaller,
                activityResultCallback = {}
            )

            awaitRegisterCall()
            val launcher = awaitNextRegisteredLauncher()
            assertThat(handler.intentConfirmationChallengeActivityContractNextActionLauncher)
                .isEqualTo(launcher)
        }
    }

    private fun createTestPaymentIntent(): PaymentIntent {
        return PaymentIntentFixtures.PI_SUCCEEDED.copy(
            nextActionData = StripeIntent.NextActionData.SdkData.IntentConfirmationChallenge
        )
    }

    private companion object {
        private const val ACCOUNT_ID = "acct_123"

        private val REQUEST_OPTIONS = ApiRequest.Options(
            apiKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            stripeAccount = ACCOUNT_ID
        )
    }
}
