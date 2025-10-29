package com.stripe.android.challenge.confirmation

import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
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
import com.stripe.android.view.AuthActivityStarterHost
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class IntentConfirmationChallengeNextActionHandlerTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
    private val productUsageTokens = setOf("TestToken")

    private val host = mock<AuthActivityStarterHost> {
        on { lifecycleOwner } doReturn TestLifecycleOwner(initialState = Lifecycle.State.RESUMED)
    }

    private val mockActivityResultLauncher =
        mock<ActivityResultLauncher<IntentConfirmationChallengeActivityContract.Args>>()

    private val argsCaptor: KArgumentCaptor<IntentConfirmationChallengeActivityContract.Args> = argumentCaptor()

    @Test
    fun `performNextActionOnResumed uses Modern starter when launcher is set`() = runTest {
        val handler = IntentConfirmationChallengeNextActionHandler(
            publishableKeyProvider = { publishableKey },
            productUsageTokens = productUsageTokens,
            uiContext = testDispatcher
        )

        handler.intentConfirmationChallengeActivityContractNextActionLauncher = mockActivityResultLauncher

        val paymentIntent = createTestPaymentIntent()

        handler.performNextAction(
            host,
            paymentIntent,
            REQUEST_OPTIONS
        )

        verify(mockActivityResultLauncher).launch(argsCaptor.capture())

        val capturedArgs = argsCaptor.firstValue
        assertThat(capturedArgs.publishableKey).isEqualTo(publishableKey)
        assertThat(capturedArgs.productUsage).isEqualTo(productUsageTokens)
        assertThat(capturedArgs.intent).isEqualTo(paymentIntent)
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

    @Test
    fun `onNewActivityResultCaller registers for activity result and handles Success`() {
        val handler = IntentConfirmationChallengeNextActionHandler(
            publishableKeyProvider = { publishableKey },
            productUsageTokens = productUsageTokens,
            uiContext = testDispatcher
        )

        val mockActivityResultCaller = mock<ActivityResultCaller>()
        val mockCallback = mock<ActivityResultCallback<PaymentFlowResult.Unvalidated>>()
        val resultCaptor: KArgumentCaptor<PaymentFlowResult.Unvalidated> = argumentCaptor()

        val successResult = IntentConfirmationChallengeActivityResult.Success(
            clientSecret = "pi_test_secret"
        )

        // Set up the mock to capture the callback and invoke it with successResult
        whenever(
            mockActivityResultCaller.registerForActivityResult(
                any<IntentConfirmationChallengeActivityContract>(),
                any()
            )
        ).thenAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            val callback = invocation.getArgument<ActivityResultCallback<IntentConfirmationChallengeActivityResult>>(1)
            // Invoke the callback immediately with our test result
            callback.onActivityResult(successResult)
            mockActivityResultLauncher
        }

        handler.onNewActivityResultCaller(mockActivityResultCaller, mockCallback)

        verify(mockCallback).onActivityResult(resultCaptor.capture())

        val capturedResult = resultCaptor.firstValue
        assertThat(capturedResult.clientSecret).isEqualTo("pi_test_secret")
        assertThat(capturedResult.flowOutcome).isEqualTo(StripeIntentResult.Outcome.UNKNOWN)
        assertThat(capturedResult.exception).isNull()
    }

    @Test
    fun `onNewActivityResultCaller registers for activity result and handles Failed`() {
        val handler = IntentConfirmationChallengeNextActionHandler(
            publishableKeyProvider = { publishableKey },
            productUsageTokens = productUsageTokens,
            uiContext = testDispatcher
        )

        val mockActivityResultCaller = mock<ActivityResultCaller>()
        val mockCallback = mock<ActivityResultCallback<PaymentFlowResult.Unvalidated>>()
        val resultCaptor: KArgumentCaptor<PaymentFlowResult.Unvalidated> = argumentCaptor()

        val testError = Throwable("Test error")
        val failedResult = IntentConfirmationChallengeActivityResult.Failed(
            error = testError
        )

        // Set up the mock to capture the callback and invoke it with failedResult
        whenever(
            mockActivityResultCaller.registerForActivityResult(
                any<IntentConfirmationChallengeActivityContract>(),
                any()
            )
        ).thenAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            val callback = invocation.getArgument<ActivityResultCallback<IntentConfirmationChallengeActivityResult>>(1)
            // Invoke the callback immediately with our test result
            callback.onActivityResult(failedResult)
            mockActivityResultLauncher
        }

        handler.onNewActivityResultCaller(mockActivityResultCaller, mockCallback)

        verify(mockCallback).onActivityResult(resultCaptor.capture())

        val capturedResult = resultCaptor.firstValue
        assertThat(capturedResult.flowOutcome).isEqualTo(StripeIntentResult.Outcome.FAILED)
        assertThat(capturedResult.exception).isInstanceOf(StripeException::class.java)
        assertThat(capturedResult.exception?.message).isEqualTo("Test error")
    }

    @Test
    fun `onNewActivityResultCaller sets the launcher on handler`() {
        val handler = IntentConfirmationChallengeNextActionHandler(
            publishableKeyProvider = { publishableKey },
            productUsageTokens = productUsageTokens,
            uiContext = testDispatcher
        )

        val mockActivityResultCaller = mock<ActivityResultCaller>()
        val mockCallback = mock<ActivityResultCallback<PaymentFlowResult.Unvalidated>>()

        whenever(
            mockActivityResultCaller.registerForActivityResult(
                any<IntentConfirmationChallengeActivityContract>(),
                any()
            )
        ).thenReturn(mockActivityResultLauncher)

        assertThat(handler.intentConfirmationChallengeActivityContractNextActionLauncher).isNull()

        handler.onNewActivityResultCaller(mockActivityResultCaller, mockCallback)

        assertThat(handler.intentConfirmationChallengeActivityContractNextActionLauncher)
            .isEqualTo(mockActivityResultLauncher)
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
