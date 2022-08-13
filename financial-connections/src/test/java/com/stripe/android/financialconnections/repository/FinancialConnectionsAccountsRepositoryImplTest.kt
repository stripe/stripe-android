package com.stripe.android.financialconnections.repository

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.model.PartnerAccountsList
import com.stripe.android.financialconnections.network.FinancialConnectionsRequestExecutor
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.KSerializer
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

internal class FinancialConnectionsAccountsRepositoryImplTest {

    private val mockRequestExecutor = mock<FinancialConnectionsRequestExecutor>()
    private val apiRequestFactory = mock<ApiRequest.Factory>()
    private val configuration = FinancialConnectionsSheet.Configuration(
        ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
        ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
    )
    private val authSessionId = "AuthSessionId"

    private fun buildRepository() = FinancialConnectionsAccountsRepository(
        publishableKey = ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY,
        requestExecutor = mockRequestExecutor,
        apiRequestFactory = apiRequestFactory,
        logger = Logger.noop(),
    )

    @Test
    fun `getOrFetchAccounts - When called twice, second call returns from cache`() =
        runTest {
            val repository = buildRepository()
            val expectedAccounts = ApiKeyFixtures.partnerAccountList()
            givenRequestReturnsAccounts(expectedAccounts)

            // first call calls backend and caches.
            repository.getOrFetchAccounts(
                configuration.financialConnectionsSessionClientSecret,
                authSessionId
            )
            // second call reads from cache
            val accounts = repository.getOrFetchAccounts(
                configuration.financialConnectionsSessionClientSecret,
                authSessionId
            )
            verify(
                mockRequestExecutor, times(1)
            ).execute(
                any(),
                eq(PartnerAccountsList.serializer())
            )
            assertThat(accounts).isEqualTo(expectedAccounts)
        }

    /**
     * Simulates an API call to retrieve manifest that takes some time.
     */
    private suspend fun givenRequestReturnsAccounts(
        partnerAccountList: PartnerAccountsList
    ) {
        val mock = mock<ApiRequest>()
        whenever(apiRequestFactory.createPost(any(), any(), any())).thenReturn(mock)
        given(
            mockRequestExecutor.execute(
                any(),
                any<KSerializer<*>>()
            )
        ).willReturn(partnerAccountList)
    }
}
