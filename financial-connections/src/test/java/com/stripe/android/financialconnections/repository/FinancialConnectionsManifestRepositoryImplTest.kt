package com.stripe.android.financialconnections.repository

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.ApiKeyFixtures.sessionManifest
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.network.FinancialConnectionsRequestExecutor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.KSerializer
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.mockito.kotlin.willSuspendableAnswer

@ExperimentalCoroutinesApi
internal class FinancialConnectionsManifestRepositoryImplTest {

    private val mockRequestExecutor = mock<FinancialConnectionsRequestExecutor>()
    private val apiRequestFactory = mock<ApiRequest.Factory>()
    private val configuration = FinancialConnectionsSheet.Configuration(
        ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
        ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
    )

    private fun buildRepository(
        initialManifest: FinancialConnectionsSessionManifest? = null
    ) = FinancialConnectionsManifestRepository(
        requestExecutor = mockRequestExecutor,
        configuration = configuration,
        apiRequestFactory = apiRequestFactory,
        apiOptions = ApiRequest.Options(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY),
        logger = Logger.noop()
    )

    @Test
    fun `getOrFetchManifest - when manifest retrieved twice concurrently, API call runs once`() =
        runTest {
            givenFetchManifestRequestReturnsAfterDelay(sessionManifest())

            val repository = buildRepository()

            // simulates to concurrent accesses to manifest.
            awaitAll(
                async { repository.getOrFetchManifest() },
                async { repository.getOrFetchManifest() }
            )

            verify(mockRequestExecutor, times(1)).execute(any(), any<KSerializer<*>>())
        }

    @Test
    fun `getOrFetchManifest - when initial manifest passed in constructor, returns it and no network interaction`() =
        runTest {
            val initialManifest = sessionManifest()
            val repository = buildRepository(initialManifest = initialManifest)

            val returnedManifest = repository.getOrFetchManifest()

            assertThat(returnedManifest).isEqualTo(initialManifest)
            verifyNoInteractions(mockRequestExecutor)
        }

    /**
     * Simulates an API call to retrieve manifest that takes some time.
     */
    private suspend fun givenFetchManifestRequestReturnsAfterDelay(
        manifest: FinancialConnectionsSessionManifest
    ) {
        val mock = mock<ApiRequest>()
        whenever(apiRequestFactory.createGet(any(), any(), any())).thenReturn(mock)
        given(mockRequestExecutor.execute(any(), any<KSerializer<*>>())).willSuspendableAnswer {
            delay(100)
            manifest
        }
    }
}
