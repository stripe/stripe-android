package com.stripe.android.financialconnections.domain

import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.NextPane
import com.stripe.android.financialconnections.model.PartnerAccount
import com.stripe.android.financialconnections.model.PartnerAccountsList
import com.stripe.android.financialconnections.repository.FinancialConnectionsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import kotlin.test.assertEquals

internal class PollAuthAccountsTest {

    private val repository = mock<FinancialConnectionsRepository>()
    private val pollAuthAccounts = PollAuthAccounts(
        repository = repository,
        configuration = FinancialConnectionsSheet.Configuration(
            ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
            ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
        )
    )

    @Test
    fun `invoke - when repository returns 202 and then accounts, returns accounts`() = runTest {
        given(repository.getAuthorizationSessionAccounts(any(), any()))
            .willAnswer { throw acceptedRequest() }
            .willAnswer { throw acceptedRequest() }
            .willReturn(accounts())

        val accounts = pollAuthAccounts(ApiKeyFixtures.authorizationSession())

        assertEquals(1, accounts.count)
    }

    @Test
    fun `invoke - when repository returns exception and then accounts, returns accounts`() = runTest {
        val invalidRequest = invalidRequest()
        given(repository.getAuthorizationSessionAccounts(any(), any()))
            .willAnswer { throw invalidRequest }
            .willReturn(accounts())

        val accounts = kotlin.runCatching { pollAuthAccounts(ApiKeyFixtures.authorizationSession()) }

        assertEquals(invalidRequest, accounts.exceptionOrNull())
    }

    private fun accounts() = PartnerAccountsList(
        listOf(
            PartnerAccount(
                id = "1234",
                authorization = "la_1KMEuEClCIKljWvsfeLEm28K",
                institutionName = "My Bank",
                category = FinancialConnectionsAccount.Category.CREDIT,
                status = FinancialConnectionsAccount.Status.ACTIVE,
                subcategory = FinancialConnectionsAccount.Subcategory.CREDIT_CARD,
                supportedPaymentMethodTypes = emptyList(),
                name = "name"
            )
        ),
        hasMore = false,
        nextPane = NextPane.ACCOUNT_PICKER,
        url = "",
        count = 1
    )

    private fun acceptedRequest(): InvalidRequestException {
        return InvalidRequestException(
            stripeError = null,
            requestId = "req_123",
            statusCode = 202,
            cause = Exception()
        )
    }

    private fun invalidRequest(): InvalidRequestException {
        return InvalidRequestException(
            stripeError = null,
            requestId = "req_123",
            statusCode = 400,
            cause = Exception()
        )
    }
}
