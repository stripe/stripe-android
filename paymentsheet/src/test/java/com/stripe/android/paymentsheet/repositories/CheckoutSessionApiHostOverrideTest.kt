package com.stripe.android.paymentsheet.repositories

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.ApiRequest
import org.junit.After
import org.junit.Test

internal class CheckoutSessionApiHostOverrideTest {

    @After
    fun tearDown() {
        ApiRequest.API_HOST_OVERRIDE = null
    }

    @Test
    fun `initUrl reflects API_HOST_OVERRIDE changes`() {
        assertUrlIsDynamic { CheckoutSessionRepository.initUrl("cs_123") }
    }

    @Test
    fun `confirmUrl reflects API_HOST_OVERRIDE changes`() {
        assertUrlIsDynamic { CheckoutSessionRepository.confirmUrl("cs_123") }
    }

    @Test
    fun `updateUrl reflects API_HOST_OVERRIDE changes`() {
        assertUrlIsDynamic { CheckoutSessionRepository.updateUrl("cs_123") }
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
