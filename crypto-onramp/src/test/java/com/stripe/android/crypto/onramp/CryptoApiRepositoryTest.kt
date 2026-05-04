package com.stripe.android.crypto.onramp

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.ApiVersion
import com.stripe.android.core.model.CountryCode
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeResponse
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.crypto.onramp.model.Compliance.ComplianceIdentifier
import com.stripe.android.crypto.onramp.model.Compliance.ComplianceIdentifierAlternativeGroup
import com.stripe.android.crypto.onramp.model.Compliance.ComplianceIdentifierRequirement
import com.stripe.android.crypto.onramp.model.Compliance.ComplianceIdentifierType
import com.stripe.android.crypto.onramp.model.Compliance.ComplianceRegulation
import com.stripe.android.crypto.onramp.model.CryptoNetwork
import com.stripe.android.crypto.onramp.model.KycInfo
import com.stripe.android.crypto.onramp.model.RefreshKycInfo
import com.stripe.android.crypto.onramp.model.Compliance.SubmitIdentifiersResult
import com.stripe.android.crypto.onramp.repositories.CryptoApiRepository
import com.stripe.android.link.LinkController
import com.stripe.android.model.DateOfBirth
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
@Suppress("LargeClass")
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
                    address = PaymentSheet.Address(city = "Orlando", state = "FL"),
                    birthCountry = CountryCode.create("IE"),
                    birthCity = "Dublin",
                    nationalities = listOf(CountryCode.create("IE"), CountryCode.create("FR"))
                ),
                consumerSessionClientSecret = "test-secret"
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())

            assertThat(result.isSuccess)
                .isEqualTo(true)
            assertKycCollectionRequest(apiRequestArgumentCaptor.firstValue)
        }
    }

    @Test
    fun testCollectKycDataTrimsNationalities() {
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
                    address = PaymentSheet.Address(city = "Orlando", state = "FL"),
                    nationalities = listOf(
                        CountryCode(" IE "),
                        CountryCode("\nFR\t"),
                    )
                ),
                consumerSessionClientSecret = "test-secret"
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())

            assertThat(result.isSuccess).isTrue()
            assertThat(apiRequestArgumentCaptor.firstValue.params?.get("nationalities"))
                .isEqualTo(listOf("IE", "FR"))
        }
    }

    @Test
    fun testRetrieveMissingIdentifiersSucceeds() {
        runTest {
            val stripeResponse = StripeResponse(
                200,
                """
                    {
                        "alternatives": [
                            {
                                "alternative_missing_identifiers": ["mt_pp"],
                                "original_missing_identifiers": ["mt_nic"]
                            }
                        ],
                        "identifiers": [
                            {
                                "regulation": "eu_mica",
                                "type": "mt_nic"
                            },
                            {
                                "regulation": "eu_carf",
                                "type": "fr_spi"
                            }
                        ]
                    }
                    """,
                emptyMap()
            )

            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenReturn(stripeResponse)

            val result = cryptoApiRepository.retrieveMissingIdentifiers(
                consumerSessionClientSecret = "test-secret"
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())
            val apiRequest = apiRequestArgumentCaptor.firstValue

            assertThat(apiRequest.baseUrl)
                .isEqualTo("https://api.stripe.com/v1/crypto/internal/identifier_requirements")
            assertThat(apiRequest.params)
                .isEqualTo(mapOf("credentials" to mapOf("consumer_session_client_secret" to "test-secret")))

            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrThrow().identifiers)
                .containsExactly(
                    ComplianceIdentifierRequirement(
                        type = ComplianceIdentifierType.MT_NIC,
                        regulation = ComplianceRegulation.EuMica
                    ),
                    ComplianceIdentifierRequirement(
                        type = ComplianceIdentifierType.FR_SPI,
                        regulation = ComplianceRegulation.EuCarf
                    )
                )
            assertThat(result.getOrThrow().alternatives)
                .containsExactly(
                    ComplianceIdentifierAlternativeGroup(
                        originalMissingIdentifiers = listOf(ComplianceIdentifierType.MT_NIC),
                        alternativeMissingIdentifiers = listOf(ComplianceIdentifierType.MT_PP)
                    )
                )
        }
    }

    @Test
    fun testSubmitIdentifiersSucceeds() {
        runTest {
            val stripeResponse = StripeResponse(
                200,
                """
                    {
                        "alternatives": [
                            {
                                "alternative_missing_identifiers": [
                                    "mt_pp"
                                ],
                                "original_missing_identifiers": [
                                    "mt_nic"
                                ]
                            }
                        ],
                        "identifiers": [
                            {
                                "regulation": "eu_carf",
                                "type": "de_stn"
                            },
                            {
                                "regulation": "eu_carf",
                                "type": "mt_nic"
                            },
                            {
                                "regulation": "eu_mica",
                                "type": "mt_nic"
                            }
                        ],
                        "invalid_identifiers": [
                            "de_stn",
                            "mt_nic"
                        ],
                        "valid": false,
                        "ignored_field": "ignored"
                    }
                    """,
                emptyMap()
            )

            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenReturn(stripeResponse)

            val result = cryptoApiRepository.submitIdentifiers(
                identifiers = listOf(
                    ComplianceIdentifier()
                        .type(ComplianceIdentifierType.MT_NIC)
                        .value("mica_123"),
                    ComplianceIdentifier()
                        .type(ComplianceIdentifierType.FR_SPI)
                        .value("carf_456")
                ),
                consumerSessionClientSecret = "test-secret"
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())
            assertIdentifiersRequest(apiRequestArgumentCaptor.firstValue)
            assertThat(result.isSuccess).isTrue()
            assertSubmitIdentifiersResult(result.getOrThrow())
        }
    }

    @Test
    fun testSubmitIdentifiersSucceedsWhenValid() {
        runTest {
            val stripeResponse = StripeResponse(
                200,
                """
                    {
                        "alternatives": [],
                        "identifiers": [],
                        "invalid_identifiers": [],
                        "valid": true
                    }
                    """,
                emptyMap()
            )

            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenReturn(stripeResponse)

            val result = cryptoApiRepository.submitIdentifiers(
                identifiers = listOf(
                    ComplianceIdentifier()
                        .type(ComplianceIdentifierType.MT_NIC)
                        .value("mica_123")
                ),
                consumerSessionClientSecret = "test-secret"
            )

            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrThrow())
                .isEqualTo(
                    SubmitIdentifiersResult(
                        valid = true,
                        identifiers = emptyList(),
                        alternatives = emptyList(),
                        invalidIdentifiers = emptyList()
                    )
                )
        }
    }

    @Test
    fun testRetrieveCrsCarfDeclarationSucceeds() {
        runTest {
            val stripeResponse = StripeResponse(
                200,
                """
                    {
                        "text": "I confirm this declaration.",
                        "version": "2026-04-23"
                    }
                    """,
                emptyMap()
            )

            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenReturn(stripeResponse)

            val result = cryptoApiRepository.retrieveCrsCarfDeclaration(
                consumerSessionClientSecret = "test-secret"
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())
            val apiRequest = apiRequestArgumentCaptor.firstValue

            assertThat(apiRequest.baseUrl)
                .isEqualTo("https://api.stripe.com/v1/crypto/internal/crs_carf_declaration")
            assertThat(apiRequest.params)
                .isEqualTo(mapOf("credentials" to mapOf("consumer_session_client_secret" to "test-secret")))
            assertThat(result.getOrThrow().text)
                .isEqualTo("I confirm this declaration.")
            assertThat(result.getOrThrow().version)
                .isEqualTo("2026-04-23")
        }
    }

    @Test
    fun testConfirmCrsCarfDeclarationSucceeds() {
        runTest {
            val stripeResponse = StripeResponse(
                200,
                "{}",
                emptyMap()
            )

            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenReturn(stripeResponse)

            val result = cryptoApiRepository.confirmCrsCarfDeclaration(
                consumerSessionClientSecret = "test-secret"
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())
            val apiRequest = apiRequestArgumentCaptor.firstValue

            assertThat(apiRequest.baseUrl)
                .isEqualTo("https://api.stripe.com/v1/crypto/internal/crs_carf_declaration")
            assertThat(apiRequest.params)
                .isEqualTo(mapOf("credentials" to mapOf("consumer_session_client_secret" to "test-secret")))
            assertThat(result.isSuccess).isTrue()
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
                            "postal_code": "32801",
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
                    idType = "social_security_number",
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
            assertThat(params["country"]).isEqualTo("")
            assertThat(params["line1"]).isEqualTo("")
            assertThat(params["line2"]).isEqualTo("")
            assertThat(params["zip"]).isEqualTo("")

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
                    idType = "social_security_number",
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

    private fun assertKycCollectionRequest(apiRequest: ApiRequest) {
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
        assertThat(params["nationalities"]).isEqualTo(listOf("IE", "FR"))
        assertThat(params["birth_country"]).isEqualTo("IE")
        assertThat(params["birth_city"]).isEqualTo("Dublin")
        assertThat(params["city"]).isEqualTo("Orlando")
        assertThat(params["state"]).isEqualTo("FL")
        assertThat(params["country"]).isNull()
        assertThat(params["line1"]).isNull()
        assertThat(params["line2"]).isNull()
        assertThat(params["zip"]).isNull()
        assertThat(params["credentials"]).isEqualTo(
            mapOf("consumer_session_client_secret" to "test-secret")
        )
    }

    private fun assertIdentifiersRequest(apiRequest: ApiRequest) {
        assertThat(apiRequest.baseUrl)
            .isEqualTo("https://api.stripe.com/v1/crypto/internal/eu_identifiers")
        assertThat(apiRequest.params)
            .isEqualTo(
                mapOf(
                    "credentials" to mapOf("consumer_session_client_secret" to "test-secret"),
                    "identifiers" to listOf(
                        mapOf(
                            "type" to "mt_nic",
                            "value" to "mica_123"
                        ),
                        mapOf(
                            "type" to "fr_spi",
                            "value" to "carf_456"
                        )
                    )
                )
            )
    }

    private fun assertSubmitIdentifiersResult(result: SubmitIdentifiersResult) {
        assertThat(result.valid).isFalse()
        assertThat(result.identifiers)
            .containsExactly(
                ComplianceIdentifierRequirement(
                    type = ComplianceIdentifierType.DE_STN,
                    regulation = ComplianceRegulation.EuCarf
                ),
                ComplianceIdentifierRequirement(
                    type = ComplianceIdentifierType.MT_NIC,
                    regulation = ComplianceRegulation.EuCarf
                ),
                ComplianceIdentifierRequirement(
                    type = ComplianceIdentifierType.MT_NIC,
                    regulation = ComplianceRegulation.EuMica
                )
            )
        assertThat(result.alternatives)
            .containsExactly(
                ComplianceIdentifierAlternativeGroup(
                    originalMissingIdentifiers = listOf(ComplianceIdentifierType.MT_NIC),
                    alternativeMissingIdentifiers = listOf(ComplianceIdentifierType.MT_PP)
                )
            )
        assertThat(result.invalidIdentifiers)
            .isEqualTo(listOf(ComplianceIdentifierType.DE_STN, ComplianceIdentifierType.MT_NIC))
    }
}
