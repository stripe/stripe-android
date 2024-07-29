package com.stripe.android.financialconnections.repository.api

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.financialconnections.repository.CachedConsumerSession
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RealProvideConsumerApiOptionsTest {

    @Test
    fun `Creates consumer API options for a verified session`() = runTest {
        val cachedConsumerSession = CachedConsumerSession(
            clientSecret = "clientSecret",
            emailAddress = "test@test.com",
            phoneNumber = "(***) *** **12",
            isVerified = true,
            publishableKey = "pk_123"
        )

        val provideConsumerApiOptions = RealProvideConsumerApiOptions(
            consumerSessionProvider = { cachedConsumerSession },
            isLinkWithStripe = { true },
        )

        val consumerApiOptions = provideConsumerApiOptions()
        assertThat(consumerApiOptions).isEqualTo(
            ApiRequest.Options(apiKey = "pk_123")
        )
    }

    @Test
    fun `Does not create consumer API options if not in Instant Debits`() = runTest {
        val cachedConsumerSession = CachedConsumerSession(
            clientSecret = "clientSecret",
            emailAddress = "test@test.com",
            phoneNumber = "(***) *** **12",
            isVerified = true,
            publishableKey = "pk_123",
        )

        val provideConsumerApiOptions = RealProvideConsumerApiOptions(
            consumerSessionProvider = { cachedConsumerSession },
            isLinkWithStripe = { false },
        )

        val consumerApiOptions = provideConsumerApiOptions()
        assertThat(consumerApiOptions).isNull()
    }

    @Test
    fun `Does not create consumer API options for an unverified session`() = runTest {
        val cachedConsumerSession = CachedConsumerSession(
            clientSecret = "clientSecret",
            emailAddress = "test@test.com",
            phoneNumber = "(***) *** **12",
            isVerified = false,
            publishableKey = "pk_123"
        )

        val provideConsumerApiOptions = RealProvideConsumerApiOptions(
            consumerSessionProvider = { cachedConsumerSession },
            isLinkWithStripe = { true },
        )

        val consumerApiOptions = provideConsumerApiOptions()
        assertThat(consumerApiOptions).isNull()
    }

    @Test
    fun `Does not create consumer API options if no consumer publishable key`() = runTest {
        val cachedConsumerSession = CachedConsumerSession(
            clientSecret = "clientSecret",
            emailAddress = "test@test.com",
            phoneNumber = "(***) *** **12",
            isVerified = false,
            publishableKey = null,
        )

        val provideConsumerApiOptions = RealProvideConsumerApiOptions(
            consumerSessionProvider = { cachedConsumerSession },
            isLinkWithStripe = { true },
        )

        val consumerApiOptions = provideConsumerApiOptions()
        assertThat(consumerApiOptions).isNull()
    }
}
