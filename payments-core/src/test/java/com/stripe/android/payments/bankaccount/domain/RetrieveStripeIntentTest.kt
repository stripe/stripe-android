package com.stripe.android.payments.bankaccount.domain

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.StripeRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class RetrieveStripeIntentTest {

    private val stripeRepository = mock<StripeRepository>()
    private val retrieveStripeIntent = RetrieveStripeIntent(stripeRepository)

    @Test
    fun `retrieve - given payment intent client secret, payment intent is retrieved`() {
        runTest {
            // Given
            val publishableKey = "publishable_key"
            val clientSecret = "pi_1234_secret_5678"
            givenRetrieveStripeIntentReturns {
                Result.success(mock<PaymentIntent>())
            }

            // When
            val intent = retrieveStripeIntent(
                publishableKey = publishableKey,
                clientSecret = clientSecret
            ).getOrNull()

            // Then
            verify(stripeRepository).retrieveStripeIntent(
                clientSecret = "pi_1234_secret_5678",
                options = ApiRequest.Options(publishableKey)
            )

            assertThat(intent).isInstanceOf(PaymentIntent::class.java)
        }
    }

    @Test
    fun `retrieve - given invalid payment intent client secret, exception is thrown`() {
        runTest {
            // Given
            val publishableKey = "publishable_key"
            val clientSecret = "pi_invalid"
            val expectedException = APIException()
            givenRetrieveStripeIntentReturns {
                Result.failure(expectedException)
            }

            // When
            val intent = retrieveStripeIntent(
                publishableKey = publishableKey,
                clientSecret = clientSecret
            )

            // Then
            verify(stripeRepository).retrieveStripeIntent(
                clientSecret = "pi_invalid",
                options = ApiRequest.Options(publishableKey)
            )

            assertThat(intent.exceptionOrNull()).isEqualTo(expectedException)
        }
    }

    @Test
    fun `retrieve - given setup intent client secret, setup intent is retrieved`() {
        runTest {
            // Given
            val publishableKey = "publishable_key"
            val clientSecret = "seti_1234_secret_5678"
            givenRetrieveStripeIntentReturns {
                Result.success(mock<SetupIntent>())
            }

            // When
            val intent = retrieveStripeIntent(
                publishableKey = publishableKey,
                clientSecret = clientSecret
            ).getOrNull()

            // Then
            verify(stripeRepository).retrieveStripeIntent(
                clientSecret = "seti_1234_secret_5678",
                options = ApiRequest.Options(publishableKey)
            )
            assertThat(intent).isInstanceOf(SetupIntent::class.java)
        }
    }

    @Test
    fun `retrieve - given invalid setup intent client secret, exception is thrown`() {
        runTest {
            // Given
            val publishableKey = "publishable_key"
            val clientSecret = "seti_invalid"
            val expectedException = APIException()
            givenRetrieveStripeIntentReturns {
                Result.failure(expectedException)
            }

            // When
            val intent = retrieveStripeIntent(
                publishableKey = publishableKey,
                clientSecret = clientSecret
            )

            // Then
            verify(stripeRepository).retrieveStripeIntent(
                clientSecret = "seti_invalid",
                options = ApiRequest.Options(publishableKey)
            )

            assertThat(intent.exceptionOrNull()).isEqualTo(expectedException)
        }
    }

    private suspend fun givenRetrieveStripeIntentReturns(
        result: () -> Result<StripeIntent>
    ) {
        whenever(
            stripeRepository.retrieveStripeIntent(
                any(),
                any(),
                any()
            )
        ).doReturn(result())
    }
}
