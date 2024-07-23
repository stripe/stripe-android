package com.stripe.android.financialconnections.repository

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
import com.stripe.android.financialconnections.ApiKeyFixtures.institutionResponse
import com.stripe.android.financialconnections.ApiKeyFixtures.networkedAccountsList
import com.stripe.android.financialconnections.ApiKeyFixtures.partnerAccount
import com.stripe.android.financialconnections.ApiKeyFixtures.partnerAccountList
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.domain.CachedPartnerAccount
import com.stripe.android.financialconnections.model.PartnerAccountsList
import com.stripe.android.financialconnections.network.FinancialConnectionsRequestExecutor
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.KSerializer
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

internal class FinancialConnectionsAccountsRepositoryImplTest {

    private val mockRequestExecutor = mock<FinancialConnectionsRequestExecutor>()
    private val apiRequestFactory = ApiRequest.Factory()
    private val configuration = FinancialConnectionsSheet.Configuration(
        ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
        DEFAULT_PUBLISHABLE_KEY
    )
    private val authSessionId = "AuthSessionId"

    private fun buildRepository(
        consumerPublishableKeyProvider: ConsumerPublishableKeyProvider = ConsumerPublishableKeyProvider { null },
    ) = FinancialConnectionsAccountsRepository(
        apiOptions = ApiRequest.Options(
            apiKey = DEFAULT_PUBLISHABLE_KEY
        ),
        requestExecutor = mockRequestExecutor,
        apiRequestFactory = apiRequestFactory,
        consumerPublishableKeyProvider = consumerPublishableKeyProvider,
        logger = Logger.noop(),
        savedStateHandle = SavedStateHandle(),
    )

    @Test
    fun `getCachedAccounts - When called twice, second call returns from cache`() = runTest {
        val repository = buildRepository()
        val expectedAccounts = partnerAccountList().copy(
            data = listOf(partnerAccount().copy(id = "id_1", linkedAccountId = "linked_id_1"))
        )
        mockPartnerAccountsList(expectedAccounts)

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

    @Test
    fun `getNetworkedAccounts - uses consumer publishable key if available`() = runTest {
        assertUsesConsumerPublishableKey {
            mockNetworkedAccountsList()
            getNetworkedAccounts(
                clientSecret = "client_secret",
                consumerSessionClientSecret = "consumer_session_client_secret",
            )
        }
    }

    @Test
    fun `getNetworkedAccounts - uses merchant publishable key if no consumer publishable key`() = runTest {
        assertUsesMerchantPublishableKey {
            mockNetworkedAccountsList()

            getNetworkedAccounts(
                clientSecret = "client_secret",
                consumerSessionClientSecret = "consumer_session_client_secret",
            )
        }
    }

    @Test
    fun `postShareNetworkedAccounts - uses consumer publishable key if available`() = runTest {
        assertUsesConsumerPublishableKey {
            mockInstitutionResponse()

            postShareNetworkedAccounts(
                clientSecret = "client_secret",
                consumerSessionClientSecret = "consumer_session_client_secret",
                selectedAccountIds = setOf("account_id_1"),
            )
        }
    }

    @Test
    fun `postShareNetworkedAccounts - uses merchant publishable key if no consumer publishable key`() = runTest {
        assertUsesMerchantPublishableKey {
            mockInstitutionResponse()

            postShareNetworkedAccounts(
                clientSecret = "client_secret",
                consumerSessionClientSecret = "consumer_session_client_secret",
                selectedAccountIds = setOf("account_id_1"),
            )
        }
    }

    private suspend fun assertUsesConsumerPublishableKey(
        operation: suspend FinancialConnectionsAccountsRepository.() -> Unit,
    ) {
        val consumerPublishableKey = "pk_123_consumer"

        val repository = buildRepository(
            consumerPublishableKeyProvider = { consumerPublishableKey },
        )

        repository.operation()

        val requestArgumentCaptor = argumentCaptor<ApiRequest>()

        verify(mockRequestExecutor).execute(
            request = requestArgumentCaptor.capture(),
            responseSerializer = any<KSerializer<*>>(),
        )

        assertThat(requestArgumentCaptor.firstValue.options.apiKey).isEqualTo(consumerPublishableKey)
    }

    private suspend fun assertUsesMerchantPublishableKey(
        operation: suspend FinancialConnectionsAccountsRepository.() -> Unit,
    ) {
        val repository = buildRepository(
            consumerPublishableKeyProvider = { null },
        )

        repository.operation()

        val requestArgumentCaptor = argumentCaptor<ApiRequest>()

        verify(mockRequestExecutor).execute(
            request = requestArgumentCaptor.capture(),
            responseSerializer = any<KSerializer<*>>(),
        )

        assertThat(requestArgumentCaptor.firstValue.options.apiKey).isEqualTo(DEFAULT_PUBLISHABLE_KEY)
    }

    private suspend fun mockPartnerAccountsList(partnerAccountList: PartnerAccountsList) {
        given(
            mockRequestExecutor.execute(
                any(),
                any<KSerializer<*>>()
            )
        ).willReturn(partnerAccountList)
    }

    private suspend fun mockNetworkedAccountsList() {
        whenever(mockRequestExecutor.execute(any(), any<KSerializer<*>>())).thenReturn(
            networkedAccountsList("account_id_1", "account_id_2")
        )
    }

    private suspend fun mockInstitutionResponse() {
        whenever(mockRequestExecutor.execute(any(), any<KSerializer<*>>())).thenReturn(
            institutionResponse()
        )
    }
}
