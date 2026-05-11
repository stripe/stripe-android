package com.stripe.android.financialconnections.repository

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.financialconnections.repository.api.FinancialConnectionsConsumersApiServiceImpl
import org.junit.After
import org.junit.Before
import org.junit.Test

internal class FinancialConnectionsApiHostOverrideTest {

    @Before
    @After
    fun resetApiHostOverride() {
        ApiRequest.API_HOST_OVERRIDE = null
    }

    @Test
    fun `authorizationSessionUrl reflects API_HOST_OVERRIDE changes`() {
        assertUrlIsDynamic { FinancialConnectionsRepositoryImpl.authorizationSessionUrl }
    }

    @Test
    fun `authorizeSessionUrl reflects API_HOST_OVERRIDE changes`() {
        assertUrlIsDynamic { FinancialConnectionsRepositoryImpl.authorizeSessionUrl }
    }

    @Test
    fun `synchronizeSessionUrl reflects API_HOST_OVERRIDE changes`() {
        assertUrlIsDynamic { FinancialConnectionsManifestRepositoryImpl.synchronizeSessionUrl }
    }

    @Test
    fun `cancelAuthSessionUrl reflects API_HOST_OVERRIDE changes`() {
        assertUrlIsDynamic { FinancialConnectionsManifestRepositoryImpl.cancelAuthSessionUrl }
    }

    @Test
    fun `accountsSessionUrl reflects API_HOST_OVERRIDE changes`() {
        assertUrlIsDynamic { FinancialConnectionsAccountsRepositoryImpl.accountsSessionUrl }
    }

    @Test
    fun `networkedAccountsUrl reflects API_HOST_OVERRIDE changes`() {
        assertUrlIsDynamic { FinancialConnectionsAccountsRepositoryImpl.networkedAccountsUrl }
    }

    @Test
    fun `consumerSessionsUrl reflects API_HOST_OVERRIDE changes`() {
        assertUrlIsDynamic { FinancialConnectionsConsumersApiServiceImpl.consumerSessionsUrl }
    }

    @Test
    fun `institutionsUrl reflects API_HOST_OVERRIDE changes`() {
        assertUrlIsDynamic { FinancialConnectionsInstitutionsRepositoryImpl.institutionsUrl }
    }

    @Test
    fun `featuredInstitutionsUrl reflects API_HOST_OVERRIDE changes`() {
        assertUrlIsDynamic { FinancialConnectionsInstitutionsRepositoryImpl.featuredInstitutionsUrl }
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
