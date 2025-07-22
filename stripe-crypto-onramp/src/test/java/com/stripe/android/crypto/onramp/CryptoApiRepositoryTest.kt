package com.stripe.android.crypto.onramp

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.ApiVersion
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeResponse
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.crypto.onramp.repositories.CryptoApiRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CryptoApiRepositoryTest {
    private val stripeNetworkClient: StripeNetworkClient = mock()

    private val cryptoApiRepository = CryptoApiRepository(
        stripeNetworkClient = stripeNetworkClient,
        publishableKeyProvider = { "pk_test_vOo1umqsYxSrP5UXfOeL3ecm" },
        stripeAccountIdProvider = { "TestAccountId" },
        apiVersion = ApiVersion(betas = emptySet()).code,
        sdkVersion = StripeSdkVersion.VERSION,
        appInfo = null
    )

    private val apiRequestArgumentCaptor: KArgumentCaptor<ApiRequest> = argumentCaptor()

    @Test
    fun testGrantingPartnerMerchantPermissionsSucceeds() {
        runTest {
            val stripeResponse = StripeResponse(
                200,
                """
                    {
                        "id": "test-id"
                    }
                    """,
                emptyMap()
            )

            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenReturn(stripeResponse)

            val result = cryptoApiRepository.grantPartnerMerchantPermissions(
                consumerSessionClientSecret = "test-secret"
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())
            val apiRequest = apiRequestArgumentCaptor.firstValue

            assertThat(apiRequest.baseUrl)
                .isEqualTo("https://api.stripe.com/v1/crypto/internal/customers")

            assertThat(apiRequest.params)
                .isEqualTo(mapOf("credentials" to mapOf("consumer_session_client_secret" to "test-secret")))

            assertThat(result.isSuccess)
                .isEqualTo(true)

            assertThat(result.getOrThrow().id)
                .isEqualTo("test-id")
        }
    }
}
