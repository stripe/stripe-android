package com.stripe.android.financialconnections.domain

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.exception.APIException
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.financialConnectionsSessionWithMoreAccounts
import com.stripe.android.financialconnections.financialConnectionsSessionWithNoMoreAccounts
import com.stripe.android.financialconnections.model.FinancialConnectionsAccountList
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.financialconnections.moreFinancialConnectionsAccountList
import com.stripe.android.financialconnections.networking.FakeFinancialConnectionsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertFailsWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class FetchFinancialConnectionsSessionTest {

    private val repository = FakeFinancialConnectionsRepository(ApiKeyFixtures.sessionManifest())
    private val getFinancialConnectionsSession = FetchFinancialConnectionsSession(repository)

    @Test
    fun `invoke - when session with no more accounts, should return unmodified session`() =
        runTest {
            // Given
            val clientSecret = "clientSecret"
            repository.getFinancialConnectionsSessionResultProvider =
                { financialConnectionsSessionWithNoMoreAccounts }

            // When
            val result: FinancialConnectionsSession = getFinancialConnectionsSession(clientSecret)

            // Then
            assertThat(result).isEqualTo(financialConnectionsSessionWithNoMoreAccounts)
        }

    @Test
    fun `invoke - when session with more accounts, should return combined account list`() =
        runTest {
            // Given
            val clientSecret = "clientSecret"
            repository.getFinancialConnectionsSessionResultProvider = { financialConnectionsSessionWithMoreAccounts }
            repository.getAccountsResultProvider = { moreFinancialConnectionsAccountList }

            // When
            val result: FinancialConnectionsSession = getFinancialConnectionsSession(clientSecret)

            // Then
            val combinedAccounts = listOf(
                // Original account list with hasMore == true
                financialConnectionsSessionWithMoreAccounts.accounts.data,
                // Next and last connected accounts page.
                moreFinancialConnectionsAccountList.data
            ).flatten()

            assertThat(result).isEqualTo(
                financialConnectionsSessionWithMoreAccounts.copy(
                    accountsNew = FinancialConnectionsAccountList(
                        data = combinedAccounts,
                        hasMore = false,
                        count = combinedAccounts.size,
                        totalCount = combinedAccounts.size,
                        url = moreFinancialConnectionsAccountList.url
                    )
                )
            )
        }

    @Test
    fun `invoke - when failure fetching more accounts, should return exception`() = runTest {
        // Given
        assertFailsWith<APIException> {
            val clientSecret = "clientSecret"
            repository.getFinancialConnectionsSessionResultProvider =
                { financialConnectionsSessionWithMoreAccounts }
            repository.getAccountsResultProvider = { throw APIException() }

            // When
            val result: FinancialConnectionsSession = getFinancialConnectionsSession(clientSecret)

            // Then
            val combinedAccounts = listOf(
                // Original account list with hasMore == true
                financialConnectionsSessionWithMoreAccounts.accounts.data,
                // Next and last connected accounts page.
                moreFinancialConnectionsAccountList.data
            ).flatten()

            assertThat(result).isEqualTo(
                financialConnectionsSessionWithMoreAccounts.copy(
                    accountsNew = FinancialConnectionsAccountList(
                        data = combinedAccounts,
                        hasMore = false,
                        totalCount = combinedAccounts.size,
                        url = "url"
                    )
                )
            )
        }
    }
}
