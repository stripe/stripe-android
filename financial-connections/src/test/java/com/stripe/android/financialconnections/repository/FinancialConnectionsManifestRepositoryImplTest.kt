package com.stripe.android.financialconnections.repository

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.FinancialConnectionsPreCollectedConsent
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEventContext
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
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.mockito.kotlin.willSuspendableAnswer
import java.util.Locale
import kotlin.test.assertFailsWith

@ExperimentalCoroutinesApi
internal class FinancialConnectionsManifestRepositoryImplTest {

    private val mockRequestExecutor = mock<FinancialConnectionsRequestExecutor>()
    private val apiRequestFactory = mock<ApiRequest.Factory>()
    private val eventContext = FinancialConnectionsEventContext(null)

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
        locale = Locale.US,
        eventContext = eventContext
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
                        supportsAppVerification = false,
                        reFetchCondition = None::shouldReFetch,
                        preCollectedConsent = null
                    )
                },
                async {
                    repository.getOrSynchronizeFinancialConnectionsSession(
                        clientSecret = "",
                        applicationId = "",
                        supportsAppVerification = false,
                        reFetchCondition = None::shouldReFetch,
                        preCollectedConsent = null
                    )
                }
            )

            verify(mockRequestExecutor, times(1)).execute(any(), any<KSerializer<*>>())
            assertThat(eventContext.manifest?.id).isEqualTo(ApiKeyFixtures.syncResponse().manifest.id)
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
                    supportsAppVerification = false,
                    reFetchCondition = None::shouldReFetch,
                    preCollectedConsent = null
                )

            assertThat(returnedManifest).isEqualTo(initialSync)
            assertThat(eventContext.manifest).isEqualTo(initialSync.manifest)
            verifyNoInteractions(mockRequestExecutor)
        }

    @Test
    fun `getOrFetchSession - includes pre_collected_consent in request when provided`() = runTest {
        givenSyncSessionRequestReturnsAfterDelay(ApiKeyFixtures.syncResponse())
        val paramsCaptor = argumentCaptor<Map<String, Any?>>()

        val repository = buildRepository()
        repository.getOrSynchronizeFinancialConnectionsSession(
            clientSecret = "",
            applicationId = "",
            supportsAppVerification = false,
            reFetchCondition = None::shouldReFetch,
            preCollectedConsent = FinancialConnectionsPreCollectedConsent(
                consent = "fccons_123",
                collectedAt = 1_725_000_000L,
            )
        )

        verify(apiRequestFactory).createPost(
            url = any(),
            options = any(),
            params = paramsCaptor.capture(),
            shouldCache = eq(false)
        )
        assertThat(paramsCaptor.firstValue["pre_collected_consent"]).isEqualTo(
            mapOf(
                "consent" to "fccons_123",
                "collected_at" to 1_725_000_000L,
            )
        )
    }

    @Test
    fun `getOrFetchSession - omits pre_collected_consent from request when not provided`() = runTest {
        givenSyncSessionRequestReturnsAfterDelay(ApiKeyFixtures.syncResponse())
        val paramsCaptor = argumentCaptor<Map<String, Any?>>()

        val repository = buildRepository()
        repository.getOrSynchronizeFinancialConnectionsSession(
            clientSecret = "",
            applicationId = "",
            supportsAppVerification = false,
            reFetchCondition = None::shouldReFetch,
            preCollectedConsent = null
        )

        verify(apiRequestFactory).createPost(
            url = any(),
            options = any(),
            params = paramsCaptor.capture(),
            shouldCache = eq(false)
        )
        assertThat(paramsCaptor.firstValue).doesNotContainKey("pre_collected_consent")
    }

    @Test
    fun `getOrFetchSession - failed initial request retries with same pre_collected_consent`() = runTest {
        val request = mock<ApiRequest>()
        val paramsCaptor = argumentCaptor<Map<String, Any?>>()
        val preCollectedConsent = preCollectedConsent()
        whenever(
            apiRequestFactory.createPost(
                url = any(),
                options = any(),
                params = any(),
                shouldCache = eq(false),
            )
        ).thenReturn(request)
        whenever(mockRequestExecutor.execute(any(), any<KSerializer<*>>()))
            .thenThrow(IllegalStateException("network failure"))
            .thenReturn(ApiKeyFixtures.syncResponse())
        val repository = buildRepository()

        assertFailsWith<IllegalStateException> {
            repository.getOrSynchronizeFinancialConnectionsSession(
                clientSecret = "",
                applicationId = "",
                supportsAppVerification = false,
                reFetchCondition = None::shouldReFetch,
                preCollectedConsent = preCollectedConsent,
            )
        }
        repository.getOrSynchronizeFinancialConnectionsSession(
            clientSecret = "",
            applicationId = "",
            supportsAppVerification = false,
            reFetchCondition = None::shouldReFetch,
            preCollectedConsent = preCollectedConsent,
        )

        verify(apiRequestFactory, times(2)).createPost(
            url = any(),
            options = any(),
            params = paramsCaptor.capture(),
            shouldCache = eq(false),
        )
        assertThat(paramsCaptor.allValues.map { it["pre_collected_consent"] }).containsExactly(
            PRE_COLLECTED_CONSENT_PARAMS,
            PRE_COLLECTED_CONSENT_PARAMS,
        ).inOrder()
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

    private fun preCollectedConsent() = FinancialConnectionsPreCollectedConsent(
        consent = "fccons_123",
        collectedAt = 1_725_000_000L,
    )

    private companion object {
        val PRE_COLLECTED_CONSENT_PARAMS = mapOf(
            "consent" to "fccons_123",
            "collected_at" to 1_725_000_000L,
        )
    }
}
