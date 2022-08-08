package com.stripe.android.payments

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentIntentResult
import com.stripe.android.StripeIntentResult
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.MaxRetryReachedException
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.StripeRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
internal class PaymentIntentFlowResultProcessorTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private val mockStripeRepository: StripeRepository = mock()

    private val processor = PaymentIntentFlowResultProcessor(
        ApplicationProvider.getApplicationContext(),
        { ApiKeyFixtures.FAKE_PUBLISHABLE_KEY },
        mockStripeRepository,
        Logger.noop(),
        testDispatcher
    )

    @Test
    fun `processPaymentIntent() when shouldCancelSource=true should return canceled PaymentIntent`() =
        runTest {
            whenever(mockStripeRepository.retrievePaymentIntent(any(), any(), any())).thenReturn(
                PaymentIntentFixtures.PI_REQUIRES_REDIRECT
            )
            whenever(
                mockStripeRepository.cancelPaymentIntentSource(
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(
                PaymentIntentFixtures.CANCELLED
            )

            val paymentIntentResult = processor.processResult(
                PaymentFlowResult.Unvalidated(
                    clientSecret = "client_secret",
                    flowOutcome = StripeIntentResult.Outcome.CANCELED,
                    canCancelSource = true
                )
            )

            assertThat(paymentIntentResult)
                .isEqualTo(
                    PaymentIntentResult(
                        intent = PaymentIntentFixtures.CANCELLED,
                        outcomeFromFlow = StripeIntentResult.Outcome.CANCELED
                    )
                )
        }

    @Test
    fun `when 3DS2 data contains intentId and publishableKey then they are used on source cancel`() =
        runTest {
            whenever(mockStripeRepository.retrievePaymentIntent(any(), any(), any()))
                .thenReturn(PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2)
            whenever(mockStripeRepository.cancelPaymentIntentSource(any(), any(), any()))
                .thenReturn(PaymentIntentFixtures.PAYMENT_INTENT_WITH_CANCELED_3DS2_SOURCE)

            processor.processResult(
                PaymentFlowResult.Unvalidated(
                    clientSecret = "client_secret",
                    sourceId = "source_id",
                    flowOutcome = StripeIntentResult.Outcome.CANCELED,
                    canCancelSource = true
                )
            )

            verify(mockStripeRepository).cancelPaymentIntentSource(
                eq("pi_1ExkUeAWhjPjYwPiLWUvXrSA"),
                eq("source_id"),
                eq(ApiRequest.Options("pk_test_nextActionData"))
            )
        }

    @Test
    fun `no refresh when user cancels the payment`() =
        runTest {
            whenever(mockStripeRepository.retrievePaymentIntent(any(), any(), any())).thenReturn(
                PaymentIntentFixtures.PI_REQUIRES_WECHAT_PAY_AUTHORIZE
            )
            whenever(mockStripeRepository.refreshPaymentIntent(any(), any())).thenReturn(
                PaymentIntentFixtures.PI_REFRESH_RESPONSE_WECHAT_PAY_SUCCESS
            )

            val clientSecret = "pi_3JkCxKBNJ02ErVOj0kNqBMAZ_secret_bC6oXqo976LFM06Z9rlhmzUQq"
            val requestOptions = ApiRequest.Options(apiKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)

            val paymentIntentResult = processor.processResult(
                PaymentFlowResult.Unvalidated(
                    clientSecret = clientSecret,
                    flowOutcome = StripeIntentResult.Outcome.CANCELED
                )
            )

            verify(mockStripeRepository).retrievePaymentIntent(
                eq(clientSecret),
                eq(requestOptions),
                eq(PaymentFlowResultProcessor.EXPAND_PAYMENT_METHOD)
            )

            verify(mockStripeRepository, never()).refreshPaymentIntent(
                eq(clientSecret),
                eq(requestOptions)
            )

            assertThat(paymentIntentResult)
                .isEqualTo(
                    PaymentIntentResult(
                        intent = PaymentIntentFixtures.PI_REQUIRES_WECHAT_PAY_AUTHORIZE,
                        outcomeFromFlow = StripeIntentResult.Outcome.CANCELED,
                        failureMessage = "We are unable to authenticate your payment method. " +
                            "Please choose a different payment method and try again."
                    )
                )
        }

    @Test
    fun `refresh succeeds when user confirms the payment`() =
        runTest {
            whenever(mockStripeRepository.retrievePaymentIntent(any(), any(), any())).thenReturn(
                PaymentIntentFixtures.PI_REQUIRES_WECHAT_PAY_AUTHORIZE
            )
            whenever(mockStripeRepository.refreshPaymentIntent(any(), any())).thenReturn(
                PaymentIntentFixtures.PI_REFRESH_RESPONSE_WECHAT_PAY_SUCCESS
            )

            val clientSecret = "pi_3JkCxKBNJ02ErVOj0kNqBMAZ_secret_bC6oXqo976LFM06Z9rlhmzUQq"
            val requestOptions = ApiRequest.Options(apiKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)

            val paymentIntentResult = processor.processResult(
                PaymentFlowResult.Unvalidated(
                    clientSecret = clientSecret,
                    flowOutcome = StripeIntentResult.Outcome.SUCCEEDED
                )
            )

            verify(mockStripeRepository).retrievePaymentIntent(
                eq(clientSecret),
                eq(requestOptions),
                eq(PaymentFlowResultProcessor.EXPAND_PAYMENT_METHOD)
            )

            verify(mockStripeRepository).refreshPaymentIntent(
                eq(clientSecret),
                eq(requestOptions)
            )

            assertThat(paymentIntentResult)
                .isEqualTo(
                    PaymentIntentResult(
                        intent = PaymentIntentFixtures.PI_REFRESH_RESPONSE_WECHAT_PAY_SUCCESS,
                        outcomeFromFlow = StripeIntentResult.Outcome.SUCCEEDED
                    )
                )
        }

    @Test
    fun `refresh reaches max retry user confirms the payment`() =
        runTest(testDispatcher) {
            whenever(mockStripeRepository.retrievePaymentIntent(any(), any(), any())).thenReturn(
                PaymentIntentFixtures.PI_REQUIRES_WECHAT_PAY_AUTHORIZE
            )
            whenever(mockStripeRepository.refreshPaymentIntent(any(), any())).thenReturn(
                PaymentIntentFixtures.PI_REFRESH_RESPONSE_REQUIRES_WECHAT_PAY_AUTHORIZE
            )

            val clientSecret = "pi_3JkCxKBNJ02ErVOj0kNqBMAZ_secret_bC6oXqo976LFM06Z9rlhmzUQq"
            val requestOptions = ApiRequest.Options(apiKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)

            assertFailsWith<MaxRetryReachedException> {
                processor.processResult(
                    PaymentFlowResult.Unvalidated(
                        clientSecret = clientSecret,
                        flowOutcome = StripeIntentResult.Outcome.SUCCEEDED
                    )
                )
            }

            verify(mockStripeRepository).retrievePaymentIntent(
                eq(clientSecret),
                eq(requestOptions),
                eq(PaymentFlowResultProcessor.EXPAND_PAYMENT_METHOD)
            )

            verify(
                mockStripeRepository,
                times(PaymentFlowResultProcessor.MAX_RETRIES)
            ).refreshPaymentIntent(
                eq(clientSecret),
                eq(requestOptions)
            )
        }

    @Test
    fun `3ds2 canceled with processing intent should succeed`() =
        runTest {
            val initialIntent = PaymentIntentFixtures.PI_VISA_3DS2.copy(
                status = StripeIntent.Status.Processing
            )
            val refreshedIntent = PaymentIntentFixtures.PI_VISA_3DS2.copy(
                status = StripeIntent.Status.Succeeded
            )
            runCanceledFlow(
                initialIntent = initialIntent,
                refreshedIntent = refreshedIntent
            )
        }

    @Test
    fun `3ds2 canceled with requires capture intent should succeed`() =
        runTest {
            val initialIntent = PaymentIntentFixtures.PI_VISA_3DS2.copy(
                status = StripeIntent.Status.Processing
            )
            val refreshedIntent = PaymentIntentFixtures.PI_VISA_3DS2.copy(
                status = StripeIntent.Status.RequiresCapture
            )
            runCanceledFlow(
                initialIntent = initialIntent,
                refreshedIntent = refreshedIntent
            )
        }

    @Test
    fun `3ds2 canceled with succeeded intent should succeed`() =
        runTest {
            runCanceledFlow(
                initialIntent = PaymentIntentFixtures.PI_VISA_3DS2.copy(
                    status = StripeIntent.Status.Succeeded
                )
            )
        }

    @Test
    fun `3ds2 canceled reaches max retry with processing intent should cancel`() =
        runTest(testDispatcher) {
            val intent = PaymentIntentFixtures.PI_VISA_3DS2.copy(
                status = StripeIntent.Status.RequiresAction
            )
            whenever(mockStripeRepository.retrievePaymentIntent(any(), any(), any())).thenReturn(
                intent
            )

            val clientSecret = requireNotNull(
                intent.clientSecret
            )
            val requestOptions = ApiRequest.Options(apiKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)

            val result = processor.processResult(
                PaymentFlowResult.Unvalidated(
                    clientSecret = clientSecret,
                    flowOutcome = StripeIntentResult.Outcome.CANCELED
                )
            )

            verify(
                mockStripeRepository,
                times(PaymentFlowResultProcessor.MAX_RETRIES + 1)
            ).retrievePaymentIntent(
                eq(clientSecret),
                eq(requestOptions),
                eq(PaymentFlowResultProcessor.EXPAND_PAYMENT_METHOD)
            )

            assertThat(result)
                .isEqualTo(
                    PaymentIntentResult(
                        intent,
                        StripeIntentResult.Outcome.CANCELED,
                        null
                    )
                )
        }

    private suspend fun runCanceledFlow(
        initialIntent: PaymentIntent,
        refreshedIntent: PaymentIntent = initialIntent,
        expectedIntent: PaymentIntent = refreshedIntent
    ) {
        whenever(mockStripeRepository.retrievePaymentIntent(any(), any(), any())).thenReturn(
            initialIntent,
            refreshedIntent
        )

        val clientSecret = requireNotNull(initialIntent.clientSecret)
        val requestOptions = ApiRequest.Options(apiKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)

        val result = processor.processResult(
            PaymentFlowResult.Unvalidated(
                clientSecret = clientSecret,
                flowOutcome = StripeIntentResult.Outcome.CANCELED
            )
        )

        verify(mockStripeRepository, atLeastOnce()).retrievePaymentIntent(
            eq(clientSecret),
            eq(requestOptions),
            eq(PaymentFlowResultProcessor.EXPAND_PAYMENT_METHOD)
        )

        assertThat(result)
            .isEqualTo(
                PaymentIntentResult(
                    expectedIntent,
                    StripeIntentResult.Outcome.SUCCEEDED,
                    null
                )
            )
    }
}
