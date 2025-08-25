package com.stripe.android.crypto.onramp

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.ApiVersion
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeResponse
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.crypto.onramp.model.DateOfBirth
import com.stripe.android.crypto.onramp.model.IdType
import com.stripe.android.crypto.onramp.model.KycInfo
import com.stripe.android.crypto.onramp.repositories.CryptoApiRepository
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentsheet.PaymentSheet
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

    private val stripeRepository = mock<StripeRepository>()

    private val cryptoApiRepository = CryptoApiRepository(
        stripeNetworkClient = stripeNetworkClient,
        stripeRepository = stripeRepository,
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

    @Test
    fun testCollectKycDataSucceeds() {
        runTest {
            val stripeResponse = StripeResponse(
                200,
                "{}",
                emptyMap()
            )

            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenReturn(stripeResponse)

            val result = cryptoApiRepository.collectKycData(
                KycInfo(
                    firstName = "Test",
                    lastName = "User",
                    idNumber = "999-88-7777",
                    idType = IdType.SOCIAL_SECURITY_NUMBER,
                    dateOfBirth = DateOfBirth(day = 1, month = 3, year = 1975),
                    address = PaymentSheet.Address(city = "Orlando", state = "FL"),
                    nationalities = listOf("TestNationality"),
                    birthCountry = "US",
                    birthCity = "Chicago"
                ),
                consumerSessionClientSecret = "test-secret"
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())
            val apiRequest = apiRequestArgumentCaptor.firstValue

            assertThat(apiRequest.baseUrl)
                .isEqualTo("https://api.stripe.com/v1/crypto/internal/kyc_data_collection")

            val params = apiRequest.params!!
            val dobValue = params["dob"] as Map<*, *>
            assertThat(params["first_name"]).isEqualTo("Test")
            assertThat(params["last_name"]).isEqualTo("User")
            assertThat(params["id_number"]).isEqualTo("999-88-7777")
            assertThat(params["id_type"]).isEqualTo("social_security_number")
            assertThat(dobValue["day"]).isEqualTo("1")
            assertThat(dobValue["month"]).isEqualTo("3")
            assertThat(dobValue["year"]).isEqualTo("1975")
            assertThat(params["nationalities"]).isEqualTo(listOf("TestNationality"))
            assertThat(params["birth_country"]).isEqualTo("US")
            assertThat(params["birth_city"]).isEqualTo("Chicago")
            assertThat(params["city"]).isEqualTo("Orlando")
            assertThat(params["state"]).isEqualTo("FL")
            assertThat(params["country"]).isNull()
            assertThat(params["line1"]).isNull()
            assertThat(params["line2"]).isNull()
            assertThat(params["zip"]).isNull()

            assertThat(params["credentials"]).isEqualTo(
                mapOf("consumer_session_client_secret" to "test-secret")
            )

            assertThat(result.isSuccess)
                .isEqualTo(true)
        }
    }

    @Test
    fun testStartIdentityVerificationSucceeds() {
        runTest {
            val stripeResponse = StripeResponse(
                200,
                """
                    {
                        "id": "test-id",
                        "url": "https://www.google.com",
                        "ephemeral_key": "test-key"
                    }
                    """,
                emptyMap()
            )

            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenReturn(stripeResponse)

            val result = cryptoApiRepository.startIdentityVerification(consumerSessionClientSecret = "test-secret")

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())
            val apiRequest = apiRequestArgumentCaptor.firstValue

            assertThat(apiRequest.baseUrl)
                .isEqualTo("https://api.stripe.com/v1/crypto/internal/start_identity_verification")

            val params = apiRequest.params!!
            assertThat(params["is_mobile"]).isEqualTo("true")
            assertThat(params["credentials"]).isEqualTo(
                mapOf("consumer_session_client_secret" to "test-secret")
            )

            assertThat(result.isSuccess)
                .isEqualTo(true)
        }
    }
}
