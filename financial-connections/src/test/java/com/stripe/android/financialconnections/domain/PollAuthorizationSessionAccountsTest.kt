package com.stripe.android.financialconnections.domain

import com.stripe.android.core.exception.APIException
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.ApiKeyFixtures.authorizationSession
import com.stripe.android.financialconnections.ApiKeyFixtures.institution
import com.stripe.android.financialconnections.ApiKeyFixtures.partnerAccount
import com.stripe.android.financialconnections.ApiKeyFixtures.sessionManifest
import com.stripe.android.financialconnections.ApiKeyFixtures.syncResponse
import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.financialconnections.exception.AccountLoadError
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.PartnerAccountsList
import com.stripe.android.financialconnections.repository.FinancialConnectionsAccountsRepository
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
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
    private val configuration = FinancialConnectionsSheetConfiguration(
        ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
        ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
    )
    private val pollAuthorizationSessionAccounts =
        PollAuthorizationSessionAccounts(repository, configuration)

    @Test
    fun `test successful account polling`() = runTest {
        val sync = syncResponse(
            sessionManifest().copy(
                activeAuthSession = authorizationSession(),
                activeInstitution = institution()
            )
        )
        val accountsList = PartnerAccountsList(
            data = listOf(
                partnerAccount()
            ),
            nextPane = FinancialConnectionsSessionManifest.Pane.CONSENT,
        )

        whenever(repository.postAuthorizationSessionAccounts(any(), any()))
            .doReturn(accountsList)

        val result = pollAuthorizationSessionAccounts.invoke(true, sync)

        assertEquals(accountsList, result)
        verify(repository).postAuthorizationSessionAccounts(
            configuration.financialConnectionsSessionClientSecret,
            sync.manifest.activeAuthSession!!.id
        )
    }

    @Test
    fun `test reached too many failed account polling`() = runTest {
        val sync = syncResponse(
            sessionManifest().copy(
                activeAuthSession = authorizationSession(),
                activeInstitution = institution()
            )
        )

        whenever(repository.postAuthorizationSessionAccounts(any(), any()))
            .thenAnswer { throw retryException() }

        val exception: Throwable? = runCatching {
            pollAuthorizationSessionAccounts.invoke(true, sync)
        }.exceptionOrNull()

        assertIs<AccountLoadError>(exception)

        // Retries 180 times
        verify(repository, times(180)).postAuthorizationSessionAccounts(
            configuration.financialConnectionsSessionClientSecret,
            sync.manifest.activeAuthSession!!.id
        )
    }

    @Test
    fun `test empty account list retrieved`() = runTest {
        val sync = syncResponse(
            sessionManifest().copy(
                activeAuthSession = authorizationSession(),
                activeInstitution = institution()
            )
        )

        val emptyList = PartnerAccountsList(
            data = emptyList(),
            nextPane = FinancialConnectionsSessionManifest.Pane.CONSENT,
        )

        whenever(repository.postAuthorizationSessionAccounts(any(), any())).doReturn(emptyList)

        val exception: Throwable? = runCatching {
            pollAuthorizationSessionAccounts.invoke(true, sync)
        }.exceptionOrNull()

        assertIs<AccountLoadError>(exception)

        verify(repository, times(1)).postAuthorizationSessionAccounts(
            configuration.financialConnectionsSessionClientSecret,
            sync.manifest.activeAuthSession!!.id
        )
    }
}

private fun retryException() = APIException(statusCode = HttpURLConnection.HTTP_ACCEPTED)
