package com.stripe.android.payments.bankaccount.domain

import com.google.common.truth.Truth
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

@ExperimentalCoroutinesApi
class VerifyWithMicrodepositTest {

    private val stripeRepository = mock<StripeRepository>()
    private val verifyWithMicrodeposit = VerifyWithMicrodeposit(stripeRepository)

    @Test
    fun `forPaymentIntent - given repository succeeds, verified with amounts and paymentIntent returned`() {
        runTest {
            // Given
            val publishableKey = "publishable_key"
            val clientSecret = "pi_1234_secret_5678"
            val paymentIntent = mock<PaymentIntent> {
                on { this.clientSecret } doReturn clientSecret
            }
            givenVerifyPaymentIntentReturns { paymentIntent }

            // When
            val result: Result<PaymentIntent> = verifyWithMicrodeposit.forPaymentIntent(
                publishableKey,
                clientSecret,
                12,
                34
            )

            // Then
            verify(stripeRepository).verifyPaymentIntentWithMicrodeposits(
                clientSecret = clientSecret,
                firstAmount = 12,
                secondAmount = 34,
                requestOptions = ApiRequest.Options(publishableKey)
            )
            Truth.assertThat((result)).isEqualTo(Result.success(paymentIntent))
        }
    }

    @Test
    fun `forPaymentIntent - given repository succeeds, verified with decriptorCode and paymentIntent returned`() {
        runTest {
            // Given
            val publishableKey = "publishable_key"
            val clientSecret = "pi_1234_secret_5678"
            val paymentIntent = mock<PaymentIntent> {
                on { this.clientSecret } doReturn clientSecret
            }
            givenVerifyPaymentIntentReturns { paymentIntent }

            // When
            val result: Result<PaymentIntent> = verifyWithMicrodeposit.forPaymentIntent(
                publishableKey,
                clientSecret,
                "code"
            )

            // Then
            verify(stripeRepository).verifyPaymentIntentWithMicrodeposits(
                clientSecret = clientSecret,
                descriptorCode = "code",
                requestOptions = ApiRequest.Options(publishableKey)
            )
            Truth.assertThat((result)).isEqualTo(Result.success(paymentIntent))
        }
    }

    @Test
    fun `forPaymentIntent - given repository returns null, results in internal error failure`() {
        runTest {
            // Given
            val publishableKey = "publishable_key"
            val clientSecret = "pi_1234_secret_5678"
            givenVerifyPaymentIntentReturns { null }

            // When
            val result: Result<PaymentIntent> = verifyWithMicrodeposit.forPaymentIntent(
                publishableKey,
                clientSecret,
                12,
                34
            )

            // Then
            verify(stripeRepository).verifyPaymentIntentWithMicrodeposits(
                clientSecret = clientSecret,
                firstAmount = 12,
                secondAmount = 34,
                requestOptions = ApiRequest.Options(publishableKey)
            )
            Truth.assertThat(result.exceptionOrNull()!!).isInstanceOf(InternalError::class.java)
        }
    }

    @Test
    fun `forPaymentIntent - given repository throws exception, results in internal error failure`() {
        runTest {
            // Given
            val publishableKey = "publishable_key"
            val clientSecret = "pi_1234_secret_5678"
            val expectedException = APIException()
            givenVerifyPaymentIntentReturns { throw expectedException }

            // When
            val result: Result<PaymentIntent> = verifyWithMicrodeposit.forPaymentIntent(
                publishableKey,
                clientSecret,
                12,
                34
            )

            // Then
            verify(stripeRepository).verifyPaymentIntentWithMicrodeposits(
                clientSecret = clientSecret,
                firstAmount = 12,
                secondAmount = 34,
                requestOptions = ApiRequest.Options(publishableKey)
            )
            Truth.assertThat(result.exceptionOrNull()!!).isEqualTo(expectedException)
        }
    }

    @Test
    fun `forSetupIntent - given repository succeeds, verified with amounts and paymentIntent returned`() {
        runTest {
            // Given
            val publishableKey = "publishable_key"
            val clientSecret = "pi_1234_secret_5678"
            val setupIntent = mock<SetupIntent> {
                on { this.clientSecret } doReturn clientSecret
            }
            givenVerifySetupIntentReturns { setupIntent }

            // When
            val result: Result<SetupIntent> = verifyWithMicrodeposit.forSetupIntent(
                publishableKey,
                clientSecret,
                12,
                34
            )

            // Then
            verify(stripeRepository).verifySetupIntentWithMicrodeposits(
                clientSecret = clientSecret,
                firstAmount = 12,
                secondAmount = 34,
                requestOptions = ApiRequest.Options(publishableKey)
            )
            Truth.assertThat((result)).isEqualTo(Result.success(setupIntent))
        }
    }

    @Test
    fun `forSetupIntent - given repository succeeds, verified with decriptorCode and paymentIntent returned`() {
        runTest {
            // Given
            val publishableKey = "publishable_key"
            val clientSecret = "pi_1234_secret_5678"
            val setupIntent = mock<SetupIntent> {
                on { this.clientSecret } doReturn clientSecret
            }
            givenVerifySetupIntentReturns { setupIntent }

            // When
            val result: Result<SetupIntent> = verifyWithMicrodeposit.forSetupIntent(
                publishableKey,
                clientSecret,
                "code"
            )

            // Then
            verify(stripeRepository).verifySetupIntentWithMicrodeposits(
                clientSecret = clientSecret,
                descriptorCode = "code",
                requestOptions = ApiRequest.Options(publishableKey)
            )
            Truth.assertThat((result)).isEqualTo(Result.success(setupIntent))
        }
    }

    @Test
    fun `forSetupIntent - given repository returns null, results in internal error failure`() {
        runTest {
            // Given
            val publishableKey = "publishable_key"
            val clientSecret = "pi_1234_secret_5678"
            givenVerifySetupIntentReturns { null }

            // When
            val result: Result<SetupIntent> = verifyWithMicrodeposit.forSetupIntent(
                publishableKey,
                clientSecret,
                12,
                34
            )

            // Then
            verify(stripeRepository).verifySetupIntentWithMicrodeposits(
                clientSecret = clientSecret,
                firstAmount = 12,
                secondAmount = 34,
                requestOptions = ApiRequest.Options(publishableKey)
            )
            Truth.assertThat(result.exceptionOrNull()!!).isInstanceOf(InternalError::class.java)
        }
    }

    @Test
    fun `forSetupIntent - given repository throws exception, results in internal error failure`() {
        runTest {
            // Given
            val publishableKey = "publishable_key"
            val clientSecret = "pi_1234_secret_5678"
            val expectedException = APIException()
            givenVerifySetupIntentReturns { throw expectedException }

            // When
            val result: Result<SetupIntent> = verifyWithMicrodeposit.forSetupIntent(
                publishableKey,
                clientSecret,
                12,
                34
            )

            // Then
            verify(stripeRepository).verifySetupIntentWithMicrodeposits(
                clientSecret = clientSecret,
                firstAmount = 12,
                secondAmount = 34,
                requestOptions = ApiRequest.Options(publishableKey)
            )
            Truth.assertThat(result.exceptionOrNull()!!).isEqualTo(expectedException)
        }
    }

    private suspend fun givenVerifyPaymentIntentReturns(paymentIntent: () -> PaymentIntent?) {
        whenever(
            stripeRepository.verifyPaymentIntentWithMicrodeposits(
                any(),
                any(),
                any(),
                any()
            )
        ).thenAnswer { paymentIntent() }
        whenever(
            stripeRepository.verifyPaymentIntentWithMicrodeposits(
                any(),
                any(),
                any()
            )
        ).thenAnswer { paymentIntent() }
    }

    private suspend fun givenVerifySetupIntentReturns(setupIntent: () -> SetupIntent?) {
        whenever(
            stripeRepository.verifySetupIntentWithMicrodeposits(
                any(),
                any(),
                any(),
                any()
            )
        ).thenAnswer { setupIntent() }
        whenever(
            stripeRepository.verifySetupIntentWithMicrodeposits(
                any(),
                any(),
                any()
            )
        ).thenAnswer { setupIntent() }
    }
}
