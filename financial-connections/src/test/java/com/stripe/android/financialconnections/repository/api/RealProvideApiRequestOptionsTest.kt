package com.stripe.android.financialconnections.repository.api

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.repository.CachedConsumerSession
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RealProvideApiRequestOptionsTest {

    private val merchantApiRequestOptions = ApiRequest.Options(
        apiKey = ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY,
        stripeAccount = "acct_123",
    )

    @Test
    fun `Provides consumer API options for a verified session`() = runTest {
        val cachedConsumerSession = makeCachedConsumerSession(
            isVerified = true,
            publishableKey = "pk_123"
        )

        val provideApiRequestOptions = RealProvideApiRequestOptions(
            consumerSessionProvider = { cachedConsumerSession },
            isLinkWithStripe = { true },
            apiRequestOptions = merchantApiRequestOptions,
        )

        val apiOptions = provideApiRequestOptions(useConsumerPublishableKey = true)
        assertThat(apiOptions).isEqualTo(
            ApiRequest.Options(apiKey = "pk_123")
        )
    }

    @Test
    fun `Provides merchant API options if not in Instant Debits`() = runTest {
        val cachedConsumerSession = makeCachedConsumerSession(
            isVerified = true,
            publishableKey = "pk_123",
        )

        val provideApiRequestOptions = RealProvideApiRequestOptions(
            consumerSessionProvider = { cachedConsumerSession },
            isLinkWithStripe = { false },
            apiRequestOptions = merchantApiRequestOptions,
        )

        val consumerApiOptions = provideApiRequestOptions(useConsumerPublishableKey = true)
        assertThat(consumerApiOptions).isEqualTo(merchantApiRequestOptions)
    }

    @Test
    fun `Provides merchant API options for an unverified session`() = runTest {
        val cachedConsumerSession = makeCachedConsumerSession(
            isVerified = false,
            publishableKey = "pk_123"
        )

        val provideApiRequestOptions = RealProvideApiRequestOptions(
            consumerSessionProvider = { cachedConsumerSession },
            isLinkWithStripe = { true },
            apiRequestOptions = merchantApiRequestOptions,
        )

        val consumerApiOptions = provideApiRequestOptions(useConsumerPublishableKey = true)
        assertThat(consumerApiOptions).isEqualTo(merchantApiRequestOptions)
    }

    @Test
    fun `Provides merchant API options if no consumer publishable key`() = runTest {
        val cachedConsumerSession = makeCachedConsumerSession(
            isVerified = false,
            publishableKey = null,
        )

        val provideApiRequestOptions = RealProvideApiRequestOptions(
            consumerSessionProvider = { cachedConsumerSession },
            isLinkWithStripe = { true },
            apiRequestOptions = merchantApiRequestOptions,
        )

        val consumerApiOptions = provideApiRequestOptions(useConsumerPublishableKey = true)
        assertThat(consumerApiOptions).isEqualTo(merchantApiRequestOptions)
    }

    @Test
    fun `Provides merchant API options if consumer options not requested`() = runTest {
        val cachedConsumerSession = makeCachedConsumerSession(
            isVerified = true,
            publishableKey = "pk_123",
        )

        val provideApiRequestOptions = RealProvideApiRequestOptions(
            consumerSessionProvider = { cachedConsumerSession },
            isLinkWithStripe = { true },
            apiRequestOptions = merchantApiRequestOptions,
        )

        val consumerApiOptions = provideApiRequestOptions(useConsumerPublishableKey = false)
        assertThat(consumerApiOptions).isEqualTo(merchantApiRequestOptions)
    }

    private fun makeCachedConsumerSession(
        isVerified: Boolean,
        publishableKey: String?,
    ): CachedConsumerSession {
        return CachedConsumerSession(
            clientSecret = "clientSecret",
            emailAddress = "test@test.com",
            phoneNumber = "(***) *** **12",
            isVerified = isVerified,
            publishableKey = publishableKey,
        )
    }
}
