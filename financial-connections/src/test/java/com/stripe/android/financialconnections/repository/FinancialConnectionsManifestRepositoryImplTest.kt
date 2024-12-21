package com.stripe.android.financialconnections.repository

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.domain.GetOrFetchSync.RefetchCondition.None
import com.stripe.android.financialconnections.model.SynchronizeSessionResponse
import com.stripe.android.financialconnections.network.FinancialConnectionsRequestExecutor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.KSerializer
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.mockito.kotlin.willSuspendableAnswer
import java.util.Locale

@ExperimentalCoroutinesApi
internal class FinancialConnectionsManifestRepositoryImplTest {

    private val mockRequestExecutor = mock<FinancialConnectionsRequestExecutor>()
    private val apiRequestFactory = mock<ApiRequest.Factory>()

    private fun buildRepository(
        initialSync: SynchronizeSessionResponse? = null
    ) = FinancialConnectionsManifestRepository(
        requestExecutor = mockRequestExecutor,
        apiRequestFactory = apiRequestFactory,
        provideApiRequestOptions = {
            ApiRequest.Options(
                apiKey = ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
            )
        },
        logger = Logger.noop(),
        initialSync = initialSync,
        locale = Locale.US
    )

    @Test
    fun `getOrFetchSession - when manifest retrieved twice concurrently, API call runs once`() =
        runTest {
            givenSyncSessionRequestReturnsAfterDelay(ApiKeyFixtures.syncResponse())

            val repository = buildRepository()

            // simulates to concurrent accesses to manifest.
            awaitAll(
                async {
                    repository.getOrSynchronizeFinancialConnectionsSession(
                        clientSecret = "",
                        applicationId = "",
                        attestationInitialized = false,
                        reFetchCondition = None::shouldReFetch
                    )
                },
                async {
                    repository.getOrSynchronizeFinancialConnectionsSession(
                        clientSecret = "",
                        applicationId = "",
                        attestationInitialized = false,
                        reFetchCondition = None::shouldReFetch
                    )
                }
            )

            verify(mockRequestExecutor, times(1)).execute(any(), any<KSerializer<*>>())
        }

    @Test
    fun `getOrFetchSession - when initial manifest passed in constructor, returns it and no network interaction`() =
        runTest {
            val initialSync = ApiKeyFixtures.syncResponse()
            val repository = buildRepository(initialSync = initialSync)

            val returnedManifest =
                repository.getOrSynchronizeFinancialConnectionsSession(
                    clientSecret = "",
                    applicationId = "",
                    attestationInitialized = false,
                    reFetchCondition = None::shouldReFetch
                )

            assertThat(returnedManifest).isEqualTo(initialSync)
            verifyNoInteractions(mockRequestExecutor)
        }

    /**
     * Simulates an API call to retrieve manifest that takes some time.
     */
    private suspend fun givenSyncSessionRequestReturnsAfterDelay(
        syncResponse: SynchronizeSessionResponse
    ) {
        val mock = mock<ApiRequest>()
        whenever(
            apiRequestFactory.createPost(
                url = any(),
                options = any(),
                params = any(),
                shouldCache = eq(false)
            )
        ).thenReturn(mock)
        given(mockRequestExecutor.execute(any(), any<KSerializer<*>>())).willSuspendableAnswer {
            delay(100)
            syncResponse
        }
    }
}
