package com.stripe.android.financialconnections.domain

import com.stripe.android.core.exception.APIException
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.ApiKeyFixtures.authorizationSession
import com.stripe.android.financialconnections.ApiKeyFixtures.institution
import com.stripe.android.financialconnections.ApiKeyFixtures.partnerAccount
import com.stripe.android.financialconnections.ApiKeyFixtures.sessionManifest
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.exception.AccountLoadError
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.PartnerAccountsList
import com.stripe.android.financialconnections.repository.FinancialConnectionsAccountsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.net.HttpURLConnection
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
internal class PollAuthorizationSessionAccountsTest {

    private val repository: FinancialConnectionsAccountsRepository = mock()
    private val configuration = FinancialConnectionsSheet.Configuration(
        ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
        ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
    )
    private val pollAuthorizationSessionAccounts =
        PollAuthorizationSessionAccounts(repository, configuration)

    @Test
    fun `test successful account polling`() = runTest {
        val manifest: FinancialConnectionsSessionManifest = sessionManifest().copy(
            activeAuthSession = authorizationSession(),
            activeInstitution = institution()
        )
        val accountsList = PartnerAccountsList(
            data = listOf(
                partnerAccount()
            ),
            hasMore = false,
            nextPane = FinancialConnectionsSessionManifest.Pane.CONSENT,
            url = ""
        )

        whenever(repository.postAuthorizationSessionAccounts(any(), any()))
            .doReturn(accountsList)

        val result = pollAuthorizationSessionAccounts.invoke(true, manifest)

        assertEquals(accountsList, result)
        verify(repository).postAuthorizationSessionAccounts(
            configuration.financialConnectionsSessionClientSecret,
            manifest.activeAuthSession!!.id
        )
    }

    @Test
    fun `test reached too many failed account polling`() = runTest {
        val manifest: FinancialConnectionsSessionManifest = sessionManifest().copy(
            activeAuthSession = authorizationSession(),
            activeInstitution = institution()
        )

        whenever(repository.postAuthorizationSessionAccounts(any(), any()))
            .thenAnswer { throw retryException() }

        val exception: Throwable? = runCatching {
            pollAuthorizationSessionAccounts.invoke(true, manifest)
        }.exceptionOrNull()

        assertIs<AccountLoadError>(exception)

        // Retries 180 times
        verify(repository, times(180)).postAuthorizationSessionAccounts(
            configuration.financialConnectionsSessionClientSecret,
            manifest.activeAuthSession!!.id
        )
    }

    @Test
    fun `test empty account list retrieved`() = runTest {
        val manifest: FinancialConnectionsSessionManifest = sessionManifest().copy(
            activeAuthSession = authorizationSession(),
            activeInstitution = institution()
        )

        val emptyList = PartnerAccountsList(
            data = emptyList(),
            hasMore = false,
            nextPane = FinancialConnectionsSessionManifest.Pane.CONSENT,
            url = ""
        )

        whenever(repository.postAuthorizationSessionAccounts(any(), any())).doReturn(emptyList)

        val exception: Throwable? = runCatching {
            pollAuthorizationSessionAccounts.invoke(true, manifest)
        }.exceptionOrNull()

        assertIs<AccountLoadError>(exception)

        verify(repository, times(1)).postAuthorizationSessionAccounts(
            configuration.financialConnectionsSessionClientSecret,
            manifest.activeAuthSession!!.id
        )
    }
}

private fun retryException() = APIException(statusCode = HttpURLConnection.HTTP_ACCEPTED)