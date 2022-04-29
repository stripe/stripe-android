package com.stripe.android.financialconnections.repository

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeResponse
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.model.BankAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.financialconnections.test.readResourceAsString
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.net.HttpURLConnection

@ExperimentalCoroutinesApi
class FinancialConnectionsApiRepositoryTest {

    private val mockStripeNetworkClient = mock<StripeNetworkClient>()
    private val apiRequestFactory = mock<ApiRequest.Factory>()

    private val financialConnectionsApiRepository = FinancialConnectionsApiRepository(
        publishableKey = ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY,
        stripeNetworkClient = mockStripeNetworkClient,
        apiRequestFactory = apiRequestFactory
    )

    @Test
    fun `getFinancialConnectionsSession - accounts under linked_accounts json key`() =
        runTest {
            givenGetRequestReturns(
                readResourceAsString(
                    "json/linked_account_session_old_accounts_key.json"
                )
            )

            val result = financialConnectionsApiRepository.getFinancialConnectionsSession("client_secret")

            assertThat(result.accounts.financialConnectionsAccounts.size).isEqualTo(1)
        }

    @Test
    fun `getFinancialConnectionsSession - accounts under account json key`() =
        runTest {
            givenGetRequestReturns(
                readResourceAsString(
                    "json/linked_account_session_new_accounts_key.json"
                )
            )

            val result = financialConnectionsApiRepository.getFinancialConnectionsSession("client_secret")

            assertThat(result.accounts.financialConnectionsAccounts.size).isEqualTo(1)
        }

    @Test
    fun `getFinancialConnectionsSession - accounts under accounts json key`() =
        runTest {
            givenGetRequestReturns(
                readResourceAsString(
                    "json/linked_account_session_payment_account_as_linked_account.json"
                )
            )

            val result = financialConnectionsApiRepository.getFinancialConnectionsSession("client_secret")

            assertThat(result.paymentAccount).isInstanceOf(FinancialConnectionsAccount::class.java)
        }

    @Test
    fun `getFinancialConnectionsSession - paymentAccount is FinancialConnectionsAccount`() =
        runTest {
            givenGetRequestReturns(
                readResourceAsString(
                    "json/linked_account_session_payment_account_as_linked_account.json"
                )
            )

            val result = financialConnectionsApiRepository.getFinancialConnectionsSession("client_secret")

            assertThat(result.paymentAccount).isInstanceOf(FinancialConnectionsAccount::class.java)
        }

    @Test
    fun `getFinancialConnectionsSession - paymentAccount is BankAccount`() =
        runTest {
            givenGetRequestReturns(
                readResourceAsString(
                    "json/linked_account_session_payment_account_as_bank_account.json"
                )
            )

            val result = financialConnectionsApiRepository.getFinancialConnectionsSession("client_secret")

            assertThat(result.paymentAccount).isInstanceOf(BankAccount::class.java)
        }

    private suspend fun givenGetRequestReturns(successBody: String) {
        val mock = mock<ApiRequest>()
        whenever(apiRequestFactory.createGet(any(), any(), any())).thenReturn(mock)
        whenever(mockStripeNetworkClient.executeRequest(any())).thenReturn(
            StripeResponse(
                code = HttpURLConnection.HTTP_OK,
                body = successBody
            )
        )
    }
}
