package com.stripe.android.link.repositories

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.networking.StripeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Locale

@ExperimentalCoroutinesApi
class LinkApiRepositoryTest {
    private val stripeRepository = mock<StripeRepository>()

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
        linkRepository.lookupConsumer(email)

        verify(stripeRepository).lookupConsumerSession(
            eq(email),
            eq(ApiRequest.Options(PUBLISHABLE_KEY, STRIPE_ACCOUNT_ID))
        )
    }

    @Test
    fun `lookupConsumer returns successful result`() = runTest {
        val consumerSessionLookup = mock<ConsumerSessionLookup>()
        whenever(stripeRepository.lookupConsumerSession(any(), any()))
            .thenReturn(consumerSessionLookup)

        val result = linkRepository.lookupConsumer("email")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(consumerSessionLookup)
    }

    @Test
    fun `lookupConsumer catches exception and returns failure`() = runTest {
        whenever(stripeRepository.lookupConsumerSession(any(), any()))
            .thenThrow(RuntimeException("error"))

        val result = linkRepository.lookupConsumer("email")

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `consumerSignUp sends correct parameters`() = runTest {
        val email = "email@example.com"
        val phone = "phone"
        val country = "US"
        linkRepository.consumerSignUp(email, phone, country)

        verify(stripeRepository).consumerSignUp(
            eq(email),
            eq(phone),
            eq(country),
            isNull(),
            eq(ApiRequest.Options(PUBLISHABLE_KEY, STRIPE_ACCOUNT_ID))
        )
    }

    @Test
    fun `consumerSignUp returns successful result`() = runTest {
        val consumerSession = mock<ConsumerSession>()
        whenever(stripeRepository.consumerSignUp(any(), any(), any(), anyOrNull(), any()))
            .thenReturn(consumerSession)

        val result = linkRepository.consumerSignUp("email", "phone", "country")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(consumerSession)
    }

    @Test
    fun `consumerSignUp catches exception and returns failure`() = runTest {
        whenever(stripeRepository.consumerSignUp(any(), any(), any(), anyOrNull(), any()))
            .thenThrow(RuntimeException("error"))

        val result = linkRepository.consumerSignUp("email", "phone", "country")

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `startVerification sends correct parameters`() = runTest {
        val secret = "secret"
        linkRepository.startVerification(secret)

        verify(stripeRepository).startConsumerVerification(
            eq(secret),
            eq(Locale.US),
            isNull(),
            eq(ApiRequest.Options(PUBLISHABLE_KEY, STRIPE_ACCOUNT_ID))
        )
    }

    @Test
    fun `startVerification returns successful result`() = runTest {
        val consumerSession = mock<ConsumerSession>()
        whenever(stripeRepository.startConsumerVerification(any(), any(), anyOrNull(), any()))
            .thenReturn(consumerSession)

        val result = linkRepository.startVerification("secret")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(consumerSession)
    }

    @Test
    fun `startVerification catches exception and returns failure`() = runTest {
        whenever(stripeRepository.startConsumerVerification(any(), any(), anyOrNull(), any()))
            .thenThrow(RuntimeException("error"))

        val result = linkRepository.startVerification("secret")

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `confirmVerification sends correct parameters`() = runTest {
        val secret = "secret"
        val code = "code"
        linkRepository.confirmVerification(secret, code)

        verify(stripeRepository).confirmConsumerVerification(
            eq(secret),
            eq(code),
            isNull(),
            eq(ApiRequest.Options(PUBLISHABLE_KEY, STRIPE_ACCOUNT_ID))
        )
    }

    @Test
    fun `confirmVerification returns successful result`() = runTest {
        val consumerSession = mock<ConsumerSession>()
        whenever(stripeRepository.confirmConsumerVerification(any(), any(), anyOrNull(), any()))
            .thenReturn(consumerSession)

        val result = linkRepository.confirmVerification("secret", "code")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(consumerSession)
    }

    @Test
    fun `confirmVerification catches exception and returns failure`() = runTest {
        whenever(stripeRepository.confirmConsumerVerification(any(), any(), anyOrNull(), any()))
            .thenThrow(RuntimeException("error"))

        val result = linkRepository.confirmVerification("secret", "code")

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `listPaymentDetails sends correct parameters`() = runTest {
        val secret = "secret"
        linkRepository.listPaymentDetails(secret)

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

        val result = linkRepository.listPaymentDetails("secret")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(paymentDetails)
    }

    @Test
    fun `listPaymentDetails catches exception and returns failure`() = runTest {
        whenever(stripeRepository.listPaymentDetails(any(), any(), any()))
            .thenThrow(RuntimeException("error"))

        val result = linkRepository.listPaymentDetails("secret")

        assertThat(result.isFailure).isTrue()
    }

    companion object {
        const val PUBLISHABLE_KEY = "publishableKey"
        const val STRIPE_ACCOUNT_ID = "stripeAccountId"
    }
}
