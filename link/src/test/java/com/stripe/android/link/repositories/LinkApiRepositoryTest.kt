package com.stripe.android.link.repositories

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.confirmation.ConfirmPaymentIntentParamsFactory
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetailsCreateParams
import com.stripe.android.model.ConsumerPaymentDetailsUpdateParams
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.PaymentIntent
import com.stripe.android.networking.StripeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Locale

@ExperimentalCoroutinesApi
class LinkApiRepositoryTest {
    private val stripeRepository = mock<StripeRepository>()

    private val paymentIntent = mock<PaymentIntent>().apply {
        whenever(clientSecret).thenReturn("secret")
    }

    private val linkRepository = LinkApiRepository(
        publishableKeyProvider = { PUBLISHABLE_KEY },
        stripeAccountIdProvider = { STRIPE_ACCOUNT_ID },
        stripeRepository = stripeRepository,
        logger = Logger.noop(),
        workContext = Dispatchers.IO,
        locale = Locale.US
    )

    @Test
    fun `lookupConsumer sends correct parameters`() = runTest {
        val email = "email@example.com"
        val cookie = "cookie1"
        linkRepository.lookupConsumer(email, cookie)

        verify(stripeRepository).lookupConsumerSession(
            eq(email),
            eq(cookie),
            eq(ApiRequest.Options(PUBLISHABLE_KEY, STRIPE_ACCOUNT_ID))
        )
    }

    @Test
    fun `lookupConsumer returns successful result`() = runTest {
        val consumerSessionLookup = mock<ConsumerSessionLookup>()
        whenever(stripeRepository.lookupConsumerSession(any(), any(), any()))
            .thenReturn(consumerSessionLookup)

        val result = linkRepository.lookupConsumer("email", "cookie")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(consumerSessionLookup)
    }

    @Test
    fun `lookupConsumer catches exception and returns failure`() = runTest {
        whenever(stripeRepository.lookupConsumerSession(any(), any(), any()))
            .thenThrow(RuntimeException("error"))

        val result = linkRepository.lookupConsumer("email", "cookie")

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `consumerSignUp sends correct parameters`() = runTest {
        val email = "email@example.com"
        val phone = "phone"
        val country = "US"
        val cookie = "cookie2"
        linkRepository.consumerSignUp(email, phone, country, cookie)

        verify(stripeRepository).consumerSignUp(
            eq(email),
            eq(phone),
            eq(country),
            eq(cookie),
            eq(ApiRequest.Options(PUBLISHABLE_KEY, STRIPE_ACCOUNT_ID))
        )
    }

    @Test
    fun `consumerSignUp returns successful result`() = runTest {
        val consumerSession = mock<ConsumerSession>()
        whenever(stripeRepository.consumerSignUp(any(), any(), any(), anyOrNull(), any()))
            .thenReturn(consumerSession)

        val result = linkRepository.consumerSignUp("email", "phone", "country", "cookie")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(consumerSession)
    }

    @Test
    fun `consumerSignUp catches exception and returns failure`() = runTest {
        whenever(stripeRepository.consumerSignUp(any(), any(), any(), anyOrNull(), any()))
            .thenThrow(RuntimeException("error"))

        val result = linkRepository.consumerSignUp("email", "phone", "country", "cookie")

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `startVerification sends correct parameters`() = runTest {
        val secret = "secret"
        val cookie = "cookie1"
        val consumerKey = "key"
        linkRepository.startVerification(secret, consumerKey, cookie)

        verify(stripeRepository).startConsumerVerification(
            eq(secret),
            eq(Locale.US),
            eq(cookie),
            eq(ApiRequest.Options(consumerKey))
        )
    }

    @Test
    fun `startVerification without consumerPublishableKey sends correct parameters`() = runTest {
        val secret = "secret"
        val cookie = "cookie1"
        linkRepository.startVerification(secret, null, cookie)

        verify(stripeRepository).startConsumerVerification(
            eq(secret),
            eq(Locale.US),
            eq(cookie),
            eq(ApiRequest.Options(PUBLISHABLE_KEY, STRIPE_ACCOUNT_ID))
        )
    }

    @Test
    fun `startVerification returns successful result`() = runTest {
        val consumerSession = mock<ConsumerSession>()
        whenever(stripeRepository.startConsumerVerification(any(), any(), anyOrNull(), any()))
            .thenReturn(consumerSession)

        val result = linkRepository.startVerification("secret", "key", "cookie")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(consumerSession)
    }

    @Test
    fun `startVerification catches exception and returns failure`() = runTest {
        whenever(stripeRepository.startConsumerVerification(any(), any(), anyOrNull(), any()))
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

        verify(stripeRepository).confirmConsumerVerification(
            eq(secret),
            eq(code),
            eq(cookie),
            eq(ApiRequest.Options(consumerKey))
        )
    }

    @Test
    fun `confirmVerification without consumerPublishableKey sends correct parameters`() = runTest {
        val secret = "secret"
        val code = "code"
        val cookie = "cookie2"
        linkRepository.confirmVerification(code, secret, null, cookie)

        verify(stripeRepository).confirmConsumerVerification(
            eq(secret),
            eq(code),
            eq(cookie),
            eq(ApiRequest.Options(PUBLISHABLE_KEY, STRIPE_ACCOUNT_ID))
        )
    }

    @Test
    fun `confirmVerification returns successful result`() = runTest {
        val consumerSession = mock<ConsumerSession>()
        whenever(stripeRepository.confirmConsumerVerification(any(), any(), anyOrNull(), any()))
            .thenReturn(consumerSession)

        val result = linkRepository.confirmVerification("code", "secret", "key", "cookie")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(consumerSession)
    }

    @Test
    fun `confirmVerification catches exception and returns failure`() = runTest {
        whenever(stripeRepository.confirmConsumerVerification(any(), any(), anyOrNull(), any()))
            .thenThrow(RuntimeException("error"))

        val result = linkRepository.confirmVerification("code", "secret", "key", "cookie")

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
            argThat { contains("card") && size == 1 },
            eq(ApiRequest.Options(consumerKey))
        )
    }

    @Test
    fun `listPaymentDetails without consumerPublishableKey sends correct parameters`() = runTest {
        val secret = "secret"
        linkRepository.listPaymentDetails(secret, null)

        verify(stripeRepository).listPaymentDetails(
            eq(secret),
            argThat { contains("card") && size == 1 },
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
    fun `createPaymentDetails sends correct parameters`() = runTest {
        val secret = "secret"
        val consumerPaymentDetailsCreateParams =
            ConsumerPaymentDetailsCreateParams.Card(emptyMap(), "email@stripe.com")
        val consumerKey = "key"

        linkRepository.createPaymentDetails(
            paymentDetails = consumerPaymentDetailsCreateParams,
            stripeIntent = paymentIntent,
            extraConfirmationParams = null,
            consumerSessionClientSecret = secret,
            consumerPublishableKey = consumerKey
        )

        verify(stripeRepository).createPaymentDetails(
            eq(secret),
            eq(consumerPaymentDetailsCreateParams),
            eq(ApiRequest.Options(consumerKey))
        )
    }

    @Test
    fun `createPaymentDetails without consumerPublishableKey sends correct parameters`() = runTest {
        val secret = "secret"
        val consumerPaymentDetailsCreateParams =
            ConsumerPaymentDetailsCreateParams.Card(emptyMap(), "email@stripe.com")

        linkRepository.createPaymentDetails(
            paymentDetails = consumerPaymentDetailsCreateParams,
            stripeIntent = paymentIntent,
            extraConfirmationParams = null,
            consumerSessionClientSecret = secret,
            consumerPublishableKey = null
        )

        verify(stripeRepository).createPaymentDetails(
            eq(secret),
            eq(consumerPaymentDetailsCreateParams),
            eq(ApiRequest.Options(PUBLISHABLE_KEY, STRIPE_ACCOUNT_ID))
        )
    }

    @Test
    fun `createPaymentDetails returns successful result`() = runTest {
        val secret = "secret"
        val paymentDetails = mock<ConsumerPaymentDetails.PaymentDetails>().apply {
            whenever(id).thenReturn("id")
        }
        val consumerPaymentDetails = mock<ConsumerPaymentDetails>().apply {
            whenever(this.paymentDetails).thenReturn(listOf(paymentDetails))
        }
        whenever(stripeRepository.createPaymentDetails(any(), any(), any()))
            .thenReturn(consumerPaymentDetails)

        val result = linkRepository.createPaymentDetails(
            paymentDetails = ConsumerPaymentDetailsCreateParams.Card(
                emptyMap(),
                "email@stripe.com"
            ),
            stripeIntent = paymentIntent,
            extraConfirmationParams = null,
            consumerSessionClientSecret = secret,
            consumerPublishableKey = null
        )

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(
            LinkPaymentDetails(
                paymentDetails,
                ConfirmPaymentIntentParamsFactory(paymentIntent)
                    .createPaymentMethodCreateParams(
                        secret,
                        paymentDetails
                    )
            )
        )
    }

    @Test
    fun `createPaymentDetails catches exception and returns failure`() = runTest {
        whenever(stripeRepository.createPaymentDetails(any(), any(), any()))
            .thenThrow(RuntimeException("error"))

        val result = linkRepository.createPaymentDetails(
            paymentDetails = ConsumerPaymentDetailsCreateParams.Card(
                emptyMap(),
                "email@stripe.com"
            ),
            stripeIntent = paymentIntent,
            extraConfirmationParams = null,
            consumerSessionClientSecret = "secret",
            consumerPublishableKey = null
        )

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `updatePaymentDetails sends correct parameters`() = runTest {
        val secret = "secret"
        val params = ConsumerPaymentDetailsUpdateParams.Card("id")
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
        val params = ConsumerPaymentDetailsUpdateParams.Card("id")

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
            ConsumerPaymentDetailsUpdateParams.Card("id"),
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
            ConsumerPaymentDetailsUpdateParams.Card("id"),
            "secret",
            "key"
        )

        assertThat(result.isFailure).isTrue()
    }

    companion object {
        const val PUBLISHABLE_KEY = "publishableKey"
        const val STRIPE_ACCOUNT_ID = "stripeAccountId"
    }
}
