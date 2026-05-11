package com.stripe.android.repository

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.ApiRequest
import org.junit.After
import org.junit.Before
import org.junit.Test

internal class ConsumersApiServiceApiHostOverrideTest {

    @Before
    @After
    fun resetApiHostOverride() {
        ApiRequest.API_HOST_OVERRIDE = null
    }

    @Test
    fun `consumerAccountsSignUpUrl reflects API_HOST_OVERRIDE changes`() {
        assertUrlIsDynamic { ConsumersApiServiceImpl.consumerAccountsSignUpUrl }
    }

    @Test
    fun `consumerSessionLookupUrl reflects API_HOST_OVERRIDE changes`() {
        assertUrlIsDynamic { ConsumersApiServiceImpl.consumerSessionLookupUrl }
    }

    @Test
    fun `startConsumerVerificationUrl reflects API_HOST_OVERRIDE changes`() {
        assertUrlIsDynamic { ConsumersApiServiceImpl.startConsumerVerificationUrl }
    }

    @Test
    fun `consentUpdateUrl reflects API_HOST_OVERRIDE changes`() {
        assertUrlIsDynamic { ConsumersApiServiceImpl.consentUpdateUrl }
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
