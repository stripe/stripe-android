package com.stripe.android.financialconnections.domain

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.exception.APIException
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.financialConnectionsSessionWithMoreAccounts
import com.stripe.android.financialconnections.financialConnectionsSessionWithNoMoreAccounts
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.financialconnections.model.LinkedAccountList
import com.stripe.android.financialconnections.moreLinkedAccountList
import com.stripe.android.financialconnections.networking.FakeFinancialConnectionsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertFailsWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class FetchFinancialConnectionsSessionTest {

    private val repository = FakeFinancialConnectionsRepository(ApiKeyFixtures.MANIFEST)
    private val getLinkAccountSession = FetchLinkAccountSession(repository)

    @Test
    fun `invoke - when session with no more accounts, should return unmodified session`() =
        runTest {
            // Given
            val clientSecret = "clientSecret"
            repository.getFinancialConnectionsSessionResultProvider =
                { financialConnectionsSessionWithNoMoreAccounts }

            // When
            val result: FinancialConnectionsSession = getLinkAccountSession(clientSecret)

            // Then
            assertThat(result).isEqualTo(financialConnectionsSessionWithNoMoreAccounts)
        }

    @Test
    fun `invoke - when session with more accounts, should return combined linked account list`() =
        runTest {
            // Given
            val clientSecret = "clientSecret"
            repository.getFinancialConnectionsSessionResultProvider = { financialConnectionsSessionWithMoreAccounts }
            repository.getLinkedAccountsResultProvider = { moreLinkedAccountList }

            // When
            val result: FinancialConnectionsSession = getLinkAccountSession(clientSecret)

            // Then
            val combinedAccounts = listOf(
                // Original account list with hasMore == true
                financialConnectionsSessionWithMoreAccounts.linkedAccounts.linkedAccounts,
                // Next and last linked accounts page.
                moreLinkedAccountList.linkedAccounts
            ).flatten()

            assertThat(result).isEqualTo(
                financialConnectionsSessionWithMoreAccounts.copy(
                    linkedAccounts = LinkedAccountList(
                        linkedAccounts = combinedAccounts,
                        hasMore = false,
                        count = combinedAccounts.size,
                        totalCount = combinedAccounts.size,
                        url = moreLinkedAccountList.url
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
            repository.getLinkedAccountsResultProvider = { throw APIException() }

            // When
            val result: FinancialConnectionsSession = getLinkAccountSession(clientSecret)

            // Then
            val combinedAccounts = listOf(
                // Original account list with hasMore == true
                financialConnectionsSessionWithMoreAccounts.linkedAccounts.linkedAccounts,
                // Next and last linked accounts page.
                moreLinkedAccountList.linkedAccounts
            ).flatten()

            assertThat(result).isEqualTo(
                financialConnectionsSessionWithMoreAccounts.copy(
                    linkedAccounts = LinkedAccountList(
                        linkedAccounts = combinedAccounts,
                        hasMore = false,
                        totalCount = combinedAccounts.size,
                        url = "url"
                    )
                )
            )
        }
    }
}
