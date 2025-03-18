package com.stripe.android.financialconnections.repository

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.ApiKeyFixtures.partnerAccount
import com.stripe.android.financialconnections.ApiKeyFixtures.partnerAccountList
import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.financialconnections.domain.CachedPartnerAccount
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
    private val configuration = FinancialConnectionsSheetConfiguration(
        ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
        ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
    )
    private val authSessionId = "AuthSessionId"

    private fun buildRepository() = FinancialConnectionsAccountsRepository(
        requestExecutor = mockRequestExecutor,
        provideApiRequestOptions = {
            ApiRequest.Options(
                apiKey = ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
            )
        },
        apiRequestFactory = apiRequestFactory,
        logger = Logger.noop(),
        savedStateHandle = SavedStateHandle(),
    )

    @Test
    fun `getCachedAccounts - When called twice, second call returns from cache`() = runTest {
        val repository = buildRepository()
        val expectedAccounts = partnerAccountList().copy(
            data = listOf(partnerAccount().copy(id = "id_1", linkedAccountId = "linked_id_1"))
        )
        givenRequestReturnsAccounts(expectedAccounts)

        // first call calls backend and caches.
        repository.postAuthorizationSessionAccounts(
            configuration.financialConnectionsSessionClientSecret,
            authSessionId
        )
        // second call reads from cache
        val accounts = repository.getCachedAccounts()
        verify(
            mockRequestExecutor,
            times(1)
        ).execute(
            any(),
            eq(PartnerAccountsList.serializer())
        )
        assertThat(accounts).containsExactly(
            CachedPartnerAccount(id = "id_1", linkedAccountId = "linked_id_1")
        )
    }

    /**
     * Simulates an API call to retrieve manifest that takes some time.
     */
    private suspend fun givenRequestReturnsAccounts(
        partnerAccountList: PartnerAccountsList
    ) {
        val mock = mock<ApiRequest>()
        whenever(
            apiRequestFactory.createPost(
                url = any(),
                options = any(),
                params = any(),
                shouldCache = eq(false)
            )
        ).thenReturn(mock)
        given(
            mockRequestExecutor.execute(
                any(),
                any<KSerializer<*>>()
            )
        ).willReturn(partnerAccountList)
    }
}
