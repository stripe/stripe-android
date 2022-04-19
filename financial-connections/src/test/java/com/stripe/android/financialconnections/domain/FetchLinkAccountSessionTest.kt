package com.stripe.android.financialconnections.domain

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.linkAccountSessionWithMoreAccounts
import com.stripe.android.financialconnections.linkAccountSessionWithNoMoreAccounts
import com.stripe.android.financialconnections.model.LinkAccountSession
import com.stripe.android.financialconnections.model.LinkedAccountList
import com.stripe.android.financialconnections.moreLinkedAccountList
import com.stripe.android.financialconnections.networking.FakeConnectionsRepository
import com.stripe.android.core.exception.APIException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertFailsWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class FetchLinkAccountSessionTest {

    private val repository = FakeConnectionsRepository(ApiKeyFixtures.MANIFEST)
    private val getLinkAccountSession = FetchLinkAccountSession(repository)

    @Test
    fun `invoke - when session with no more accounts, should return unmodified session`() =
        runTest {
            // Given
            val clientSecret = "clientSecret"
            repository.getLinkAccountSessionResultProvider =
                { linkAccountSessionWithNoMoreAccounts }

            // When
            val result: LinkAccountSession = getLinkAccountSession(clientSecret)

            // Then
            assertThat(result).isEqualTo(linkAccountSessionWithNoMoreAccounts)
        }

    @Test
    fun `invoke - when session with more accounts, should return combined linked account list`() =
        runTest {
            // Given
            val clientSecret = "clientSecret"
            repository.getLinkAccountSessionResultProvider = { linkAccountSessionWithMoreAccounts }
            repository.getLinkedAccountsResultProvider = { moreLinkedAccountList }

            // When
            val result: LinkAccountSession = getLinkAccountSession(clientSecret)

            // Then
            val combinedAccounts = listOf(
                // Original account list with hasMore == true
                linkAccountSessionWithMoreAccounts.linkedAccounts.linkedAccounts,
                // Next and last linked accounts page.
                moreLinkedAccountList.linkedAccounts
            ).flatten()

            assertThat(result).isEqualTo(
                linkAccountSessionWithMoreAccounts.copy(
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
            repository.getLinkAccountSessionResultProvider =
                { linkAccountSessionWithMoreAccounts }
            repository.getLinkedAccountsResultProvider = { throw APIException() }

            // When
            val result: LinkAccountSession = getLinkAccountSession(clientSecret)

            // Then
            val combinedAccounts = listOf(
                // Original account list with hasMore == true
                linkAccountSessionWithMoreAccounts.linkedAccounts.linkedAccounts,
                // Next and last linked accounts page.
                moreLinkedAccountList.linkedAccounts
            ).flatten()

            assertThat(result).isEqualTo(
                linkAccountSessionWithMoreAccounts.copy(
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
