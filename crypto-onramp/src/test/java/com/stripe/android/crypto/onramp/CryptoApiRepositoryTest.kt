package com.stripe.android.crypto.onramp

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.ApiVersion
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeResponse
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.crypto.onramp.model.CryptoNetwork
import com.stripe.android.crypto.onramp.model.DateOfBirth
import com.stripe.android.crypto.onramp.model.KycInfo
import com.stripe.android.crypto.onramp.model.RefreshKycInfo
import com.stripe.android.crypto.onramp.repositories.CryptoApiRepository
import com.stripe.android.link.LinkController
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
    private val linkController = mock<LinkController>()

    private val cryptoApiRepository = CryptoApiRepository(
        stripeNetworkClient = stripeNetworkClient,
        stripeRepository = stripeRepository,
        publishableKeyProvider = { "pk_test_vOo1umqsYxSrP5UXfOeL3ecm" },
        stripeAccountIdProvider = { "TestAccountId" },
        apiVersion = ApiVersion(betas = emptySet()).code,
        sdkVersion = StripeSdkVersion.VERSION,
        appInfo = null,
        linkController = linkController
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

            val result = cryptoApiRepository.createCryptoCustomer(
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
    fun testGrantingPartnerMerchantPermissionsFails() {
        runTest {
            val stripeResponse = StripeResponse(
                400,
                """
                    {
                    }
                    """,
                emptyMap()
            )

            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenReturn(stripeResponse)

            val result = cryptoApiRepository.createCryptoCustomer(
                consumerSessionClientSecret = "test-secret"
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())

            assertThat(result.isFailure)
                .isEqualTo(true)
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
                    dateOfBirth = DateOfBirth(day = 1, month = 3, year = 1975),
                    address = PaymentSheet.Address(city = "Orlando", state = "FL")
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
            assertThat(params["id_type"]).isEqualTo("social_security_number") // Hardcoded to match iOS
            assertThat(dobValue["day"]).isEqualTo("1")
            assertThat(dobValue["month"]).isEqualTo("3")
            assertThat(dobValue["year"]).isEqualTo("1975")
            assertThat(params["nationalities"]).isNull()
            assertThat(params["birth_country"]).isNull()
            assertThat(params["birth_city"]).isNull()
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
    fun testCollectKycDataFails() {
        runTest {
            val stripeResponse = StripeResponse(
                400,
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
                    dateOfBirth = DateOfBirth(day = 1, month = 3, year = 1975),
                    address = PaymentSheet.Address(city = "Orlando", state = "FL")
                ),
                consumerSessionClientSecret = "test-secret"
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())

            assertThat(result.isFailure)
                .isEqualTo(true)
        }
    }

    @Test
    fun testRetrieveKycDataSucceeds() {
        runTest {
            val stripeResponse = StripeResponse(
                200,
                """
                    {
                        "first_name": "Test",
                        "last_name": "User",
                        "id_number_last4": "7777",
                        "id_type": "social_security_number",
                        "dob": {
                            "day": 1,
                            "month": 3,
                            "year": 1975
                        },
                        "address": {
                            "line1": "123 Main St",
                            "line2": "Apt 4B",
                            "zip": "32801",
                            "country": "US",
                            "city": "Orlando",
                            "state": "FL"
                        }
                    }
                    """,
                emptyMap()
            )

            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenReturn(stripeResponse)

            val result = cryptoApiRepository.retrieveKycInfo(
                consumerSessionClientSecret = "test-secret"
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())
            val apiRequest = apiRequestArgumentCaptor.firstValue

            assertThat(apiRequest.baseUrl)
                .isEqualTo("https://api.stripe.com/v1/crypto/internal/kyc_data_retrieve")

            val params = apiRequest.params!!
            assertThat(params["credentials"]).isEqualTo(
                mapOf("consumer_session_client_secret" to "test-secret")
            )

            assertThat(result.getOrThrow().firstName).isEqualTo("Test")
            assertThat(result.getOrThrow().lastName).isEqualTo("User")
            assertThat(result.getOrThrow().idNumberLastFour).isEqualTo("7777")
            assertThat(result.getOrThrow().idType).isEqualTo("social_security_number")
            assertThat(result.getOrThrow().dateOfBirth).isEqualTo(DateOfBirth(day = 1, month = 3, year = 1975))
            assertThat(result.getOrThrow().address.line1).isEqualTo("123 Main St")
            assertThat(result.getOrThrow().address.line2).isEqualTo("Apt 4B")
            assertThat(result.getOrThrow().address.postalCode).isEqualTo("32801")
            assertThat(result.getOrThrow().address.country).isEqualTo("US")
            assertThat(result.getOrThrow().address.city).isEqualTo("Orlando")
            assertThat(result.getOrThrow().address.state).isEqualTo("FL")

            assertThat(result.isSuccess)
                .isEqualTo(true)
        }
    }

    @Test
    fun testRetrieveKycDataFails() {
        runTest {
            val stripeResponse = StripeResponse(
                400,
                "{}",
                emptyMap()
            )

            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenReturn(stripeResponse)

            val result = cryptoApiRepository.retrieveKycInfo(
                consumerSessionClientSecret = "test-secret"
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())

            assertThat(result.isFailure)
                .isEqualTo(true)
        }
    }

    @Test
    fun testRefreshKycDataSucceeds() {
        runTest {
            val stripeResponse = StripeResponse(
                200,
                "{}",
                emptyMap()
            )

            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenReturn(stripeResponse)

            val result = cryptoApiRepository.refreshKycData(
                RefreshKycInfo(
                    firstName = "Test",
                    lastName = "User",
                    idNumberLastFour = "7777",
                    dateOfBirth = DateOfBirth(day = 1, month = 3, year = 1975),
                    address = PaymentSheet.Address(city = "Orlando", state = "FL")
                ),
                consumerSessionClientSecret = "test-secret"
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())
            val apiRequest = apiRequestArgumentCaptor.firstValue

            assertThat(apiRequest.baseUrl)
                .isEqualTo("https://api.stripe.com/v1/crypto/internal/refresh_consumer_person")

            val params = apiRequest.params!!
            val dobValue = params["dob"] as Map<*, *>
            assertThat(params["first_name"]).isEqualTo("Test")
            assertThat(params["last_name"]).isEqualTo("User")
            assertThat(params["id_number_last4"]).isEqualTo("7777")
            assertThat(params["id_type"]).isEqualTo("social_security_number") // Hardcoded to match iOS
            assertThat(dobValue["day"]).isEqualTo("1")
            assertThat(dobValue["month"]).isEqualTo("3")
            assertThat(dobValue["year"]).isEqualTo("1975")
            assertThat(params["nationalities"]).isNull()
            assertThat(params["birth_country"]).isNull()
            assertThat(params["birth_city"]).isNull()
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
    fun testRefreshKycDataFails() {
        runTest {
            val stripeResponse = StripeResponse(
                400,
                "{}",
                emptyMap()
            )

            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenReturn(stripeResponse)

            val result = cryptoApiRepository.refreshKycData(
                RefreshKycInfo(
                    firstName = "Test",
                    lastName = "User",
                    idNumberLastFour = "7777",
                    dateOfBirth = DateOfBirth(day = 1, month = 3, year = 1975),
                    address = PaymentSheet.Address(city = "Orlando", state = "FL")
                ),
                consumerSessionClientSecret = "test-secret"
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())

            assertThat(result.isFailure)
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

    @Test
    fun testStartIdentityVerificationFails() {
        runTest {
            val stripeResponse = StripeResponse(
                400,
                """
                    {
                    }
                    """,
                emptyMap()
            )

            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenReturn(stripeResponse)

            val result = cryptoApiRepository.startIdentityVerification(consumerSessionClientSecret = "test-secret")

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())

            assertThat(result.isFailure)
                .isEqualTo(true)
        }
    }

    @Test
    fun testCollectWalletAddressSucceeds() {
        runTest {
            val stripeResponse = StripeResponse(
                200,
                "{}",
                emptyMap()
            )

            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenReturn(stripeResponse)

            val result = cryptoApiRepository.setWalletAddress(
                walletAddress = "0x1234567890abcdef",
                network = CryptoNetwork.Ethereum,
                consumerSessionClientSecret = "test-secret"
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())
            val apiRequest = apiRequestArgumentCaptor.firstValue

            assertThat(apiRequest.baseUrl)
                .isEqualTo("https://api.stripe.com/v1/crypto/internal/wallet")

            val params = apiRequest.params!!
            assertThat(params["wallet_address"]).isEqualTo("0x1234567890abcdef")
            assertThat(params["network"]).isEqualTo("ethereum")
            assertThat(params["credentials"]).isEqualTo(
                mapOf("consumer_session_client_secret" to "test-secret")
            )

            assertThat(result.isSuccess)
                .isEqualTo(true)
        }
    }

    @Test
    fun testCollectWalletAddressFails() {
        runTest {
            val stripeResponse = StripeResponse(
                400,
                "{}",
                emptyMap()
            )

            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenReturn(stripeResponse)

            val result = cryptoApiRepository.setWalletAddress(
                walletAddress = "0x1234567890abcdef",
                network = CryptoNetwork.Ethereum,
                consumerSessionClientSecret = "test-secret"
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())

            assertThat(result.isFailure)
                .isEqualTo(true)
        }
    }
}
