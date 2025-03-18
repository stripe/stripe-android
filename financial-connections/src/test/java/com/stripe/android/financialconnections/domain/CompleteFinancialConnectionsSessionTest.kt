package com.stripe.android.financialconnections.domain

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message.Complete.EarlyTerminationCause.USER_INITIATED_WITH_CUSTOM_MANUAL_ENTRY
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsAccountFixtures
import com.stripe.android.financialconnections.model.FinancialConnectionsAccountList
import com.stripe.android.financialconnections.networking.FakeFinancialConnectionsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class CompleteFinancialConnectionsSessionTest {

    @Test
    fun `Returns completed status for successful completion`() = runTest {
        val completeSession = buildCompleteSessionUseCase()
        val result = completeSession(
            earlyTerminationCause = null,
            closeAuthFlowError = null,
        )
        assertThat(result.status).isEqualTo("completed")
    }

    @Test
    fun `Returns correct status for early termination`() = runTest {
        val completeSession = buildCompleteSessionUseCase()
        val result = completeSession(
            earlyTerminationCause = USER_INITIATED_WITH_CUSTOM_MANUAL_ENTRY,
            closeAuthFlowError = null,
        )
        assertThat(result.status).isEqualTo("custom_manual_entry")
    }

    @Test
    fun `Returns correct status for canceled sessions`() = runTest {
        val completeSession = buildCompleteSessionUseCase(accounts = emptyList())
        val result = completeSession(
            earlyTerminationCause = null,
            closeAuthFlowError = null,
        )
        assertThat(result.status).isEqualTo("canceled")
    }

    @Test
    fun `Returns correct status for failed sessions`() = runTest {
        val completeSession = buildCompleteSessionUseCase(accounts = emptyList())
        val result = completeSession(
            earlyTerminationCause = null,
            closeAuthFlowError = APIConnectionException(),
        )
        assertThat(result.status).isEqualTo("failed")
    }

    @Test
    fun `Prioritizes early termination status over failure status`() = runTest {
        val completeSession = buildCompleteSessionUseCase()
        val result = completeSession(
            earlyTerminationCause = USER_INITIATED_WITH_CUSTOM_MANUAL_ENTRY,
            closeAuthFlowError = APIConnectionException(),
        )
        assertThat(result.status).isEqualTo("custom_manual_entry")
    }

    private fun buildCompleteSessionUseCase(
        accounts: List<FinancialConnectionsAccount> = listOf(
            FinancialConnectionsAccountFixtures.CHECKING_ACCOUNT,
        ),
    ): CompleteFinancialConnectionsSession {
        val session = ApiKeyFixtures.financialConnectionsSessionNoAccounts().copy(
            accountsNew = FinancialConnectionsAccountList(
                data = accounts,
                hasMore = false,
                url = "https://url.com",
            )
        )

        val repository = FakeFinancialConnectionsRepository().also {
            it.postCompleteFinancialConnectionsSessionsResultProvider = { session }
        }

        val fetchPaginatedAccountsForSession = FetchPaginatedAccountsForSession(repository)

        val configuration = FinancialConnectionsSheetConfiguration(
            ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
            ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
        )

        return CompleteFinancialConnectionsSession(
            repository = repository,
            fetchPaginatedAccountsForSession = fetchPaginatedAccountsForSession,
            configuration = configuration,
        )
    }
}
