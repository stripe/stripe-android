package com.stripe.android.financialconnections.repository

import com.google.common.truth.Truth.assertThat
import com.stripe.android.connections.test.readResourceAsString
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeResponse
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.model.BankAccount
import com.stripe.android.financialconnections.model.LinkedAccount
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

    private val financialConnectionsApiRepository = FinancialFinancialConnectionsApiRepository(
        publishableKey = ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY,
        stripeNetworkClient = mockStripeNetworkClient,
        apiRequestFactory = apiRequestFactory
    )

    @Test
    fun `getLinkAccountSession - when paymentAccount is LinkedAccount, deserializes correct type`() =
        runTest {
            givenGetRequestReturns(
                readResourceAsString(
                    "json/linked_account_session_payment_account_as_linked_account.json"
                )
            )

            val result = financialConnectionsApiRepository.getLinkAccountSession("client_secret")

            assertThat(result.paymentAccount).isInstanceOf(LinkedAccount::class.java)
        }

    @Test
    fun `getLinkAccountSession - when paymentAccount is BankAccount, deserializes correct type`() =
        runTest {
            givenGetRequestReturns(
                readResourceAsString(
                    "json/linked_account_session_payment_account_as_bank_account.json"
                )
            )

            val result = financialConnectionsApiRepository.getLinkAccountSession("client_secret")

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
