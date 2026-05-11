package com.stripe.android.crypto.onramp.repositories

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.ApiRequest
import org.junit.After
import org.junit.Test

internal class CryptoApiRepositoryApiHostOverrideTest {

    @After
    fun tearDown() {
        ApiRequest.API_HOST_OVERRIDE = null
    }

    @Test
    fun `customersUrl reflects API_HOST_OVERRIDE changes`() {
        assertUrlIsDynamic { CryptoApiRepository.customersUrl }
    }

    @Test
    fun `getOnrampSessionUrl reflects API_HOST_OVERRIDE changes`() {
        assertUrlIsDynamic { CryptoApiRepository.getOnrampSessionUrl }
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
