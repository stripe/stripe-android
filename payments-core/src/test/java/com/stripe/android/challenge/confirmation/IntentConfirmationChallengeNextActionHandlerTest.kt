package com.stripe.android.challenge.confirmation

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.testing.TestLifecycleOwner
import app.cash.turbine.Turbine
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
import com.stripe.android.testing.asCallbackFor
import com.stripe.android.view.AuthActivityStarterHost
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class IntentConfirmationChallengeNextActionHandlerTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @Test
    fun `performNextActionOnResumed uses Modern starter when launcher is set`() = runTest {
        DummyActivityResultCaller.test {
            val handler = IntentConfirmationChallengeNextActionHandler(
                publishableKeyProvider = { ApiKeyFixtures.FAKE_PUBLISHABLE_KEY },
                uiContext = testDispatcher,
                productUsageTokens = PRODUCT_USAGE
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
                FakeAuthActivityStarterHost(),
                paymentIntent,
                REQUEST_OPTIONS
            )

            val launchArgs = awaitLaunchCall() as IntentConfirmationChallengeActivityContract.Args
            assertThat(launchArgs.publishableKey).isEqualTo(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
            assertThat(launchArgs.intent).isEqualTo(paymentIntent)
        }
    }

    @Test
    fun `performNextActionOnResumed uses Legacy starter when launcher is null`() = runTest {
        val handler = IntentConfirmationChallengeNextActionHandler(
            publishableKeyProvider = { ApiKeyFixtures.FAKE_PUBLISHABLE_KEY },
            uiContext = testDispatcher,
            productUsageTokens = PRODUCT_USAGE
        )
        val host = FakeAuthActivityStarterHost()

        val paymentIntent = createTestPaymentIntent()

        handler.performNextAction(
            host,
            paymentIntent,
            REQUEST_OPTIONS
        )

        val startActivityForResultCall = host.calls.awaitItem()
        assertThat(startActivityForResultCall.target).isEqualTo(IntentConfirmationChallengeActivity::class.java)
        host.calls.ensureAllEventsConsumed()
    }

    @Test
    fun `onNewActivityResultCaller registers for activity result and handles Success`() = runTest {
        DummyActivityResultCaller.test {
            val handler = IntentConfirmationChallengeNextActionHandler(
                publishableKeyProvider = { ApiKeyFixtures.FAKE_PUBLISHABLE_KEY },
                uiContext = testDispatcher,
                productUsageTokens = PRODUCT_USAGE
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
            val callback = registerCall.callback.asCallbackFor<IntentConfirmationChallengeActivityResult>()
            callback.onActivityResult(successResult)

            assertThat(resultCallback).hasSize(1)
            val capturedResult = resultCallback[0]
            assertThat(capturedResult.clientSecret).isEqualTo("pi_test_secret")
            assertThat(capturedResult.flowOutcome).isEqualTo(StripeIntentResult.Outcome.UNKNOWN)
            assertThat(capturedResult.exception).isNull()
        }
    }

    @Test
    fun `onNewActivityResultCaller registers for activity result and handles Failed`() = runTest {
        DummyActivityResultCaller.test {
            val handler = IntentConfirmationChallengeNextActionHandler(
                publishableKeyProvider = { ApiKeyFixtures.FAKE_PUBLISHABLE_KEY },
                uiContext = testDispatcher,
                productUsageTokens = PRODUCT_USAGE
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
                clientSecret = null,
                error = testError
            )
            val callback = registerCall.callback.asCallbackFor<IntentConfirmationChallengeActivityResult>()
            callback.onActivityResult(failedResult)

            assertThat(resultCallback).hasSize(1)
            val capturedResult = resultCallback[0]
            assertThat(capturedResult.flowOutcome).isEqualTo(StripeIntentResult.Outcome.FAILED)
            assertThat(capturedResult.exception).isInstanceOf(StripeException::class.java)
            assertThat(capturedResult.exception?.message).isEqualTo("Test error")
        }
    }

    @Test
    fun `onNewActivityResultCaller registers for activity result and handles Canceled`() = runTest {
        DummyActivityResultCaller.test {
            val handler = IntentConfirmationChallengeNextActionHandler(
                publishableKeyProvider = { ApiKeyFixtures.FAKE_PUBLISHABLE_KEY },
                uiContext = testDispatcher,
                productUsageTokens = PRODUCT_USAGE
            )

            val resultCallback = mutableListOf<PaymentFlowResult.Unvalidated>()
            handler.onNewActivityResultCaller(
                activityResultCaller = activityResultCaller,
                activityResultCallback = { result -> resultCallback.add(result) }
            )

            val registerCall = awaitRegisterCall()
            awaitNextRegisteredLauncher()
            assertThat(registerCall.contract).isInstanceOf(IntentConfirmationChallengeActivityContract::class.java)

            // Simulate canceled result
            val callback = registerCall.callback.asCallbackFor<IntentConfirmationChallengeActivityResult>()
            callback.onActivityResult(
                IntentConfirmationChallengeActivityResult.Canceled(clientSecret = "pi_test_secret")
            )

            assertThat(resultCallback).hasSize(1)
            val capturedResult = resultCallback[0]
            assertThat(capturedResult.flowOutcome).isEqualTo(StripeIntentResult.Outcome.CANCELED)
            assertThat(capturedResult.exception).isNull()
        }
    }

    @Test
    fun `onNewActivityResultCaller sets the launcher on handler`() = runTest {
        DummyActivityResultCaller.test {
            val handler = IntentConfirmationChallengeNextActionHandler(
                publishableKeyProvider = { ApiKeyFixtures.FAKE_PUBLISHABLE_KEY },
                uiContext = testDispatcher,
                productUsageTokens = PRODUCT_USAGE
            )

            handler.onNewActivityResultCaller(
                activityResultCaller = activityResultCaller,
                activityResultCallback = {}
            )

            awaitRegisterCall()
            awaitNextRegisteredLauncher()
        }
    }

    private fun createTestPaymentIntent(): PaymentIntent {
        return PaymentIntentFixtures.PI_SUCCEEDED.copy(
            nextActionData = StripeIntent.NextActionData.SdkData.IntentConfirmationChallenge(
                stripeJs = StripeIntent.NextActionData.SdkData.IntentConfirmationChallenge.StripeJs(
                    siteKey = "test_site_key",
                    verificationUrl = "/v1/payment_intents/pi_123/verify_challenge",
                )
            )
        )
    }

    private class FakeAuthActivityStarterHost : AuthActivityStarterHost {
        val calls = Turbine<Call>()
        override fun startActivityForResult(target: Class<*>, extras: Bundle, requestCode: Int) {
            calls.add(
                item = Call(
                    target = target,
                    extras = extras,
                    requestCode = requestCode
                )
            )
        }

        override val statusBarColor: Int? = null
        override val lifecycleOwner: LifecycleOwner = TestLifecycleOwner(initialState = Lifecycle.State.RESUMED)
        override val application: Application
            get() {
                throw NotImplementedError()
            }

        data class Call(
            val target: Class<*>,
            val extras: Bundle,
            val requestCode: Int
        )
    }

    private companion object {
        private const val ACCOUNT_ID = "acct_123"

        private val REQUEST_OPTIONS = ApiRequest.Options(
            apiKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            stripeAccount = ACCOUNT_ID
        )

        private val PRODUCT_USAGE = setOf("PaymentSheet")
    }
}
