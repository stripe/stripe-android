package com.stripe.android.financialconnections.repository

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.financialconnections.ApiKeyFixtures
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RealApiRequestOptionsProviderTest {

    private val originalRequestOptions = ApiRequest.Options(
        apiKey = ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY,
        stripeAccount = null,
    )

    private val consumerPublishableKey = "pk_this_is_not_valid_but_its_ok_for_a_test"

    @Test
    fun `Only sets consumer publishable key tentatively`() = runTest {
        val savedStateHandle = SavedStateHandle()

        val provider = RealApiRequestOptionsProvider(
            originalRequestOptions = originalRequestOptions,
            savedStateHandle = savedStateHandle,
            isLinkWithStripe = { true },
        )

        provider.setTentativeConsumerPublishableKey(consumerPublishableKey)
        provider.assertIsProvidingOriginalPublishableKey()
    }

    @Test
    fun `Provides consumer publishable key after being confirmed`() = runTest {
        val savedStateHandle = SavedStateHandle()

        val provider = RealApiRequestOptionsProvider(
            originalRequestOptions = originalRequestOptions,
            savedStateHandle = savedStateHandle,
            isLinkWithStripe = { true },
        )

        provider.setTentativeConsumerPublishableKey(consumerPublishableKey)
        provider.assertIsProvidingOriginalPublishableKey()

        provider.confirmConsumerPublishableKey()
        provider.assertIsProvidingConsumerPublishableKey()
    }

    @Test
    fun `Does not set consumer publishable key if not in Instant Debits flow`() = runTest {
        val provider = RealApiRequestOptionsProvider(
            originalRequestOptions = originalRequestOptions,
            savedStateHandle = SavedStateHandle(),
            isLinkWithStripe = { false },
        )

        provider.setTentativeConsumerPublishableKey(consumerPublishableKey)
        provider.confirmConsumerPublishableKey()

        provider.assertIsProvidingOriginalPublishableKey()
    }

    @Test
    fun `Sets consumer publishable key if in Instant Debits flow`() = runTest {
        val provider = RealApiRequestOptionsProvider(
            originalRequestOptions = originalRequestOptions,
            savedStateHandle = SavedStateHandle(),
            isLinkWithStripe = { true },
        )

        provider.setTentativeConsumerPublishableKey(consumerPublishableKey)
        provider.confirmConsumerPublishableKey()

        provider.assertIsProvidingConsumerPublishableKey()
    }

    @Test
    fun `Provides consumer publishable key if SavedStateHandle includes it`() = runTest {
        val provider = RealApiRequestOptionsProvider(
            originalRequestOptions = originalRequestOptions,
            savedStateHandle = SavedStateHandle(
                initialState = mapOf(
                    KeyConsumerPublishableKey to consumerPublishableKey,
                ),
            ),
            isLinkWithStripe = { true },
        )

        provider.assertIsProvidingConsumerPublishableKey()
    }

    private fun ApiRequestOptionsProvider.assertIsProvidingOriginalPublishableKey() {
        val key = provideApiRequestOptions().apiKey
        assertThat(key).isEqualTo(originalRequestOptions.apiKey)
    }

    private fun ApiRequestOptionsProvider.assertIsProvidingConsumerPublishableKey() {
        val key = provideApiRequestOptions().apiKey
        assertThat(key).isEqualTo(consumerPublishableKey)
    }
}
