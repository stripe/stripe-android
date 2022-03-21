package com.stripe.android.payments.bankaccount.domain

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.networking.StripeRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class AttachLinkAccountSessionTest {

    private val stripeRepository = mock<StripeRepository>()
    private val attachLinkAccountSession = AttachLinkAccountSession(stripeRepository)

    @Test
    fun `forPaymentIntent - given repository succeeds, linkedSession attached and paymentIntent returned`() {
        runTest {
            // Given
            val publishableKey = "publishable_key"
            val linkedAccountSessionId = "session_id"
            val clientSecret = "pi_1234_secret_5678"
            val paymentIntent = mock<PaymentIntent> {
                on { this.clientSecret } doReturn clientSecret
            }
            givenAttachPaymentIntentReturns { paymentIntent }

            // When
            val result: Result<Unit> = attachLinkAccountSession.forPaymentIntent(
                publishableKey,
                linkedAccountSessionId,
                clientSecret
            )

            // Then
            verify(stripeRepository).attachLinkAccountSessionToPaymentIntent(
                clientSecret = clientSecret,
                paymentIntentId = "pi_1234",
                linkAccountSessionId = linkedAccountSessionId,
                requestOptions = ApiRequest.Options(publishableKey)
            )
            assertThat((result)).isEqualTo(Result.success(Unit))
        }
    }

    @Test
    fun `forPaymentIntent - given repository returns null, results in internal error failure`() {
        runTest {
            // Given
            val publishableKey = "publishable_key"
            val linkedAccountSessionId = "session_id"
            val clientSecret = "pi_1234_secret_5678"
            givenAttachPaymentIntentReturns { null }

            // When
            val result: Result<Unit> = attachLinkAccountSession.forPaymentIntent(
                publishableKey,
                linkedAccountSessionId,
                clientSecret
            )

            // Then
            verify(stripeRepository).attachLinkAccountSessionToPaymentIntent(
                clientSecret = clientSecret,
                paymentIntentId = "pi_1234",
                linkAccountSessionId = linkedAccountSessionId,
                requestOptions = ApiRequest.Options(publishableKey)
            )
            assertThat(result.exceptionOrNull()!!).isInstanceOf(InternalError::class.java)
        }
    }

    @Test
    fun `forPaymentIntent - given repository throws exception, results in internal error failure`() {
        runTest {
            // Given
            val publishableKey = "publishable_key"
            val linkedAccountSessionId = "session_id"
            val clientSecret = "pi_1234_secret_5678"
            val expectedException = APIException()
            givenAttachPaymentIntentReturns { throw expectedException }

            // When
            val result: Result<Unit> = attachLinkAccountSession.forPaymentIntent(
                publishableKey,
                linkedAccountSessionId,
                clientSecret
            )

            // Then
            verify(stripeRepository).attachLinkAccountSessionToPaymentIntent(
                clientSecret = clientSecret,
                paymentIntentId = "pi_1234",
                linkAccountSessionId = linkedAccountSessionId,
                requestOptions = ApiRequest.Options(publishableKey)
            )
            assertThat(result.exceptionOrNull()!!).isEqualTo(expectedException)
        }
    }

    @Test
    fun `forPaymentIntent - given wrong secret, results in internal error failure`() {
        runTest {
            // Given
            val publishableKey = "publishable_key"
            val linkedAccountSessionId = "session_id"
            val clientSecret = "wrong_secret"
            val paymentIntent = mock<PaymentIntent>()
            givenAttachPaymentIntentReturns { paymentIntent }

            // When
            val result: Result<Unit> = attachLinkAccountSession.forPaymentIntent(
                publishableKey,
                linkedAccountSessionId,
                clientSecret
            )

            // Then
            assertTrue(result.isFailure)
        }
    }

    @Test
    fun `forSetupIntent - given repository succeeds, linkedSession attached and setupIntent returned`() {
        runTest {
            // Given
            val publishableKey = "publishable_key"
            val linkedAccountSessionId = "session_id"
            val clientSecret = "seti_1234_secret_5678"
            val resultSetupIntent = mock<SetupIntent> {
                on { this.clientSecret } doReturn clientSecret
            }
            givenAttachSetupIntentReturns { resultSetupIntent }

            // When
            val result: Result<Unit> = attachLinkAccountSession.forSetupIntent(
                publishableKey,
                linkedAccountSessionId,
                clientSecret
            )

            // Then
            verify(stripeRepository).attachLinkAccountSessionToSetupIntent(
                clientSecret = clientSecret,
                setupIntentId = "seti_1234",
                linkAccountSessionId = linkedAccountSessionId,
                requestOptions = ApiRequest.Options(publishableKey)
            )
            assertThat((result)).isEqualTo(Result.success(Unit))
        }
    }

    @Test
    fun `forSetupIntent - given repository returns null, results in internal error failure`() {
        runTest {
            // Given
            val publishableKey = "publishable_key"
            val linkedAccountSessionId = "session_id"
            val clientSecret = "seti_1234_secret_5678"
            givenAttachSetupIntentReturns { null }

            // When
            val setupIntent: Result<Unit> = attachLinkAccountSession.forSetupIntent(
                publishableKey,
                linkedAccountSessionId,
                clientSecret
            )

            // Then
            verify(stripeRepository).attachLinkAccountSessionToSetupIntent(
                clientSecret = clientSecret,
                setupIntentId = "seti_1234",
                linkAccountSessionId = linkedAccountSessionId,
                requestOptions = ApiRequest.Options(publishableKey)
            )
            assertThat(setupIntent.exceptionOrNull()!!).isInstanceOf(InternalError::class.java)
        }
    }

    @Test
    fun `forSetupIntent - given repository throws exception, results in internal error failure`() {
        runTest {
            // Given
            val publishableKey = "publishable_key"
            val linkedAccountSessionId = "session_id"
            val clientSecret = "seti_1234_secret_5678"
            val expectedException = APIException()
            givenAttachSetupIntentReturns { throw expectedException }

            // When
            val setupIntent: Result<Unit> = attachLinkAccountSession.forSetupIntent(
                publishableKey,
                linkedAccountSessionId,
                clientSecret
            )

            // Then
            verify(stripeRepository).attachLinkAccountSessionToSetupIntent(
                clientSecret = clientSecret,
                setupIntentId = "seti_1234",
                linkAccountSessionId = linkedAccountSessionId,
                requestOptions = ApiRequest.Options(publishableKey)
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
            givenAttachSetupIntentReturns { resultSetupIntent }

            // When
            val setupIntent: Result<Unit> = attachLinkAccountSession.forSetupIntent(
                publishableKey,
                linkedAccountSessionId,
                clientSecret
            )

            // Then
            assertTrue(setupIntent.isFailure)
        }
    }

    private suspend fun givenAttachPaymentIntentReturns(paymentIntent: () -> PaymentIntent?) {
        whenever(
            stripeRepository.attachLinkAccountSessionToPaymentIntent(
                any(),
                any(),
                any(),
                any()
            )
        ).thenAnswer { paymentIntent() }
    }

    private suspend fun givenAttachSetupIntentReturns(setupIntent: () -> SetupIntent?) {
        whenever(
            stripeRepository.attachLinkAccountSessionToSetupIntent(
                any(),
                any(),
                any(),
                any()
            )
        ).thenAnswer { setupIntent() }
    }
}
