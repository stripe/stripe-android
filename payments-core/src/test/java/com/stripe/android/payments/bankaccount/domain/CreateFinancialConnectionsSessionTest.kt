package com.stripe.android.payments.bankaccount.domain

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.CreateFinancialConnectionsSessionForDeferredPaymentParams
import com.stripe.android.model.CreateFinancialConnectionsSessionParams
import com.stripe.android.model.FinancialConnectionsSession
import com.stripe.android.model.VerificationMethodParam
import com.stripe.android.networking.StripeRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertTrue

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
            givenCreateSessionWithPaymentIntentReturns { Result.success(linkedAccountSession) }

            // When
            val paymentIntent: Result<FinancialConnectionsSession> =
                createFinancialConnectionsSession.forPaymentIntent(
                    publishableKey = publishableKey,
                    clientSecret = clientSecret,
                    customerName = customerName,
                    customerEmail = null,
                    stripeAccountId = null
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
    fun `forPaymentIntent - given repository throws exception, results in internal error failure`() {
        runTest {
            // Given
            val publishableKey = "publishable_key"
            val clientSecret = "pi_1234_secret_5678"

            val expectedException = APIException()
            givenCreateSessionWithPaymentIntentReturns { Result.failure(expectedException) }

            // When
            val paymentIntent: Result<FinancialConnectionsSession> =
                createFinancialConnectionsSession.forPaymentIntent(
                    publishableKey = publishableKey,
                    clientSecret = clientSecret,
                    customerName = customerName,
                    customerEmail = null,
                    stripeAccountId = null
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
            givenCreateSessionWithPaymentIntentReturns { Result.success(linkedAccountSession) }

            // When
            val paymentIntent: Result<FinancialConnectionsSession> =
                createFinancialConnectionsSession.forPaymentIntent(
                    publishableKey = publishableKey,
                    clientSecret = clientSecret,
                    customerName = customerName,
                    customerEmail = null,
                    stripeAccountId = null
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
            val stripeAccountId = "accountId"
            givenCreateSessionWithSetupIntentReturns { Result.success(linkedAccountSession) }

            // When
            val paymentIntent: Result<FinancialConnectionsSession> =
                createFinancialConnectionsSession.forSetupIntent(
                    publishableKey = publishableKey,
                    clientSecret = clientSecret,
                    customerName = customerName,
                    customerEmail = null,
                    stripeAccountId = stripeAccountId
                )

            // Then
            verify(stripeRepository).createSetupIntentFinancialConnectionsSession(
                setupIntentId = "seti_1234",
                params = CreateFinancialConnectionsSessionParams(
                    clientSecret,
                    customerName,
                    null
                ),
                requestOptions = ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId
                )
            )
            assertThat((paymentIntent)).isEqualTo(Result.success(linkedAccountSession))
        }
    }

    @Test
    fun `forSetupIntent - given repository throws exception, results in internal error failure`() {
        runTest {
            // Given
            val publishableKey = "publishable_key"
            val clientSecret = "seti_1234_secret_5678"
            val expectedException = APIException()
            givenCreateSessionWithSetupIntentReturns { Result.failure(expectedException) }

            // When
            val paymentIntent: Result<FinancialConnectionsSession> =
                createFinancialConnectionsSession.forSetupIntent(
                    publishableKey = publishableKey,
                    clientSecret = clientSecret,
                    customerName = customerName,
                    customerEmail = null,
                    stripeAccountId = null
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
            givenCreateSessionWithSetupIntentReturns { Result.success(linkedAccountSession) }

            // When
            val paymentIntent: Result<FinancialConnectionsSession> =
                createFinancialConnectionsSession.forSetupIntent(
                    publishableKey = publishableKey,
                    clientSecret = clientSecret,
                    customerName = customerName,
                    customerEmail = null,
                    stripeAccountId = null
                )

            // Then
            assertTrue(paymentIntent.isFailure)
        }
    }

    @Test
    fun `forDeferredIntent - given repository succeeds, linkedSession created for deferred intent`() {
        runTest {
            // Given
            val publishableKey = "publishable_key"
            val stripeAccountId = "accountId"
            val elementsSessionId = "unique_id"
            val customerId = "customer_id"
            val onBehalfOf = "on_behalf_of_id"
            val amount = 1000
            val currency = "usd"
            givenCreateSessionWithDeferredIntentReturns { Result.success(linkedAccountSession) }

            // When
            val deferredIntent: Result<FinancialConnectionsSession> =
                createFinancialConnectionsSession.forDeferredPayments(
                    publishableKey = publishableKey,
                    stripeAccountId = stripeAccountId,
                    elementsSessionId = elementsSessionId,
                    customerId = customerId,
                    onBehalfOf = onBehalfOf,
                    amount = amount,
                    currency = currency
                )

            // Then
            verify(stripeRepository).createFinancialConnectionsSessionForDeferredPayments(
                params = eq(
                    CreateFinancialConnectionsSessionForDeferredPaymentParams(
                        uniqueId = elementsSessionId,
                        initialInstitution = null,
                        manualEntryOnly = null,
                        searchSession = null,
                        verificationMethod = VerificationMethodParam.Automatic,
                        customer = customerId,
                        onBehalfOf = onBehalfOf,
                        amount = amount,
                        currency = currency
                    )
                ),
                requestOptions = eq(
                    ApiRequest.Options(
                        apiKey = publishableKey,
                        stripeAccount = stripeAccountId
                    )
                )
            )
            assertThat((deferredIntent)).isEqualTo(Result.success(linkedAccountSession))
        }
    }

    @Test
    fun `forDeferredIntent - given repository throws exception, results in internal error failure`() {
        runTest {
            // Given
            val publishableKey = "publishable_key"
            val elementsSessionId = "unique_id"
            val customerId = "customer_id"
            val onBehalfOf = "on_behalf_of_id"
            val amount = 1000
            val currency = "usd"

            val expectedException = APIException()
            givenCreateSessionWithDeferredIntentReturns { Result.failure(expectedException) }

            // When
            val paymentIntent: Result<FinancialConnectionsSession> =
                createFinancialConnectionsSession.forDeferredPayments(
                    publishableKey = publishableKey,
                    stripeAccountId = null,
                    elementsSessionId = elementsSessionId,
                    customerId = customerId,
                    onBehalfOf = onBehalfOf,
                    amount = amount,
                    currency = currency
                )

            // Then
            verify(stripeRepository).createFinancialConnectionsSessionForDeferredPayments(
                params = eq(
                    CreateFinancialConnectionsSessionForDeferredPaymentParams(
                        uniqueId = elementsSessionId,
                        initialInstitution = null,
                        manualEntryOnly = null,
                        searchSession = null,
                        verificationMethod = VerificationMethodParam.Automatic,
                        customer = customerId,
                        onBehalfOf = onBehalfOf,
                        amount = amount,
                        currency = currency
                    )
                ),
                requestOptions = eq(
                    ApiRequest.Options(publishableKey)
                )
            )
            assertThat(paymentIntent.exceptionOrNull()!!).isEqualTo(expectedException)
        }
    }

    private suspend fun givenCreateSessionWithPaymentIntentReturns(
        session: () -> Result<FinancialConnectionsSession>,
    ) {
        whenever(
            stripeRepository.createPaymentIntentFinancialConnectionsSession(
                any(),
                any(),
                any()
            )
        ).doReturn(session())
    }

    private suspend fun givenCreateSessionWithSetupIntentReturns(
        session: () -> Result<FinancialConnectionsSession>,
    ) {
        whenever(
            stripeRepository.createSetupIntentFinancialConnectionsSession(
                any(),
                any(),
                any()
            )
        ).doReturn(session())
    }

    private suspend fun givenCreateSessionWithDeferredIntentReturns(
        session: () -> Result<FinancialConnectionsSession>,
    ) {
        whenever(
            stripeRepository.createFinancialConnectionsSessionForDeferredPayments(
                any(),
                any(),
            )
        ).doReturn(session())
    }
}
