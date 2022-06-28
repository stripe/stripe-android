package com.stripe.android.payments.bankaccount.domain

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.CreateFinancialConnectionsSessionParams
import com.stripe.android.model.FinancialConnectionsSession
import com.stripe.android.networking.StripeRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class CreateFinancialConnectionsSessionTest {

    private val stripeRepository = mock<StripeRepository>()
    private val createFinancialConnectionsSession = CreateFinancialConnectionsSession(stripeRepository)

    private val customerName = "name"
    private val linkedAccountSession = FinancialConnectionsSession(
        "session_secret",
        "session_id"
    )

    @Test
    fun `forPaymentIntent - given repository succeeds, linkedSession created for payment intent`() {
        runTest {
            // Given
            val publishableKey = "publishable_key"
            val clientSecret = "pi_1234_secret_5678"
            givenCreateSessionWithPaymentIntentReturns { linkedAccountSession }

            // When
            val paymentIntent: Result<FinancialConnectionsSession> =
                createFinancialConnectionsSession.forPaymentIntent(
                    publishableKey = publishableKey,
                    clientSecret = clientSecret,
                    customerName = customerName,
                    customerEmail = null
                )

            // Then
            verify(stripeRepository).createPaymentIntentFinancialConnectionsSession(
                paymentIntentId = "pi_1234",
                params = CreateFinancialConnectionsSessionParams(
                    clientSecret,
                    customerName,
                    null
                ),
                requestOptions = ApiRequest.Options(publishableKey)
            )
            assertThat((paymentIntent)).isEqualTo(Result.success(linkedAccountSession))
        }
    }

    @Test
    fun `forPaymentIntent - given repository returns null, results in internal error failure`() {
        runTest {
            // Given
            val publishableKey = "publishable_key"
            val clientSecret = "pi_1234_secret_5678"
            givenCreateSessionWithPaymentIntentReturns { null }

            // When
            val paymentIntent: Result<FinancialConnectionsSession> =
                createFinancialConnectionsSession.forPaymentIntent(
                    publishableKey = publishableKey,
                    clientSecret = clientSecret,
                    customerName = customerName,
                    customerEmail = null
                )

            // Then
            verify(stripeRepository).createPaymentIntentFinancialConnectionsSession(
                paymentIntentId = "pi_1234",
                params = CreateFinancialConnectionsSessionParams(
                    clientSecret,
                    customerName,
                    null
                ),
                requestOptions = ApiRequest.Options(publishableKey)
            )
            assertThat(paymentIntent.exceptionOrNull()!!)
                .isInstanceOf(InternalError::class.java)
        }
    }

    @Test
    fun `forPaymentIntent - given repository throws exception, results in internal error failure`() {
        runTest {
            // Given
            val publishableKey = "publishable_key"
            val clientSecret = "pi_1234_secret_5678"

            val expectedException = APIException()
            givenCreateSessionWithPaymentIntentReturns { throw expectedException }

            // When
            val paymentIntent: Result<FinancialConnectionsSession> =
                createFinancialConnectionsSession.forPaymentIntent(
                    publishableKey = publishableKey,
                    clientSecret = clientSecret,
                    customerName = customerName,
                    customerEmail = null
                )

            // Then
            verify(stripeRepository).createPaymentIntentFinancialConnectionsSession(
                paymentIntentId = "pi_1234",
                params = CreateFinancialConnectionsSessionParams(
                    clientSecret = clientSecret,
                    customerName = customerName,
                    customerEmailAddress = null
                ),
                requestOptions = ApiRequest.Options(publishableKey)
            )
            assertThat(paymentIntent.exceptionOrNull()!!).isEqualTo(expectedException)
        }
    }

    @Test
    fun `forPaymentIntent - given wrong secret, results in internal error failure`() {
        runTest {
            // Given
            val publishableKey = "publishable_key"
            val clientSecret = "wrong_secret"
            givenCreateSessionWithPaymentIntentReturns { linkedAccountSession }

            // When
            val paymentIntent: Result<FinancialConnectionsSession> =
                createFinancialConnectionsSession.forPaymentIntent(
                    publishableKey = publishableKey,
                    clientSecret = clientSecret,
                    customerName = customerName,
                    customerEmail = null
                )

            // Then
            assertTrue(paymentIntent.isFailure)
        }
    }

    @Test
    fun `forSetupIntent - given repository succeeds, linkedSession created for setup intent`() {
        runTest {
            // Given
            val publishableKey = "publishable_key"
            val clientSecret = "seti_1234_secret_5678"
            givenCreateSessionWithSetupIntentReturns { linkedAccountSession }

            // When
            val paymentIntent: Result<FinancialConnectionsSession> =
                createFinancialConnectionsSession.forSetupIntent(
                    publishableKey = publishableKey,
                    clientSecret = clientSecret,
                    customerName = customerName,
                    customerEmail = null
                )

            // Then
            verify(stripeRepository).createSetupIntentFinancialConnectionsSession(
                setupIntentId = "seti_1234",
                params = CreateFinancialConnectionsSessionParams(
                    clientSecret,
                    customerName,
                    null
                ),
                requestOptions = ApiRequest.Options(publishableKey)
            )
            assertThat((paymentIntent)).isEqualTo(Result.success(linkedAccountSession))
        }
    }

    @Test
    fun `forSetupIntent - given repository returns null, results in internal error failure`() {
        runTest {
            // Given
            val publishableKey = "publishable_key"
            val clientSecret = "seti_1234_secret_5678"
            givenCreateSessionWithSetupIntentReturns { null }

            // When
            val paymentIntent: Result<FinancialConnectionsSession> =
                createFinancialConnectionsSession.forSetupIntent(
                    publishableKey = publishableKey,
                    clientSecret = clientSecret,
                    customerName = customerName,
                    customerEmail = null
                )

            // Then
            verify(stripeRepository).createSetupIntentFinancialConnectionsSession(
                setupIntentId = "seti_1234",
                params = CreateFinancialConnectionsSessionParams(
                    clientSecret,
                    customerName,
                    null
                ),
                requestOptions = ApiRequest.Options(publishableKey)
            )
            assertThat(paymentIntent.exceptionOrNull()!!)
                .isInstanceOf(InternalError::class.java)
        }
    }

    @Test
    fun `forSetupIntent - given repository throws exception, results in internal error failure`() {
        runTest {
            // Given
            val publishableKey = "publishable_key"
            val clientSecret = "seti_1234_secret_5678"
            val expectedException = APIException()
            givenCreateSessionWithSetupIntentReturns { throw expectedException }

            // When
            val paymentIntent: Result<FinancialConnectionsSession> =
                createFinancialConnectionsSession.forSetupIntent(
                    publishableKey = publishableKey,
                    clientSecret = clientSecret,
                    customerName = customerName,
                    customerEmail = null
                )

            // Then
            verify(stripeRepository).createSetupIntentFinancialConnectionsSession(
                setupIntentId = "seti_1234",
                params = CreateFinancialConnectionsSessionParams(
                    clientSecret,
                    customerName,
                    null
                ),
                requestOptions = ApiRequest.Options(publishableKey)
            )
            assertThat(paymentIntent.exceptionOrNull()!!).isEqualTo(expectedException)
        }
    }

    @Test
    fun `forSetupIntent - given wrong secret, results in internal error failure`() {
        runTest {
            // Given
            val publishableKey = "publishable_key"
            val clientSecret = "wrong_secret"
            givenCreateSessionWithSetupIntentReturns { linkedAccountSession }

            // When
            val paymentIntent: Result<FinancialConnectionsSession> =
                createFinancialConnectionsSession.forSetupIntent(
                    publishableKey = publishableKey,
                    clientSecret = clientSecret,
                    customerName = customerName,
                    customerEmail = null
                )

            // Then
            assertTrue(paymentIntent.isFailure)
        }
    }

    private suspend fun givenCreateSessionWithPaymentIntentReturns(
        session: () -> FinancialConnectionsSession?
    ) {
        whenever(
            stripeRepository.createPaymentIntentFinancialConnectionsSession(
                any(),
                any(),
                any()
            )
        ).thenAnswer { session() }
    }

    private suspend fun givenCreateSessionWithSetupIntentReturns(
        session: () -> FinancialConnectionsSession?
    ) {
        whenever(
            stripeRepository.createSetupIntentFinancialConnectionsSession(
                any(),
                any(),
                any()
            )
        ).thenAnswer { session() }
    }
}
