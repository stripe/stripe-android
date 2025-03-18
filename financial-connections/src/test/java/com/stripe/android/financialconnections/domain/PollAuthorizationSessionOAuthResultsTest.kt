package com.stripe.android.financialconnections.domain

import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.financialconnections.model.MixedOAuthParams
import com.stripe.android.financialconnections.repository.FinancialConnectionsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import kotlin.test.assertEquals

internal class PollAuthorizationSessionOAuthResultsTest {

    private val repository = mock<FinancialConnectionsRepository>()
    private val pollAuthAccounts = PollAuthorizationSessionOAuthResults(
        repository = repository,
        configuration = FinancialConnectionsSheetConfiguration(
            ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
            ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
        )
    )

    @Test
    fun `invoke - when repository returns 202 and then accounts, returns accounts`() = runTest {
        given(repository.postAuthorizationSessionOAuthResults(any(), any()))
            .willAnswer { throw acceptedRequest() }
            .willAnswer { throw acceptedRequest() }
            .willReturn(params())

        val accounts = pollAuthAccounts(ApiKeyFixtures.authorizationSession())

        assertEquals("State", accounts.state)
    }

    @Test
    fun `invoke - when repository returns exception and then accounts, returns accounts`() =
        runTest {
            val invalidRequest = invalidRequest()
            given(repository.postAuthorizationSessionOAuthResults(any(), any()))
                .willAnswer { throw invalidRequest }
                .willReturn(params())

            val accounts =
                kotlin.runCatching { pollAuthAccounts(ApiKeyFixtures.authorizationSession()) }

            assertEquals(invalidRequest, accounts.exceptionOrNull())
        }

    private fun params() = MixedOAuthParams(
        state = "State",
        code = null,
        status = null,
        publicToken = "memberGuid"
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
