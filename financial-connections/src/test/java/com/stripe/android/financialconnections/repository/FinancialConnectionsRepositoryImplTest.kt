package com.stripe.android.financialconnections.repository

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeResponse
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.model.BankAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.financialconnections.network.FinancialConnectionsRequestExecutor
import com.stripe.android.financialconnections.test.readResourceAsString
import com.stripe.android.financialconnections.utils.testJson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.net.HttpURLConnection

@ExperimentalCoroutinesApi
class FinancialConnectionsRepositoryImplTest {

    private val mockStripeNetworkClient = mock<StripeNetworkClient>()
    private val apiRequestFactory = ApiRequest.Factory()

    private fun buildRepository(
        consumerPublishableKeyProvider: ConsumerPublishableKeyProvider = ConsumerPublishableKeyProvider { null },
    ): FinancialConnectionsRepositoryImpl {
        return FinancialConnectionsRepositoryImpl(
            requestExecutor = FinancialConnectionsRequestExecutor(
                json = testJson(),
                eventEmitter = mock(),
                stripeNetworkClient = mockStripeNetworkClient,
                logger = Logger.noop(),
            ),
            apiOptions = ApiRequest.Options(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY),
            apiRequestFactory = apiRequestFactory,
            consumerPublishableKeyProvider = consumerPublishableKeyProvider,
        )
    }

    @Test
    fun `getFinancialConnectionsSession - accounts under linked_accounts json key`() =
        runTest {
            givenGetRequestReturns(
                readResourceAsString(
                    "json/linked_account_session_old_accounts_key.json"
                )
            )

            val repository = buildRepository()
            val result = repository.getFinancialConnectionsSession("client_secret")

            assertThat(result.accounts.data.size).isEqualTo(1)
        }

    @Test
    fun `getFinancialConnectionsSession - accounts under account json key`() =
        runTest {
            givenGetRequestReturns(
                readResourceAsString(
                    "json/linked_account_session_new_accounts_key.json"
                )
            )

            val repository = buildRepository()
            val result = repository.getFinancialConnectionsSession("client_secret")

            assertThat(result.accounts.data.size).isEqualTo(1)
        }

    @Test
    fun `getFinancialConnectionsSession - accounts under accounts json key`() =
        runTest {
            givenGetRequestReturns(
                readResourceAsString(
                    "json/linked_account_session_payment_account_as_linked_account.json"
                )
            )

            val repository = buildRepository()
            val result = repository.getFinancialConnectionsSession("client_secret")

            assertThat(result.paymentAccount).isInstanceOf(FinancialConnectionsAccount::class.java)
        }

    @Test
    fun `getFinancialConnectionsSession - paymentAccount is LinkedAccount`() =
        runTest {
            givenGetRequestReturns(
                readResourceAsString(
                    "json/linked_account_session_payment_account_as_linked_account.json"
                )
            )

            val repository = buildRepository()
            val result = repository.getFinancialConnectionsSession("client_secret")

            assertThat(result.paymentAccount).isInstanceOf(FinancialConnectionsAccount::class.java)
        }

    @Test
    fun `getFinancialConnectionsSession - paymentAccount is FinancialConnectionsAccount`() =
        runTest {
            givenGetRequestReturns(
                readResourceAsString(
                    "json/linked_account_session_payment_account_as_financial_account.json"
                )
            )

            val repository = buildRepository()
            val result = repository.getFinancialConnectionsSession("client_secret")

            assertThat(result.paymentAccount).isInstanceOf(FinancialConnectionsAccount::class.java)
        }

    @Test
    fun `getFinancialConnectionsSession - account with unknown permissions ignores unknown values`() =
        runTest {
            givenGetRequestReturns(
                readResourceAsString(
                    "json/linked_account_session_unknown_permission.json"
                )
            )

            val repository = buildRepository()
            val result = repository.getFinancialConnectionsSession("client_secret")

            val financialConnectionsAccount = result.paymentAccount as FinancialConnectionsAccount
            assertThat(financialConnectionsAccount.permissions).containsExactly(
                FinancialConnectionsAccount.Permissions.PAYMENT_METHOD,
                FinancialConnectionsAccount.Permissions.UNKNOWN
            )
        }

    @Test
    fun `getFinancialConnectionsSession - paymentAccount is BankAccount`() =
        runTest {
            givenGetRequestReturns(
                readResourceAsString(
                    "json/linked_account_session_payment_account_as_bank_account.json"
                )
            )

            val repository = buildRepository()
            val result = repository.getFinancialConnectionsSession("client_secret")

            assertThat(result.paymentAccount).isInstanceOf(BankAccount::class.java)
        }

    @Test
    fun `postCompleteFinancialConnectionsSessions - uses consumer publishable key if present`() = runTest {
        givenGetRequestReturns(
            readResourceAsString("json/linked_account_session_complete.json")
        )

        val consumerPublishableKey = "pk_123_consumer"

        val repository = buildRepository(
            consumerPublishableKeyProvider = { consumerPublishableKey },
        )

        repository.postCompleteFinancialConnectionsSessions(
            clientSecret = "client_secret",
            terminalError = null,
        )

        val apiRequestCaptor = argumentCaptor<ApiRequest>()
        verify(mockStripeNetworkClient).executeRequest(apiRequestCaptor.capture())

        assertThat(apiRequestCaptor.firstValue.options.apiKey).isEqualTo(consumerPublishableKey)
    }

    private suspend fun givenGetRequestReturns(successBody: String) {
        whenever(mockStripeNetworkClient.executeRequest(any())).thenReturn(
            StripeResponse(
                code = HttpURLConnection.HTTP_OK,
                body = successBody
            )
        )
    }
}
