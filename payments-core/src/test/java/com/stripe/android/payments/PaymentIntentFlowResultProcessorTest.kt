package com.stripe.android.payments

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentIntentResult
import com.stripe.android.StripeIntentResult
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.PaymentFlowResultProcessor.Companion.MAX_POLLING_DURATION
import com.stripe.android.payments.PaymentFlowResultProcessor.Companion.POLLING_DELAY
import com.stripe.android.payments.PaymentFlowResultProcessor.Companion.REDUCED_POLLING_DURATION
import com.stripe.android.payments.PaymentIntentFlowResultProcessorTest.Companion.MINIMUM_REFRESH_CALLS
import com.stripe.android.payments.PaymentIntentFlowResultProcessorTest.Companion.MINIMUM_RETRIEVE_CALLS
import com.stripe.android.testing.AbsFakeStripeRepository
import com.stripe.android.testing.PaymentMethodFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.atMost
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class PaymentIntentFlowResultProcessorTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private val mockStripeRepository: StripeRepository = mock()

    @Test
    fun `processPaymentIntent() when shouldCancelSource=true should return canceled PaymentIntent`() =
        runTest(testDispatcher) {
            whenever(mockStripeRepository.retrievePaymentIntent(any(), any(), any())).thenReturn(
                Result.success(PaymentIntentFixtures.PI_REQUIRES_REDIRECT)
            )
            whenever(
                mockStripeRepository.cancelPaymentIntentSource(
                    paymentIntentId = any(),
                    sourceId = any(),
                    options = any(),
                )
            ).thenReturn(
                Result.success(PaymentIntentFixtures.CANCELLED)
            )

            val paymentIntentResult = createProcessor().processResult(
                PaymentFlowResult.Unvalidated(
                    clientSecret = "client_secret",
                    flowOutcome = StripeIntentResult.Outcome.CANCELED,
                    canCancelSource = true
                )
            ).getOrThrow()

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
        runTest(testDispatcher) {
            whenever(mockStripeRepository.retrievePaymentIntent(any(), any(), any()))
                .thenReturn(Result.success(PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2))
            whenever(mockStripeRepository.cancelPaymentIntentSource(any(), any(), any()))
                .thenReturn(Result.success(PaymentIntentFixtures.PAYMENT_INTENT_WITH_CANCELED_3DS2_SOURCE))

            createProcessor().processResult(
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
    fun `no refresh when user cancels the payment`() = runTest(testDispatcher) {
        whenever(mockStripeRepository.retrievePaymentIntent(any(), any(), any())).thenReturn(
            Result.success(PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD)
        )

        whenever(mockStripeRepository.refreshPaymentIntent(any(), any())).thenThrow(
            AssertionError("No expected to call refresh in this test")
        )

        val clientSecret = "pi_3JkCxKBNJ02ErVOj0kNqBMAZ_secret_bC6oXqo976LFM06Z9rlhmzUQq"
        val requestOptions = ApiRequest.Options(apiKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)

        verify(mockStripeRepository, never()).refreshPaymentIntent(
            eq(clientSecret),
            eq(requestOptions)
        )
    }

    @Test
    fun `refresh succeeds when user confirms the payment`() =
        runTest(testDispatcher) {
            whenever(mockStripeRepository.retrievePaymentIntent(any(), any(), any())).thenReturn(
                Result.success(PaymentIntentFixtures.PI_REQUIRES_WECHAT_PAY_AUTHORIZE)
            )
            whenever(mockStripeRepository.refreshPaymentIntent(any(), any())).thenReturn(
                Result.success(PaymentIntentFixtures.PI_REFRESH_RESPONSE_WECHAT_PAY_SUCCESS)
            )

            val clientSecret = "pi_3JkCxKBNJ02ErVOj0kNqBMAZ_secret_bC6oXqo976LFM06Z9rlhmzUQq"
            val requestOptions = ApiRequest.Options(apiKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)

            val paymentIntentResult = createProcessor().processResult(
                PaymentFlowResult.Unvalidated(
                    clientSecret = clientSecret,
                    flowOutcome = StripeIntentResult.Outcome.SUCCEEDED
                )
            ).getOrThrow()

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
    fun `refresh reaches max time user confirms the payment`() =
        runTest(testDispatcher) {
            whenever(mockStripeRepository.retrievePaymentIntent(any(), any(), any())).thenReturn(
                Result.success(PaymentIntentFixtures.PI_REQUIRES_WECHAT_PAY_AUTHORIZE)
            )
            whenever(mockStripeRepository.refreshPaymentIntent(any(), any())).thenReturn(
                Result.success(PaymentIntentFixtures.PI_REFRESH_RESPONSE_REQUIRES_WECHAT_PAY_AUTHORIZE)
            )

            val clientSecret = "pi_3JkCxKBNJ02ErVOj0kNqBMAZ_secret_bC6oXqo976LFM06Z9rlhmzUQq"
            val requestOptions = ApiRequest.Options(apiKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)

            createProcessor().processResult(
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

            verify(
                mockStripeRepository,
                atLeast(MINIMUM_REFRESH_CALLS)
            ).refreshPaymentIntent(eq(clientSecret), eq(requestOptions))

            verify(
                mockStripeRepository,
                atMost(
                    getMaxNumberOfInvocations(
                        PaymentMethod.Type.WeChatPay
                    )
                )
            ).refreshPaymentIntent(eq(clientSecret), eq(requestOptions))
        }

    @Test
    fun `keeps retrying for polling duration and makes final attempt for wechat`() {
        runTest(testDispatcher) {
            val clientSecret = "pi_3JkCxKBNJ02ErVOj0kNqBMAZ_secret_bC6oXqo976LFM06Z9rlhmzUQq"
            val paymentIntentResult = async(Dispatchers.IO) {
                val stripeRepository = FakeStripeRepository(
                    retrievePaymentIntent = {
                        Result.success(PaymentIntentFixtures.PI_REQUIRES_WECHAT_PAY_AUTHORIZE)
                    },
                    refreshPaymentIntent = {
                        if (testDispatcher.scheduler.currentTime < MAX_POLLING_DURATION) {
                            testDispatcher.scheduler.advanceTimeBy(POLLING_DELAY)
                            Result.success(PaymentIntentFixtures.PI_REFRESH_RESPONSE_REQUIRES_WECHAT_PAY_AUTHORIZE)
                        } else {
                            Result.success(PaymentIntentFixtures.PI_REFRESH_RESPONSE_WECHAT_PAY_SUCCESS)
                        }
                    },
                )

                createProcessor(stripeRepository).processResult(
                    PaymentFlowResult.Unvalidated(
                        clientSecret = clientSecret,
                        flowOutcome = StripeIntentResult.Outcome.SUCCEEDED
                    )
                ).getOrThrow()
            }

            assertThat(paymentIntentResult.await())
                .isEqualTo(
                    PaymentIntentResult(
                        intent = PaymentIntentFixtures.PI_REFRESH_RESPONSE_WECHAT_PAY_SUCCESS,
                        outcomeFromFlow = StripeIntentResult.Outcome.SUCCEEDED,
                        failureMessage = null
                    )
                )
        }
    }

    @Test
    fun `keeps retrying for polling duration and makes final attempt for revolut pay`() {
        runTest(testDispatcher) {
            val clientSecret = "pi_3JkCxKBNJ02ErVOj0kNqBMAZ_secret_bC6oXqo976LFM06Z9rlhmzUQq"
            val requiresActionIntent = PaymentIntentFixtures.PI_SUCCEEDED.copy(
                status = StripeIntent.Status.RequiresAction,
                paymentMethod = PaymentMethodFactory.revolutPay(),
                paymentMethodTypes = listOf("card", "revolut_pay"),
            )

            val successIntent = PaymentIntentFixtures.PI_SUCCEEDED.copy(
                status = StripeIntent.Status.Succeeded,
                paymentMethod = PaymentMethodFactory.revolutPay(),
                paymentMethodTypes = listOf("card", "revolut_pay"),
            )

            val paymentIntentResult = async(Dispatchers.IO) {
                val stripeRepository = FakeStripeRepository(
                    retrievePaymentIntent = {
                        if (testDispatcher.scheduler.currentTime < REDUCED_POLLING_DURATION) {
                            testDispatcher.scheduler.advanceTimeBy(POLLING_DELAY)
                            Result.success(requiresActionIntent)
                        } else {
                            Result.success(successIntent)
                        }
                    },
                    refreshPaymentIntent = {
                        Result.success(requiresActionIntent)
                    },
                )

                createProcessor(stripeRepository).processResult(
                    PaymentFlowResult.Unvalidated(
                        clientSecret = clientSecret,
                        flowOutcome = StripeIntentResult.Outcome.SUCCEEDED
                    )
                ).getOrThrow()
            }

            assertThat(paymentIntentResult.await())
                .isEqualTo(
                    PaymentIntentResult(
                        intent = successIntent,
                        outcomeFromFlow = StripeIntentResult.Outcome.SUCCEEDED,
                        failureMessage = null
                    )
                )
        }
    }

    @Test
    fun `keeps retrying for polling duration for 3ds2`() {
        runTest(testDispatcher) {
            val clientSecret = "pi_3JkCxKBNJ02ErVOj0kNqBMAZ_secret_bC6oXqo976LFM06Z9rlhmzUQq"
            val requiresActionIntent = PaymentIntentFixtures.PI_SUCCEEDED.copy(
                status = StripeIntent.Status.RequiresAction,
                paymentMethod = PaymentMethodFactory.revolutPay(),
                paymentMethodTypes = listOf("card", "revolut_pay"),
            )

            val successIntent = PaymentIntentFixtures.PI_SUCCEEDED.copy(
                status = StripeIntent.Status.Succeeded,
                paymentMethod = PaymentMethodFactory.revolutPay(),
                paymentMethodTypes = listOf("card", "revolut_pay"),
            )

            var hasMadeInitialFetch = false
            val paymentIntentResult = async(Dispatchers.IO) {
                val stripeRepository = FakeStripeRepository(
                    retrievePaymentIntent = {
                        if (!hasMadeInitialFetch) {
                            hasMadeInitialFetch = true
                            Result.success(requiresActionIntent)
                        } else if (testDispatcher.scheduler.currentTime < REDUCED_POLLING_DURATION) {
                            testDispatcher.scheduler.advanceTimeBy(POLLING_DELAY)
                            Result.success(requiresActionIntent)
                        } else {
                            Result.success(successIntent)
                        }
                    },
                    refreshPaymentIntent = {
                        Result.success(requiresActionIntent)
                    },
                )

                createProcessor(stripeRepository).processResult(
                    PaymentFlowResult.Unvalidated(
                        clientSecret = clientSecret,
                        flowOutcome = StripeIntentResult.Outcome.SUCCEEDED
                    )
                ).getOrThrow()
            }

            assertThat(paymentIntentResult.await())
                .isEqualTo(
                    PaymentIntentResult(
                        intent = successIntent,
                        outcomeFromFlow = StripeIntentResult.Outcome.SUCCEEDED,
                        failureMessage = null
                    )
                )
        }
    }

    @Test
    fun `3ds2 canceled with processing intent should succeed`() =
        runTest(testDispatcher) {
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
        runTest(testDispatcher) {
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
        runTest(testDispatcher) {
            runCanceledFlow(
                initialIntent = PaymentIntentFixtures.PI_VISA_3DS2.copy(
                    status = StripeIntent.Status.Succeeded
                )
            )
        }

    @Test
    fun `3ds2 canceled reaches max time with processing intent should cancel`() =
        runTest(testDispatcher) {
            val intent = PaymentIntentFixtures.PI_VISA_3DS2.copy(
                status = StripeIntent.Status.RequiresAction
            )
            whenever(mockStripeRepository.retrievePaymentIntent(any(), any(), any())).thenReturn(
                Result.success(intent)
            )

            val clientSecret = requireNotNull(
                intent.clientSecret
            )

            val result = createProcessor().processResult(
                PaymentFlowResult.Unvalidated(
                    clientSecret = clientSecret,
                    flowOutcome = StripeIntentResult.Outcome.CANCELED
                )
            ).getOrThrow()

            verify(
                mockStripeRepository,
                atLeast(MINIMUM_RETRIEVE_CALLS)
            ).retrievePaymentIntent(any(), any(), any())

            verify(
                mockStripeRepository,
                atMost(
                    getMaxNumberOfInvocations(PaymentMethod.Type.Card)
                )
            ).retrievePaymentIntent(any(), any(), any())

            assertThat(result)
                .isEqualTo(
                    PaymentIntentResult(
                        intent,
                        StripeIntentResult.Outcome.CANCELED,
                        null
                    )
                )
        }

    @Test
    fun `Keeps retrying when encountering a failure while retrieving intent`() = runTest(testDispatcher) {
        val processingIntent = PaymentIntentFixtures.PI_VISA_3DS2.copy(
            status = StripeIntent.Status.RequiresAction,
        )

        val succeededIntent = processingIntent.copy(status = StripeIntent.Status.Succeeded)

        whenever(mockStripeRepository.retrievePaymentIntent(any(), any(), any())).thenReturn(
            Result.success(processingIntent),
            Result.failure(APIConnectionException()),
            Result.failure(APIConnectionException()),
            Result.failure(APIConnectionException()),
            Result.failure(APIConnectionException()),
            Result.success(succeededIntent),
        )

        val clientSecret = requireNotNull(processingIntent.clientSecret)
        val requestOptions = ApiRequest.Options(apiKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)

        val result = createProcessor().processResult(
            PaymentFlowResult.Unvalidated(
                clientSecret = clientSecret,
                flowOutcome = StripeIntentResult.Outcome.CANCELED
            )
        ).getOrThrow()

        verify(
            mockStripeRepository,
            times(6)
        ).retrievePaymentIntent(
            eq(clientSecret),
            eq(requestOptions),
            eq(PaymentFlowResultProcessor.EXPAND_PAYMENT_METHOD),
        )

        val expectedResult = PaymentIntentResult(
            intent = succeededIntent,
            outcomeFromFlow = StripeIntentResult.Outcome.SUCCEEDED,
            failureMessage = null,
        )

        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun `Stops polling after max time when encountering a Swish payment that still requires action`() =
        runTest(testDispatcher) {
            val paymentMethod = PaymentMethodFactory.swish()
            val requiresActionIntent = PaymentIntentFixtures.PI_SUCCEEDED.copy(
                status = StripeIntent.Status.RequiresAction,
                paymentMethod = paymentMethod,
                paymentMethodTypes = listOf("card", "swish"),
            )

            whenever(mockStripeRepository.retrievePaymentIntent(any(), any(), any())).thenReturn(
                Result.success(requiresActionIntent),
            )

            val clientSecret = requireNotNull(requiresActionIntent.clientSecret)

            val result = createProcessor().processResult(
                PaymentFlowResult.Unvalidated(
                    clientSecret = clientSecret,
                    flowOutcome = StripeIntentResult.Outcome.UNKNOWN,
                )
            ).getOrThrow()

            val expectedResult = PaymentIntentResult(
                intent = requiresActionIntent,
                outcomeFromFlow = StripeIntentResult.Outcome.UNKNOWN,
                failureMessage = "We are unable to authenticate your payment method." +
                    " Please choose a different payment method and try again.",
            )

            assertThat(result).isEqualTo(expectedResult)

            verify(
                mockStripeRepository,
                atLeast(MINIMUM_RETRIEVE_CALLS)
            ).retrievePaymentIntent(any(), any(), any())

            verify(
                mockStripeRepository,
                atMost(
                    getMaxNumberOfInvocations(paymentMethod.type!!)
                )
            ).retrievePaymentIntent(any(), any(), any())
        }

    @Test
    fun `Keeps retrying when encountering a Swish payment that still requires action`() =
        runTest(testDispatcher) {
            val requiresActionIntent = PaymentIntentFixtures.PI_SUCCEEDED.copy(
                status = StripeIntent.Status.RequiresAction,
                paymentMethod = PaymentMethodFactory.swish(),
                paymentMethodTypes = listOf("card", "swish"),
            )

            val succeededIntent = requiresActionIntent.copy(status = StripeIntent.Status.Succeeded)

            whenever(mockStripeRepository.retrievePaymentIntent(any(), any(), any())).thenReturn(
                Result.success(requiresActionIntent),
                Result.success(requiresActionIntent),
                Result.success(succeededIntent),
            )

            val clientSecret = requireNotNull(requiresActionIntent.clientSecret)

            val result = createProcessor().processResult(
                PaymentFlowResult.Unvalidated(
                    clientSecret = clientSecret,
                    flowOutcome = StripeIntentResult.Outcome.UNKNOWN,
                )
            ).getOrThrow()

            val expectedResult = PaymentIntentResult(
                intent = succeededIntent,
                outcomeFromFlow = StripeIntentResult.Outcome.SUCCEEDED,
                failureMessage = null,
            )

            assertThat(result).isEqualTo(expectedResult)
        }

    @Test
    fun `Calls refresh endpoint when encountering a CashAppPay payment that still requires action`() =
        runTest(testDispatcher) {
            val requiresActionIntent = PaymentIntentFixtures.PI_SUCCEEDED.copy(
                status = StripeIntent.Status.RequiresAction,
                paymentMethod = PaymentMethodFactory.cashAppPay(),
                paymentMethodTypes = listOf("card", "cashapp"),
            )

            val succeededIntent = requiresActionIntent.copy(status = StripeIntent.Status.Succeeded)

            whenever(mockStripeRepository.retrievePaymentIntent(any(), any(), any())).thenReturn(
                Result.success(requiresActionIntent),
            )

            whenever(mockStripeRepository.refreshPaymentIntent(any(), any())).thenReturn(
                Result.success(succeededIntent),
            )

            val clientSecret = requireNotNull(requiresActionIntent.clientSecret)

            val result = createProcessor().processResult(
                PaymentFlowResult.Unvalidated(
                    clientSecret = clientSecret,
                    flowOutcome = StripeIntentResult.Outcome.UNKNOWN,
                )
            ).getOrThrow()

            val expectedResult = PaymentIntentResult(
                intent = succeededIntent,
                outcomeFromFlow = StripeIntentResult.Outcome.SUCCEEDED,
                failureMessage = null,
            )

            assertThat(result).isEqualTo(expectedResult)
        }

    @Test
    fun `Calls refresh endpoint once when encountering a CashAppPay payment that remains requires action`() =
        runTest(testDispatcher) {
            val requiresActionIntent = PaymentIntentFixtures.PI_SUCCEEDED.copy(
                status = StripeIntent.Status.RequiresAction,
                paymentMethod = PaymentMethodFactory.cashAppPay(),
                paymentMethodTypes = listOf("card", "cashapp"),
            )

            whenever(mockStripeRepository.retrievePaymentIntent(any(), any(), any())).thenReturn(
                Result.success(requiresActionIntent),
            )

            whenever(mockStripeRepository.refreshPaymentIntent(any(), any())).thenReturn(
                Result.success(requiresActionIntent),
            )

            val clientSecret = requireNotNull(requiresActionIntent.clientSecret)

            val result = createProcessor().processResult(
                PaymentFlowResult.Unvalidated(
                    clientSecret = clientSecret,
                    flowOutcome = StripeIntentResult.Outcome.UNKNOWN,
                )
            ).getOrThrow()

            val expectedResult = PaymentIntentResult(
                intent = requiresActionIntent,
                outcomeFromFlow = StripeIntentResult.Outcome.UNKNOWN,
                failureMessage = "We are unable to authenticate your payment method." +
                    " Please choose a different payment method and try again.",
            )

            assertThat(result).isEqualTo(expectedResult)

            verify(mockStripeRepository).refreshPaymentIntent(any(), any())
        }

    @Test
    fun `Stops polling after max time when encountering a Amazon Pay payment that still requires action`() =
        runTest(testDispatcher) {
            val paymentMethod = PaymentMethodFactory.amazonPay()
            val requiresActionIntent = PaymentIntentFixtures.PI_SUCCEEDED.copy(
                status = StripeIntent.Status.RequiresAction,
                paymentMethod = paymentMethod,
                paymentMethodTypes = listOf("card", "amazon_pay"),
            )

            whenever(mockStripeRepository.retrievePaymentIntent(any(), any(), any())).thenReturn(
                Result.success(requiresActionIntent),
            )

            val clientSecret = requireNotNull(requiresActionIntent.clientSecret)

            val result = createProcessor().processResult(
                PaymentFlowResult.Unvalidated(
                    clientSecret = clientSecret,
                    flowOutcome = StripeIntentResult.Outcome.UNKNOWN,
                )
            ).getOrThrow()

            val expectedResult = PaymentIntentResult(
                intent = requiresActionIntent,
                outcomeFromFlow = StripeIntentResult.Outcome.UNKNOWN,
                failureMessage = "We are unable to authenticate your payment method." +
                    " Please choose a different payment method and try again.",
            )

            assertThat(result).isEqualTo(expectedResult)

            verify(
                mockStripeRepository,
                atLeast(MINIMUM_RETRIEVE_CALLS)
            ).retrievePaymentIntent(any(), any(), any())

            verify(
                mockStripeRepository,
                atMost(
                    getMaxNumberOfInvocations(paymentMethod.type!!)
                )
            ).retrievePaymentIntent(any(), any(), any())
        }

    @Test
    fun `Keeps retrying when encountering a Amazon Pay payment that still requires action`() =
        runTest(testDispatcher) {
            val requiresActionIntent = PaymentIntentFixtures.PI_SUCCEEDED.copy(
                status = StripeIntent.Status.RequiresAction,
                paymentMethod = PaymentMethodFactory.amazonPay(),
                paymentMethodTypes = listOf("card", "amazon_pay"),
            )

            val succeededIntent = requiresActionIntent.copy(status = StripeIntent.Status.Succeeded)

            whenever(mockStripeRepository.retrievePaymentIntent(any(), any(), any())).thenReturn(
                Result.success(requiresActionIntent),
                Result.success(requiresActionIntent),
                Result.success(succeededIntent),
            )

            val clientSecret = requireNotNull(requiresActionIntent.clientSecret)

            val result = createProcessor().processResult(
                PaymentFlowResult.Unvalidated(
                    clientSecret = clientSecret,
                    flowOutcome = StripeIntentResult.Outcome.UNKNOWN,
                )
            ).getOrThrow()

            val expectedResult = PaymentIntentResult(
                intent = succeededIntent,
                outcomeFromFlow = StripeIntentResult.Outcome.SUCCEEDED,
                failureMessage = null,
            )

            assertThat(result).isEqualTo(expectedResult)
        }

    @Test
    fun `Stops polling after max time when encountering a Revolut Pay payment that still requires action`() =
        runTest(testDispatcher) {
            val paymentMethod = PaymentMethodFactory.revolutPay()
            val requiresActionIntent = PaymentIntentFixtures.PI_SUCCEEDED.copy(
                status = StripeIntent.Status.RequiresAction,
                paymentMethod = paymentMethod,
                paymentMethodTypes = listOf("card", "revolut_pay"),
            )

            whenever(mockStripeRepository.retrievePaymentIntent(any(), any(), any())).thenReturn(
                Result.success(requiresActionIntent),
            )

            val clientSecret = requireNotNull(requiresActionIntent.clientSecret)

            val result = createProcessor().processResult(
                PaymentFlowResult.Unvalidated(
                    clientSecret = clientSecret,
                    flowOutcome = StripeIntentResult.Outcome.UNKNOWN,
                )
            ).getOrThrow()

            val expectedResult = PaymentIntentResult(
                intent = requiresActionIntent,
                outcomeFromFlow = StripeIntentResult.Outcome.UNKNOWN,
                failureMessage = "We are unable to authenticate your payment method." +
                    " Please choose a different payment method and try again.",
            )

            assertThat(result).isEqualTo(expectedResult)

            verify(
                mockStripeRepository,
                atLeast(3)
            ).retrievePaymentIntent(any(), any(), any())

            verify(
                mockStripeRepository,
                atMost(8)
            ).retrievePaymentIntent(any(), any(), any())
        }

    @Test
    fun `Keeps retrying when encountering a Revolut Pay payment that still requires action`() =
        runTest(testDispatcher) {
            val requiresActionIntent = PaymentIntentFixtures.PI_SUCCEEDED.copy(
                status = StripeIntent.Status.RequiresAction,
                paymentMethod = PaymentMethodFactory.revolutPay(),
                paymentMethodTypes = listOf("card", "revolut_pay"),
            )

            val succeededIntent = requiresActionIntent.copy(status = StripeIntent.Status.Succeeded)

            whenever(mockStripeRepository.retrievePaymentIntent(any(), any(), any())).thenReturn(
                Result.success(requiresActionIntent),
                Result.success(requiresActionIntent),
                Result.success(succeededIntent),
            )

            val clientSecret = requireNotNull(requiresActionIntent.clientSecret)

            val result = createProcessor().processResult(
                PaymentFlowResult.Unvalidated(
                    clientSecret = clientSecret,
                    flowOutcome = StripeIntentResult.Outcome.UNKNOWN,
                )
            ).getOrThrow()

            val expectedResult = PaymentIntentResult(
                intent = succeededIntent,
                outcomeFromFlow = StripeIntentResult.Outcome.SUCCEEDED,
                failureMessage = null,
            )

            assertThat(result).isEqualTo(expectedResult)
        }

    private suspend fun runCanceledFlow(
        initialIntent: PaymentIntent,
        refreshedIntent: PaymentIntent = initialIntent,
        expectedIntent: PaymentIntent = refreshedIntent
    ) {
        whenever(mockStripeRepository.retrievePaymentIntent(any(), any(), any())).thenReturn(
            Result.success(initialIntent),
            Result.success(refreshedIntent),
        )

        val clientSecret = requireNotNull(initialIntent.clientSecret)
        val requestOptions = ApiRequest.Options(apiKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)

        val result = createProcessor().processResult(
            PaymentFlowResult.Unvalidated(
                clientSecret = clientSecret,
                flowOutcome = StripeIntentResult.Outcome.CANCELED
            )
        ).getOrThrow()

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

    private fun createProcessor(
        stripeRepository: StripeRepository = mockStripeRepository,
    ): PaymentIntentFlowResultProcessor = PaymentIntentFlowResultProcessor(
        ApplicationProvider.getApplicationContext(),
        { ApiKeyFixtures.FAKE_PUBLISHABLE_KEY },
        stripeRepository,
        Logger.noop(),
        testDispatcher
    )

    private class FakeStripeRepository(
        private val retrievePaymentIntent: suspend () -> Result<PaymentIntent>,
        private val refreshPaymentIntent: suspend () -> Result<PaymentIntent>,
    ) : AbsFakeStripeRepository() {
        override suspend fun retrievePaymentIntent(
            clientSecret: String,
            options: ApiRequest.Options,
            expandFields: List<String>
        ): Result<PaymentIntent> {
            return retrievePaymentIntent()
        }

        override suspend fun refreshPaymentIntent(
            clientSecret: String,
            options: ApiRequest.Options
        ): Result<PaymentIntent> {
            return refreshPaymentIntent()
        }
    }

    internal companion object {
        // Accounts for the initial retrieve call in processResult, first retrieve in
        // refreshStripeIntentUntilTerminalState, and final retrieve after retry loop
        const val MINIMUM_RETRIEVE_CALLS = 3
        const val MINIMUM_REFRESH_CALLS = 2
    }
}

internal fun getMaxNumberOfInvocations(paymentMethodType: PaymentMethod.Type): Int {
    val retryPollMaxAttempts = when (paymentMethodType) {
        PaymentMethod.Type.Card -> MAX_POLLING_DURATION / POLLING_DELAY + MINIMUM_RETRIEVE_CALLS
        // WeChatPay uses the refresh endpoint
        PaymentMethod.Type.WeChatPay -> MAX_POLLING_DURATION / POLLING_DELAY + MINIMUM_REFRESH_CALLS
        PaymentMethod.Type.P24,
        PaymentMethod.Type.RevolutPay,
        PaymentMethod.Type.AmazonPay,
        PaymentMethod.Type.Swish,
        PaymentMethod.Type.Twint -> REDUCED_POLLING_DURATION / POLLING_DELAY + MINIMUM_RETRIEVE_CALLS
        else -> 3
    }
    return retryPollMaxAttempts.toInt()
}
