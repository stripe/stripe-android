package com.stripe.android.payments

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.Logger
import com.stripe.android.PaymentIntentResult
import com.stripe.android.StripeIntentResult
import com.stripe.android.exception.MaxRetryReachedException
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.StripeRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
internal class PaymentIntentFlowResultProcessorTest {
    private val testDispatcher = TestCoroutineDispatcher()

    private val mockStripeRepository: StripeRepository = mock()

    private val processor = PaymentIntentFlowResultProcessor(
        ApplicationProvider.getApplicationContext(),
        { ApiKeyFixtures.FAKE_PUBLISHABLE_KEY },
        mockStripeRepository,
        Logger.noop(),
        testDispatcher,
        mock()
    )

    @AfterTest
    fun after() {
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `processPaymentIntent() when shouldCancelSource=true should return canceled PaymentIntent`() =
        testDispatcher.runBlockingTest {
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
                        outcomeFromFlow = StripeIntentResult.Outcome.CANCELED,
                    )
                )
        }

    @Test
    fun `no refresh when user cancels the payment`() =
        testDispatcher.runBlockingTest {
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
                    flowOutcome = StripeIntentResult.Outcome.CANCELED,
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
        testDispatcher.runBlockingTest {
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
                    flowOutcome = StripeIntentResult.Outcome.SUCCEEDED,
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
                        outcomeFromFlow = StripeIntentResult.Outcome.SUCCEEDED,
                    )
                )
        }

    @Test
    fun `refresh reaches max retry user confirms the payment`() =
        testDispatcher.runBlockingTest {
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
                        flowOutcome = StripeIntentResult.Outcome.SUCCEEDED,
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
                times(PaymentIntentFlowResultProcessor.MAX_RETRIES)
            ).refreshPaymentIntent(
                eq(clientSecret),
                eq(requestOptions)
            )
        }
}
