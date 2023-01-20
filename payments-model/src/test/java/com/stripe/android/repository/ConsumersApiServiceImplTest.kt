package com.stripe.android.repository

import com.stripe.android.ConsumerFixtures
import com.stripe.android.core.ApiVersion
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeResponse
import com.stripe.android.core.version.StripeSdkVersion
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.verify
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ConsumersApiServiceImplTest {

    private val stripeNetworkClient: StripeNetworkClient = mock()

    private val consumersApiService = ConsumersApiServiceImpl(
        stripeNetworkClient = stripeNetworkClient,
        sdkVersion = StripeSdkVersion.VERSION,
        apiVersion = ApiVersion(betas = emptySet()).code,
        appInfo = null
    )

    private val apiRequestArgumentCaptor: KArgumentCaptor<ApiRequest> = argumentCaptor()

    @Test
    fun `lookupConsumerSession() sends all parameters`() = runTest {
        val stripeResponse = StripeResponse(
            200,
            ConsumerFixtures.EXISTING_CONSUMER_JSON.toString(),
            emptyMap()
        )
        whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
            .thenReturn(stripeResponse)

        val email = "email@example.com"
        val cookie = "cookie1"
        consumersApiService.lookupConsumerSession(
            email,
            cookie,
            DEFAULT_OPTIONS
        )

        verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())
        val params = requireNotNull(apiRequestArgumentCaptor.firstValue.params)

        with(params) {
            assertEquals(this["email_address"], email)
            assertEquals(
                this["cookies"],
                mapOf(
                    "verification_session_client_secrets" to listOf(cookie)
                )
            )
        }
    }

    @Test
    fun testConsumerSessionLookupUrl() {
        assertEquals(
            "https://api.stripe.com/v1/consumers/sessions/lookup",
            ConsumersApiServiceImpl.consumerSessionLookupUrl
        )
    }

    private companion object {
        private val DEFAULT_OPTIONS = ApiRequest.Options("pk_test_vOo1umqsYxSrP5UXfOeL3ecm")
    }
}