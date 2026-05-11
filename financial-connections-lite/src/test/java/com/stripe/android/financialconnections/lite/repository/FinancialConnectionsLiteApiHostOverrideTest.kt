package com.stripe.android.financialconnections.lite.repository

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.ApiRequest
import org.junit.After
import org.junit.Before
import org.junit.Test

internal class FinancialConnectionsLiteApiHostOverrideTest {

    @Before
    @After
    fun resetApiHostOverride() {
        ApiRequest.API_HOST_OVERRIDE = null
    }

    @Test
    fun `synchronizeSessionUrl reflects API_HOST_OVERRIDE changes`() {
        assertUrlIsDynamic { FinancialConnectionsLiteRepositoryImpl.synchronizeSessionUrl }
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
