package com.stripe.android.connections.repository

import com.google.common.truth.Truth.assertThat
import com.stripe.android.connections.ApiKeyFixtures
import com.stripe.android.connections.model.BankAccount
import com.stripe.android.connections.model.LinkedAccount
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.net.HttpURLConnection

@ExperimentalCoroutinesApi
class ConnectionsApiRepositoryTest {

    private val mockStripeNetworkClient = mock<StripeNetworkClient>()
    private val apiRequestFactory = mock<ApiRequest.Factory>()

    private val connectionsApiRepository = ConnectionsApiRepository(
        publishableKey = ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY,
        stripeNetworkClient = mockStripeNetworkClient,
        apiRequestFactory = apiRequestFactory
    )

    @Test
    fun `getLinkAccountSession - when paymentAccount is LinkedAccount, deserializes correct type`() =
        runTest {
            givenGetRequestReturns(LINKED_ACCOUNT_SESSION_WITH_LINKED_ACCOUNT)

            val result = connectionsApiRepository.getLinkAccountSession("client_secret")

            assertThat(result.paymentAccount).isInstanceOf(LinkedAccount::class.java)
        }


    @Test
    fun `getLinkAccountSession - when paymentAccount is BankAccount, deserializes correct type`() =
        runTest {
            givenGetRequestReturns(LINKED_ACCOUNT_SESSION_WITH_BANK_ACCOUNT)

            val result = connectionsApiRepository.getLinkAccountSession("client_secret")

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

    companion object {
        private val LINKED_ACCOUNT_SESSION_WITH_BANK_ACCOUNT = """ 
            {
              "id": "las_dhgfsklhgfkdsjhgk",
              "object": "link_account_session",
              "client_secret": "las_client_secret_ldafjlfkjlsfadkjk",
              "linked_accounts": {
                "object": "list",
                "data": [
            
                ],
                "has_more": false,
                "total_count": 0,
                "url": "/v1/linked_accounts"
              },
              "livemode": true,
              "payment_account": {
                "id": "ba_1Kj6xvClCIKljWvs6z6QBHpN",
                "bank_name": "JPMORGAN CHASE BANK, NA",
                "last4": "3211",
                "routing_number": "12341234"
              }
            }
        """.trimIndent()


        private val LINKED_ACCOUNT_SESSION_WITH_LINKED_ACCOUNT = """ 
            {
              "id": "las_dhgfsklhgfkdsjhgk",
              "object": "link_account_session",
              "client_secret": "las_client_secret_ldafjlfkjlsfadkjk",
              "linked_accounts": {
                "object": "list",
                "data": [
            
                ],
                "has_more": false,
                "total_count": 0,
                "url": "/v1/linked_accounts"
              },
              "livemode": true,
              "payment_account": {
                "id": "ba_1Kj6xvClCIKljWvs6z6QBHpN",
                "created": "233",
                "institution_name": "JPMORGAN CHASE BANK, NA",
                "livemode": true,
                "supported_payment_method_types": ["link"],
                "last4": "3211"
              }
            }
        """.trimIndent()
    }

}