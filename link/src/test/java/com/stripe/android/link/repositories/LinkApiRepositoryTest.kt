package com.stripe.android.link.repositories

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.link.model.PaymentDetailsFixtures
import com.stripe.android.link.ui.paymentmethod.SupportedPaymentMethod
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetailsCreateParams
import com.stripe.android.model.ConsumerPaymentDetailsUpdateParams
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.ConsumerSignUpConsentAction
import com.stripe.android.model.FinancialConnectionsSession
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.networking.StripeRepository
import com.stripe.android.repository.ConsumersApiService
import com.stripe.android.ui.core.FieldValuesToParamsMapConverter
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@Suppress("LargeClass")
@RunWith(RobolectricTestRunner::class)
class LinkApiRepositoryTest {
    private val stripeRepository = mock<StripeRepository>()
    private val consumersApiService = mock<ConsumersApiService>()

    private val paymentIntent = mock<PaymentIntent>().apply {
        whenever(clientSecret).thenReturn("secret")
    }

    private val linkRepository = LinkApiRepository(
        publishableKeyProvider = { PUBLISHABLE_KEY },
        stripeAccountIdProvider = { STRIPE_ACCOUNT_ID },
        stripeRepository = stripeRepository,
        consumersApiService = consumersApiService,
        workContext = Dispatchers.IO,
        locale = Locale.US
    )

    @Test
    fun `lookupConsumer sends correct parameters`() = runTest {
        val email = "email@example.com"
        val cookie = "cookie1"
        linkRepository.lookupConsumer(email, cookie)

        verify(consumersApiService).lookupConsumerSession(
            eq(email),
            eq(cookie),
            eq(CONSUMER_SURFACE),
            eq(ApiRequest.Options(PUBLISHABLE_KEY, STRIPE_ACCOUNT_ID))
        )
    }

    @Test
    fun `lookupConsumer returns successful result`() = runTest {
        val consumerSessionLookup = mock<ConsumerSessionLookup>()
        whenever(
            consumersApiService.lookupConsumerSession(
                any(),
                any(),
                any(),
                any()
            )
        )
            .thenReturn(consumerSessionLookup)

        val result = linkRepository.lookupConsumer("email", "cookie")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(consumerSessionLookup)
    }

    @Test
    fun `lookupConsumer catches exception and returns failure`() = runTest {
        whenever(
            consumersApiService.lookupConsumerSession(
                any(),
                any(),
                any(),
                any()
            )
        )
            .thenThrow(RuntimeException("error"))

        val result = linkRepository.lookupConsumer("email", "cookie")

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `consumerSignUp sends correct parameters`() = runTest {
        val email = "email@example.com"
        val phone = "phone"
        val country = "US"
        val name = "name"
        val cookie = "cookie2"
        linkRepository.consumerSignUp(
            email,
            phone,
            country,
            name,
            cookie,
            ConsumerSignUpConsentAction.Checkbox
        )

        verify(stripeRepository).consumerSignUp(
            eq(email),
            eq(phone),
            eq(country),
            eq(name),
            eq(Locale.US),
            eq(cookie),
            eq(ConsumerSignUpConsentAction.Checkbox),
            eq(ApiRequest.Options(PUBLISHABLE_KEY, STRIPE_ACCOUNT_ID))
        )
    }

    @Test
    fun `consumerSignUp returns successful result`() = runTest {
        val consumerSession = mock<ConsumerSession>()
        whenever(
            stripeRepository.consumerSignUp(
                email = any(),
                phoneNumber = any(),
                country = any(),
                name = anyOrNull(),
                locale = anyOrNull(),
                authSessionCookie = anyOrNull(),
                consentAction = any(),
                requestOptions = any()
            )
        ).thenReturn(consumerSession)

        val result = linkRepository.consumerSignUp(
            "email",
            "phone",
            "country",
            "name",
            "cookie",
            ConsumerSignUpConsentAction.Checkbox
        )

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(consumerSession)
    }

    @Test
    fun `consumerSignUp catches exception and returns failure`() = runTest {
        whenever(
            stripeRepository.consumerSignUp(
                email = any(),
                phoneNumber = any(),
                country = any(),
                name = anyOrNull(),
                locale = anyOrNull(),
                authSessionCookie = anyOrNull(),
                consentAction = any(),
                requestOptions = any()
            )
        ).thenThrow(RuntimeException("error"))

        val result = linkRepository.consumerSignUp(
            "email",
            "phone",
            "country",
            "name",
            "cookie",
            ConsumerSignUpConsentAction.Button
        )

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `startVerification sends correct parameters`() = runTest {
        val secret = "secret"
        val cookie = "cookie1"
        val consumerKey = "key"
        linkRepository.startVerification(secret, consumerKey, cookie)

        verify(consumersApiService).startConsumerVerification(
            eq(secret),
            eq(Locale.US),
            eq(cookie),
            eq(CONSUMER_SURFACE),
            eq(ApiRequest.Options(consumerKey))
        )
    }

    @Test
    fun `startVerification without consumerPublishableKey sends correct parameters`() = runTest {
        val secret = "secret"
        val cookie = "cookie1"
        linkRepository.startVerification(secret, null, cookie)

        verify(consumersApiService).startConsumerVerification(
            eq(secret),
            eq(Locale.US),
            eq(cookie),
            eq(CONSUMER_SURFACE),
            eq(ApiRequest.Options(PUBLISHABLE_KEY, STRIPE_ACCOUNT_ID))
        )
    }

    @Test
    fun `startVerification returns successful result`() = runTest {
        val consumerSession = mock<ConsumerSession>()
        whenever(
            consumersApiService.startConsumerVerification(
                any(),
                any(),
                anyOrNull(),
                any(),
                any()
            )
        )
            .thenReturn(consumerSession)

        val result = linkRepository.startVerification("secret", "key", "cookie")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(consumerSession)
    }

    @Test
    fun `startVerification catches exception and returns failure`() = runTest {
        whenever(
            consumersApiService.startConsumerVerification(
                any(),
                any(),
                anyOrNull(),
                any(),
                any()
            )
        )
            .thenThrow(RuntimeException("error"))

        val result = linkRepository.startVerification("secret", "key", "cookie")

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `confirmVerification sends correct parameters`() = runTest {
        val secret = "secret"
        val code = "code"
        val cookie = "cookie2"
        val consumerKey = "key2"
        linkRepository.confirmVerification(code, secret, consumerKey, cookie)

        verify(consumersApiService).confirmConsumerVerification(
            consumerSessionClientSecret = eq(secret),
            verificationCode = eq(code),
            authSessionCookie = eq(cookie),
            requestSurface = eq(CONSUMER_SURFACE),
            requestOptions = eq(ApiRequest.Options(consumerKey))
        )
    }

    @Test
    fun `confirmVerification without consumerPublishableKey sends correct parameters`() = runTest {
        val secret = "secret"
        val code = "code"
        val cookie = "cookie2"
        linkRepository.confirmVerification(code, secret, null, cookie)

        verify(consumersApiService).confirmConsumerVerification(
            consumerSessionClientSecret = eq(secret),
            verificationCode = eq(code),
            authSessionCookie = eq(cookie),
            requestSurface = eq(CONSUMER_SURFACE),
            requestOptions = eq(ApiRequest.Options(PUBLISHABLE_KEY, STRIPE_ACCOUNT_ID))
        )
    }

    @Test
    fun `confirmVerification returns successful result`() = runTest {
        val consumerSession = mock<ConsumerSession>()
        whenever(
            consumersApiService.confirmConsumerVerification(
                consumerSessionClientSecret = any(),
                verificationCode = any(),
                authSessionCookie = anyOrNull(),
                requestSurface = any(),
                requestOptions = any()
            )
        )
            .thenReturn(consumerSession)

        val result = linkRepository.confirmVerification(
            verificationCode = "code",
            consumerSessionClientSecret = "secret",
            consumerPublishableKey = "key",
            authSessionCookie = "cookie"
        )

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(consumerSession)
    }

    @Test
    fun `confirmVerification catches exception and returns failure`() = runTest {
        whenever(
            consumersApiService.confirmConsumerVerification(
                consumerSessionClientSecret = any(),
                verificationCode = any(),
                authSessionCookie = anyOrNull(),
                requestSurface = any(),
                requestOptions = any()
            )
        )
            .thenThrow(RuntimeException("error"))

        val result = linkRepository.confirmVerification(
            verificationCode = "code",
            consumerSessionClientSecret = "secret",
            consumerPublishableKey = "key",
            authSessionCookie = "cookie"
        )

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `logout sends correct parameters`() = runTest {
        val secret = "secret"
        val cookie = "cookie2"
        val consumerKey = "key2"
        linkRepository.logout(secret, consumerKey, cookie)

        verify(stripeRepository).logoutConsumer(
            eq(secret),
            eq(cookie),
            eq(ApiRequest.Options(consumerKey))
        )
    }

    @Test
    fun `logout without consumerPublishableKey sends correct parameters`() = runTest {
        val secret = "secret"
        val cookie = "cookie2"
        linkRepository.logout(secret, null, cookie)

        verify(stripeRepository).logoutConsumer(
            eq(secret),
            eq(cookie),
            eq(ApiRequest.Options(PUBLISHABLE_KEY, STRIPE_ACCOUNT_ID))
        )
    }

    @Test
    fun `logout returns successful result`() = runTest {
        val consumerSession = mock<ConsumerSession>()
        whenever(stripeRepository.logoutConsumer(any(), any(), any()))
            .thenReturn(consumerSession)

        val result = linkRepository.logout("secret", "key", "cookie")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(consumerSession)
    }

    @Test
    fun `logout catches exception and returns failure`() = runTest {
        whenever(stripeRepository.logoutConsumer(any(), any(), any()))
            .thenThrow(RuntimeException("error"))

        val result = linkRepository.logout("secret", "key", "cookie")

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `listPaymentDetails sends correct parameters`() = runTest {
        val secret = "secret"
        val consumerKey = "key"
        linkRepository.listPaymentDetails(secret, consumerKey)

        verify(stripeRepository).listPaymentDetails(
            eq(secret),
            eq(SupportedPaymentMethod.allTypes),
            eq(ApiRequest.Options(consumerKey))
        )
    }

    @Test
    fun `listPaymentDetails without consumerPublishableKey sends correct parameters`() = runTest {
        val secret = "secret"
        linkRepository.listPaymentDetails(secret, null)

        verify(stripeRepository).listPaymentDetails(
            eq(secret),
            eq(SupportedPaymentMethod.allTypes),
            eq(ApiRequest.Options(PUBLISHABLE_KEY, STRIPE_ACCOUNT_ID))
        )
    }

    @Test
    fun `listPaymentDetails returns successful result`() = runTest {
        val paymentDetails = mock<ConsumerPaymentDetails>()
        whenever(stripeRepository.listPaymentDetails(any(), any(), any()))
            .thenReturn(paymentDetails)

        val result = linkRepository.listPaymentDetails("secret", "key")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(paymentDetails)
    }

    @Test
    fun `listPaymentDetails catches exception and returns failure`() = runTest {
        whenever(stripeRepository.listPaymentDetails(any(), any(), any()))
            .thenThrow(RuntimeException("error"))

        val result = linkRepository.listPaymentDetails("secret", "key")

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `deletePaymentDetails sends correct parameters`() = runTest {
        val secret = "secret"
        val id = "payment_details_id"
        val consumerKey = "key"
        linkRepository.deletePaymentDetails(id, secret, consumerKey)

        verify(stripeRepository).deletePaymentDetails(
            eq(secret),
            eq(id),
            eq(ApiRequest.Options(consumerKey))
        )
    }

    @Test
    fun `deletePaymentDetails without consumerPublishableKey sends correct parameters`() = runTest {
        val secret = "secret"
        val id = "payment_details_id"
        linkRepository.deletePaymentDetails(id, secret, null)

        verify(stripeRepository).deletePaymentDetails(
            eq(secret),
            eq(id),
            eq(ApiRequest.Options(PUBLISHABLE_KEY, STRIPE_ACCOUNT_ID))
        )
    }

    @Test
    fun `deletePaymentDetails returns successful result`() = runTest {
        whenever(stripeRepository.deletePaymentDetails(any(), any(), any())).thenReturn(Unit)

        val result = linkRepository.deletePaymentDetails("id", "secret", "key")

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `deletePaymentDetails catches exception and returns failure`() = runTest {
        whenever(stripeRepository.deletePaymentDetails(any(), any(), any()))
            .thenThrow(RuntimeException("error"))

        val result = linkRepository.deletePaymentDetails("id", "secret", "key")

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `createFinancialConnectionsSession sends correct parameters`() = runTest {
        val secret = "secret"
        val consumerKey = "key"
        linkRepository.createFinancialConnectionsSession(secret, consumerKey)

        verify(stripeRepository).createLinkFinancialConnectionsSession(
            eq(secret),
            eq(ApiRequest.Options(consumerKey))
        )
    }

    @Test
    fun `createFinancialConnectionsSession returns successful result`() = runTest {
        val session = FinancialConnectionsSession("client_secret", "id")
        whenever(stripeRepository.createLinkFinancialConnectionsSession(any(), any())).thenReturn(
            session
        )

        val result = linkRepository.createFinancialConnectionsSession("secret", "key")

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `createFinancialConnectionsSession catches exception and returns failure`() = runTest {
        whenever(stripeRepository.createLinkFinancialConnectionsSession(any(), any()))
            .thenThrow(RuntimeException("error"))

        val result = linkRepository.createFinancialConnectionsSession("secret", "key")

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `createPaymentDetails for financial connections sends correct parameters`() = runTest {
        val accountId = "id"
        val secret = "secret"
        val consumerKey = "key"

        linkRepository.createBankAccountPaymentDetails(
            financialConnectionsAccountId = accountId,
            consumerSessionClientSecret = secret,
            consumerPublishableKey = consumerKey
        )

        verify(stripeRepository).createPaymentDetails(
            eq(secret),
            eq(accountId),
            eq(ApiRequest.Options(consumerKey))
        )
    }

    @Test
    fun `createPaymentDetails for financial connections returns new LinkPaymentDetails when successful`() =
        runTest {
            val accountId = "id"
            val secret = "secret"
            val consumerKey = "key"

            val paymentDetails = PaymentDetailsFixtures.CONSUMER_SINGLE_BANK_ACCOUNT_PAYMENT_DETAILS
            whenever(stripeRepository.createPaymentDetails(any(), any<String>(), any()))
                .thenReturn(paymentDetails)

            val result = linkRepository.createBankAccountPaymentDetails(
                financialConnectionsAccountId = accountId,
                consumerSessionClientSecret = secret,
                consumerPublishableKey = consumerKey
            )

            assertThat(result.isSuccess).isTrue()
        }

    @Test
    fun `createPaymentDetails for financial connections catches exception and returns failure`() =
        runTest {
            val accountId = "id"
            val secret = "secret"
            val consumerKey = "key"

            whenever(stripeRepository.createPaymentDetails(any(), any<String>(), any()))
                .thenThrow(RuntimeException("error"))

            val result = linkRepository.createBankAccountPaymentDetails(
                financialConnectionsAccountId = accountId,
                consumerSessionClientSecret = secret,
                consumerPublishableKey = consumerKey
            )

            assertThat(result.isFailure).isTrue()
        }

    @Test
    fun `createPaymentDetails for card sends correct parameters`() = runTest {
        val secret = "secret"
        val email = "email@stripe.com"
        val consumerKey = "key"

        linkRepository.createCardPaymentDetails(
            paymentMethodCreateParams = cardPaymentMethodCreateParams,
            userEmail = email,
            stripeIntent = paymentIntent,
            consumerSessionClientSecret = secret,
            consumerPublishableKey = consumerKey
        )

        verify(stripeRepository).createPaymentDetails(
            eq(secret),
            argThat<ConsumerPaymentDetailsCreateParams> {
                toParamMap() == mapOf(
                    "type" to "card",
                    "billing_email_address" to "email@stripe.com",
                    "card" to mapOf(
                        "number" to "5555555555554444",
                        "exp_month" to "12",
                        "exp_year" to "2050"
                    ),
                    "billing_address" to mapOf(
                        "country_code" to "US",
                        "postal_code" to "12345"
                    )
                )
            },
            eq(ApiRequest.Options(consumerKey))
        )
    }

    @Test
    fun `createPaymentDetails for card without consumerPublishableKey sends correct parameters`() =
        runTest {
            val secret = "secret"
            val email = "email@stripe.com"

            linkRepository.createCardPaymentDetails(
                paymentMethodCreateParams = cardPaymentMethodCreateParams,
                userEmail = email,
                stripeIntent = paymentIntent,
                consumerSessionClientSecret = secret,
                consumerPublishableKey = null
            )

            verify(stripeRepository).createPaymentDetails(
                eq(secret),
                argThat<ConsumerPaymentDetailsCreateParams> {
                    toParamMap() == mapOf(
                        "type" to "card",
                        "billing_email_address" to "email@stripe.com",
                        "card" to mapOf(
                            "number" to "5555555555554444",
                            "exp_month" to "12",
                            "exp_year" to "2050"
                        ),
                        "billing_address" to mapOf(
                            "country_code" to "US",
                            "postal_code" to "12345"
                        )
                    )
                },
                eq(ApiRequest.Options(PUBLISHABLE_KEY, STRIPE_ACCOUNT_ID))
            )
        }

    @Test
    fun `createPaymentDetails for card returns new LinkPaymentDetails when successful`() = runTest {
        val consumerSessionSecret = "consumer_session_secret"
        val email = "email@stripe.com"
        val paymentDetails = PaymentDetailsFixtures.CONSUMER_SINGLE_PAYMENT_DETAILS
        whenever(
            stripeRepository.createPaymentDetails(
                any(),
                any<ConsumerPaymentDetailsCreateParams>(),
                any()
            )
        )
            .thenReturn(paymentDetails)

        val result = linkRepository.createCardPaymentDetails(
            paymentMethodCreateParams = cardPaymentMethodCreateParams,
            userEmail = email,
            stripeIntent = paymentIntent,
            consumerSessionClientSecret = consumerSessionSecret,
            consumerPublishableKey = null
        )

        assertThat(result.isSuccess).isTrue()

        val newLinkPaymentDetails = result.getOrThrow()

        assertThat(newLinkPaymentDetails.paymentDetails)
            .isEqualTo(paymentDetails.paymentDetails.first())
        assertThat(newLinkPaymentDetails.paymentMethodCreateParams)
            .isEqualTo(
                PaymentMethodCreateParams.createLink(
                    paymentDetails.paymentDetails.first().id,
                    consumerSessionSecret,
                    mapOf("card" to mapOf("cvc" to "123"))
                )
            )
        assertThat(newLinkPaymentDetails.buildFormValues()).isEqualTo(
            mapOf(
                IdentifierSpec.get("type") to "card",
                IdentifierSpec.CardNumber to "5555555555554444",
                IdentifierSpec.CardCvc to "123",
                IdentifierSpec.CardExpMonth to "12",
                IdentifierSpec.CardExpYear to "2050",
                IdentifierSpec.Country to "US",
                IdentifierSpec.PostalCode to "12345"
            )
        )
    }

    @Test
    fun `createPaymentDetails for card catches exception and returns failure`() = runTest {
        whenever(
            stripeRepository.createPaymentDetails(
                any(),
                any<ConsumerPaymentDetailsCreateParams>(),
                any()
            )
        )
            .thenThrow(RuntimeException("error"))

        val result = linkRepository.createCardPaymentDetails(
            paymentMethodCreateParams = cardPaymentMethodCreateParams,
            userEmail = "email@stripe.com",
            stripeIntent = paymentIntent,
            consumerSessionClientSecret = "secret",
            consumerPublishableKey = null
        )

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `updatePaymentDetails sends correct parameters`() = runTest {
        val secret = "secret"
        val params = ConsumerPaymentDetailsUpdateParams("id")
        val consumerKey = "key"

        linkRepository.updatePaymentDetails(
            params,
            secret,
            consumerKey
        )

        verify(stripeRepository).updatePaymentDetails(
            eq(secret),
            eq(params),
            eq(ApiRequest.Options(consumerKey))
        )
    }

    @Test
    fun `updatePaymentDetails without consumerPublishableKey sends correct parameters`() = runTest {
        val secret = "secret"
        val params = ConsumerPaymentDetailsUpdateParams("id")

        linkRepository.updatePaymentDetails(
            params,
            secret,
            null
        )

        verify(stripeRepository).updatePaymentDetails(
            eq(secret),
            eq(params),
            eq(ApiRequest.Options(PUBLISHABLE_KEY, STRIPE_ACCOUNT_ID))
        )
    }

    @Test
    fun `updatePaymentDetails returns successful result`() = runTest {
        val consumerPaymentDetails = mock<ConsumerPaymentDetails>()

        whenever(stripeRepository.updatePaymentDetails(any(), any(), any()))
            .thenReturn(consumerPaymentDetails)

        val result = linkRepository.updatePaymentDetails(
            ConsumerPaymentDetailsUpdateParams("id"),
            "secret",
            "key"
        )

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(consumerPaymentDetails)
    }

    @Test
    fun `updatePaymentDetails catches exception and returns failure`() = runTest {
        whenever(stripeRepository.updatePaymentDetails(any(), any(), any()))
            .thenThrow(RuntimeException("error"))

        val result = linkRepository.updatePaymentDetails(
            ConsumerPaymentDetailsUpdateParams("id"),
            "secret",
            "key"
        )

        assertThat(result.isFailure).isTrue()
    }

    private val cardPaymentMethodCreateParams =
        FieldValuesToParamsMapConverter.transformToPaymentMethodCreateParams(
            mapOf(
                IdentifierSpec.CardNumber to FormFieldEntry("5555555555554444", true),
                IdentifierSpec.CardCvc to FormFieldEntry("123", true),
                IdentifierSpec.CardExpMonth to FormFieldEntry("12", true),
                IdentifierSpec.CardExpYear to FormFieldEntry("2050", true),
                IdentifierSpec.Country to FormFieldEntry("US", true),
                IdentifierSpec.PostalCode to FormFieldEntry("12345", true)
            ),
            "card",
            false
        )

    companion object {
        const val PUBLISHABLE_KEY = "publishableKey"
        const val STRIPE_ACCOUNT_ID = "stripeAccountId"
        const val CONSUMER_SURFACE = "android_payment_element"
    }
}
