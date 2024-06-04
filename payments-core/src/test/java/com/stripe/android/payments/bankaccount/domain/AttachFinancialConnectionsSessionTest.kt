package com.stripe.android.payments.bankaccount.domain

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.networking.StripeRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertTrue

class AttachFinancialConnectionsSessionTest {

    private val stripeRepository = mock<StripeRepository>()
    private val attachFinancialConnectionsSession = AttachFinancialConnectionsSession(stripeRepository)

    private val publishableKey = "publishable_key"
    private val linkedAccountSessionId = "session_id"
    private val stripeAccountId = "stripe_account_id"

    @Test
    fun `forPaymentIntent - given repository succeeds, linkedSession attached and paymentIntent returned`() {
        runTest {
            // Given
            val clientSecret = "pi_1234_secret_5678"
            val paymentIntent = mock<PaymentIntent> {
                on { this.clientSecret } doReturn clientSecret
            }
            givenAttachPaymentIntentReturns { Result.success(paymentIntent) }

            // When
            val result: Result<PaymentIntent> = attachFinancialConnectionsSession.forPaymentIntent(
                publishableKey = publishableKey,
                linkedAccountSessionId = linkedAccountSessionId,
                clientSecret = clientSecret,
                stripeAccountId = stripeAccountId
            )

            // Then
            verify(stripeRepository).attachFinancialConnectionsSessionToPaymentIntent(
                clientSecret = clientSecret,
                paymentIntentId = "pi_1234",
                financialConnectionsSessionId = linkedAccountSessionId,
                requestOptions = ApiRequest.Options(publishableKey, stripeAccountId),
                expandFields = listOf("payment_method")
            )
            assertThat((result)).isEqualTo(Result.success(paymentIntent))
        }
    }

    @Test
    fun `forPaymentIntent - given repository throws exception, results in internal error failure`() {
        runTest {
            // Given
            val clientSecret = "pi_1234_secret_5678"
            val expectedException = APIException()
            givenAttachPaymentIntentReturns { Result.failure(expectedException) }

            // When
            val result: Result<PaymentIntent> = attachFinancialConnectionsSession.forPaymentIntent(
                publishableKey = publishableKey,
                linkedAccountSessionId = linkedAccountSessionId,
                clientSecret = clientSecret,
                stripeAccountId = stripeAccountId
            )

            // Then
            verify(stripeRepository).attachFinancialConnectionsSessionToPaymentIntent(
                clientSecret = clientSecret,
                paymentIntentId = "pi_1234",
                financialConnectionsSessionId = linkedAccountSessionId,
                requestOptions = ApiRequest.Options(publishableKey, stripeAccountId),
                expandFields = listOf("payment_method")
            )
            assertThat(result.exceptionOrNull()!!).isEqualTo(expectedException)
        }
    }

    @Test
    fun `forPaymentIntent - given wrong secret, results in internal error failure`() {
        runTest {
            // Given
            val clientSecret = "wrong_secret"
            val paymentIntent = mock<PaymentIntent>()
            givenAttachPaymentIntentReturns { Result.success(paymentIntent) }

            // When
            val result: Result<PaymentIntent> = attachFinancialConnectionsSession.forPaymentIntent(
                publishableKey,
                linkedAccountSessionId,
                clientSecret,
                stripeAccountId
            )

            // Then
            assertTrue(result.isFailure)
        }
    }

    @Test
    fun `forSetupIntent - given repository succeeds, linkedSession attached and setupIntent returned`() {
        runTest {
            // Given
            val clientSecret = "seti_1234_secret_5678"
            val setupIntent = mock<SetupIntent> {
                on { this.clientSecret } doReturn clientSecret
            }
            givenAttachSetupIntentReturns { Result.success(setupIntent) }

            // When
            val result: Result<SetupIntent> = attachFinancialConnectionsSession.forSetupIntent(
                publishableKey,
                linkedAccountSessionId,
                clientSecret,
                stripeAccountId
            )

            // Then
            verify(stripeRepository).attachFinancialConnectionsSessionToSetupIntent(
                clientSecret = clientSecret,
                setupIntentId = "seti_1234",
                financialConnectionsSessionId = linkedAccountSessionId,
                requestOptions = ApiRequest.Options(publishableKey, stripeAccountId),
                expandFields = listOf("payment_method")
            )
            assertThat((result)).isEqualTo(Result.success(setupIntent))
        }
    }

    @Test
    fun `forSetupIntent - given repository throws exception, results in internal error failure`() {
        runTest {
            // Given
            val clientSecret = "seti_1234_secret_5678"
            val expectedException = APIException()
            givenAttachSetupIntentReturns { Result.failure(expectedException) }

            // When
            val setupIntent: Result<SetupIntent> = attachFinancialConnectionsSession.forSetupIntent(
                publishableKey,
                linkedAccountSessionId,
                clientSecret,
                stripeAccountId
            )

            // Then
            verify(stripeRepository).attachFinancialConnectionsSessionToSetupIntent(
                clientSecret = clientSecret,
                setupIntentId = "seti_1234",
                financialConnectionsSessionId = linkedAccountSessionId,
                requestOptions = ApiRequest.Options(publishableKey, stripeAccountId),
                expandFields = listOf("payment_method")
            )
            assertThat(setupIntent.exceptionOrNull()!!).isEqualTo(expectedException)
        }
    }

    @Test
    fun `forSetupIntent - given wrong secret, results in internal error failure`() {
        runTest {
            // Given
            val publishableKey = "publishable_key"
            val linkedAccountSessionId = "session_id"
            val clientSecret = "wrong_secret"
            val resultSetupIntent = mock<SetupIntent>()
            givenAttachSetupIntentReturns { Result.success(resultSetupIntent) }

            // When
            val setupIntent: Result<SetupIntent> = attachFinancialConnectionsSession.forSetupIntent(
                publishableKey,
                linkedAccountSessionId,
                clientSecret,
                stripeAccountId
            )

            // Then
            assertTrue(setupIntent.isFailure)
        }
    }

    private suspend fun givenAttachPaymentIntentReturns(
        paymentIntent: () -> Result<PaymentIntent>,
    ) {
        whenever(
            stripeRepository.attachFinancialConnectionsSessionToPaymentIntent(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        ).doReturn(paymentIntent())
    }

    private suspend fun givenAttachSetupIntentReturns(
        setupIntent: () -> Result<SetupIntent>,
    ) {
        whenever(
            stripeRepository.attachFinancialConnectionsSessionToSetupIntent(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        ).doReturn(setupIntent())
    }
}
