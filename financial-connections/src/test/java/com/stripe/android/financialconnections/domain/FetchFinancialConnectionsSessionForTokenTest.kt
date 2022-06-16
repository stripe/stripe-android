package com.stripe.android.financialconnections.domain

import com.google.common.truth.Truth.assertThat
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.financialconnections.networking.FakeFinancialConnectionsRepository
import com.stripe.android.financialconnections.test.readResourceAsString
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertFailsWith

@ExperimentalCoroutinesApi
class FetchFinancialConnectionsSessionForTokenTest {

    private val repository = FakeFinancialConnectionsRepository(ApiKeyFixtures.sessionManifest())
    private val fetchFinancialConnectionsSessionForToken = FetchFinancialConnectionsSessionForToken(repository)
    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun `invoke - when session includes a token, it's correctly deserialized from string`() =
        runTest {
            // Given
            val clientSecret = "clientSecret"
            repository.getFinancialConnectionsSessionResultProvider = {
                json.decodeFromString(
                    FinancialConnectionsSession.serializer(),
                    readResourceAsString("json/linked_account_session_with_token.json")
                )
            }

            // When
            val (_, token) = fetchFinancialConnectionsSessionForToken(clientSecret)

            // Then
            assertThat(token.id).isEqualTo("tok_1F4ACMCRMbs6FrXf6fPqLnN7")
        }

    @Test
    fun `invoke - when session does not include a token, usecase fails`() =
        runTest {
            // Given
            val clientSecret = "clientSecret"
            repository.getFinancialConnectionsSessionResultProvider = {
                json.decodeFromString(
                    FinancialConnectionsSession.serializer(),
                    readResourceAsString("json/linked_account_session_payment_account_as_bank_account.json")
                )
            }

            // Then
            assertFailsWith<Exception> {
                fetchFinancialConnectionsSessionForToken(clientSecret)
            }
        }
}
