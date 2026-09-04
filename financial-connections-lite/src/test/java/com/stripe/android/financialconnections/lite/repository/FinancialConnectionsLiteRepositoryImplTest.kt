package com.stripe.android.financialconnections.lite.repository

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.financialconnections.FinancialConnectionsPreCollectedConsent
import com.stripe.android.financialconnections.lite.TextFixtures
import com.stripe.android.financialconnections.lite.network.FinancialConnectionsLiteRequestExecutor
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.KSerializer
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

internal class FinancialConnectionsLiteRepositoryImplTest {

    private val requestExecutor = mock<FinancialConnectionsLiteRequestExecutor>()
    private val apiRequestFactory = mock<ApiRequest.Factory>()

    private val repository = FinancialConnectionsLiteRepositoryImpl(
        requestExecutor = requestExecutor,
        apiRequestFactory = apiRequestFactory,
    )

    @Test
    fun `synchronize - includes pre_collected_consent in request when provided`() = runTest {
        val paramsCaptor = argumentCaptor<Map<String, Any?>>()
        givenSynchronizeRequestSucceeds()

        repository.synchronize(
            configuration = TextFixtures.configuration.copy(
                preCollectedConsent = FinancialConnectionsPreCollectedConsent(consent = "fccons_123")
            ),
            applicationId = "com.stripe.android.test"
        )

        verify(apiRequestFactory).createPost(
            url = any(),
            options = any(),
            params = paramsCaptor.capture(),
            shouldCache = eq(false)
        )
        assertThat(paramsCaptor.firstValue["pre_collected_consent"]).isEqualTo(mapOf("consent" to "fccons_123"))
    }

    @Test
    fun `synchronize - omits pre_collected_consent from request when not provided`() = runTest {
        val paramsCaptor = argumentCaptor<Map<String, Any?>>()
        givenSynchronizeRequestSucceeds()

        repository.synchronize(
            configuration = TextFixtures.configuration,
            applicationId = "com.stripe.android.test"
        )

        verify(apiRequestFactory).createPost(
            url = any(),
            options = any(),
            params = paramsCaptor.capture(),
            shouldCache = eq(false)
        )
        assertThat(paramsCaptor.firstValue).doesNotContainKey("pre_collected_consent")
    }

    private suspend fun givenSynchronizeRequestSucceeds() {
        val request = mock<ApiRequest>()
        whenever(
            apiRequestFactory.createPost(
                url = any(),
                options = any(),
                params = any(),
                shouldCache = eq(false)
            )
        ).thenReturn(request)
        whenever(requestExecutor.execute(any(), any<KSerializer<*>>())).thenReturn(
            Result.success(TextFixtures.syncResponse)
        )
    }
}
