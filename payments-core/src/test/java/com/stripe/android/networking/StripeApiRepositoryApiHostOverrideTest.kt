package com.stripe.android.networking

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.ApiRequest
import org.junit.After
import org.junit.Before
import org.junit.Test

internal class StripeApiRepositoryApiHostOverrideTest {

    @Before
    @After
    fun resetApiHostOverride() {
        ApiRequest.API_HOST_OVERRIDE = null
    }

    @Test
    fun `tokensUrl reflects API_HOST_OVERRIDE changes`() {
        assertUrlIsDynamic { StripeApiRepository.tokensUrl }
    }

    @Test
    fun `paymentMethodsUrl reflects API_HOST_OVERRIDE changes`() {
        assertUrlIsDynamic { StripeApiRepository.paymentMethodsUrl }
    }

    @Test
    fun `confirmationTokensUrl reflects API_HOST_OVERRIDE changes`() {
        assertUrlIsDynamic { StripeApiRepository.confirmationTokensUrl }
    }

    @Test
    fun `getRetrievePaymentIntentUrl reflects API_HOST_OVERRIDE changes`() {
        assertUrlIsDynamic { StripeApiRepository.getRetrievePaymentIntentUrl("pi_123") }
    }

    @Test
    fun `getConfirmSetupIntentUrl reflects API_HOST_OVERRIDE changes`() {
        assertUrlIsDynamic { StripeApiRepository.getConfirmSetupIntentUrl("seti_123") }
    }

    @Test
    fun `sourcesUrl reflects API_HOST_OVERRIDE changes`() {
        assertUrlIsDynamic { StripeApiRepository.sourcesUrl }
    }

    @Test
    fun `deferredFinancialConnectionsSessionUrl reflects API_HOST_OVERRIDE changes`() {
        assertUrlIsDynamic { StripeApiRepository.deferredFinancialConnectionsSessionUrl }
    }

    private fun assertUrlIsDynamic(urlProvider: () -> String) {
        ApiRequest.API_HOST_OVERRIDE = HOST_A
        val urlA = urlProvider()

        ApiRequest.API_HOST_OVERRIDE = HOST_B
        val urlB = urlProvider()

        assertThat(urlA).startsWith(HOST_A)
        assertThat(urlB).startsWith(HOST_B)
        assertThat(urlA).isNotEqualTo(urlB)
    }

    private companion object {
        const val HOST_A = "https://host-a.example.com"
        const val HOST_B = "https://host-b.example.com"
    }
}
