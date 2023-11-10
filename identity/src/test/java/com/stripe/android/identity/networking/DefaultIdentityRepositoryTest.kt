package com.stripe.android.identity.networking

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.model.Country
import com.stripe.android.core.model.CountryCode
import com.stripe.android.core.model.StripeFile
import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.HEADER_AUTHORIZATION
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeRequest
import com.stripe.android.core.networking.StripeResponse
import com.stripe.android.identity.networking.models.ClearDataParam
import com.stripe.android.identity.networking.models.ClearDataParam.Companion.createCollectedDataParamEntry
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.CollectedDataParam.Companion.createCollectedDataParamEntry
import com.stripe.android.identity.networking.models.Requirement
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageData
import com.stripe.android.identity.networking.models.VerificationPageIconType
import com.stripe.android.identity.networking.models.VerificationPageStaticConsentLineContent
import com.stripe.android.identity.utils.IdentityIO
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.net.HttpURLConnection.HTTP_OK
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
class DefaultIdentityRepositoryTest {
    private val mockIO = mock<IdentityIO>().also {
        whenever(it.createTFLiteFile(any())).thenReturn(mock())
    }
    private val mockStripeNetworkClient: StripeNetworkClient = mock()
    private val identityRepository = DefaultIdentityRepository(
        mockStripeNetworkClient,
        mockIO
    )

    private val requestCaptor: KArgumentCaptor<StripeRequest> = argumentCaptor()

    @Test
    fun `retrieveVerificationPage - type document, not require live capture`() {
        testFetchVerificationPage(VERIFICATION_PAGE_NOT_REQUIRE_LIVE_CAPTURE_JSON_STRING) {
            assertThat(it.documentCapture.requireLiveCapture).isFalse()
            assertThat(it.requirements.missing).containsExactly(
                Requirement.BIOMETRICCONSENT,
                Requirement.IDDOCUMENTFRONT,
                Requirement.IDDOCUMENTBACK
            )
        }
    }

    @Test
    fun `retrieveVerificationPage - type document, require selfie`() {
        testFetchVerificationPage(VERIFICATION_PAGE_REQUIRE_SELFIE_LIVE_CAPTURE_JSON_STRING) {
            assertThat(it.selfieCapture).isNotNull()
            assertThat(it.requirements.missing).containsExactly(
                Requirement.BIOMETRICCONSENT,
                Requirement.IDDOCUMENTFRONT,
                Requirement.IDDOCUMENTBACK,
                Requirement.FACE
            )
        }
    }

    @Test
    fun `retrieveVerificationPage - type document, require id_number`() {
        testFetchVerificationPage(VERIFICATION_PAGE_TYPE_DOCUMENT_REQUIRE_ID_NUMBER_JSON_STRING) {
            assertThat(it.requirements.missing).containsExactly(
                Requirement.BIOMETRICCONSENT,
                Requirement.IDDOCUMENTFRONT,
                Requirement.IDDOCUMENTBACK,
                Requirement.IDNUMBER
            )
        }
    }

    @Test
    fun `retrieveVerificationPage - type document, require address`() {
        testFetchVerificationPage(VERIFICATION_PAGE_TYPE_DOCUMENT_REQUIRE_ADDRESS_JSON_STRING) {
            assertThat(it.requirements.missing).containsExactly(
                Requirement.BIOMETRICCONSENT,
                Requirement.IDDOCUMENTFRONT,
                Requirement.IDDOCUMENTBACK,
                Requirement.ADDRESS
            )
        }
    }

    @Test
    fun `retrieveVerificationPage - type document, require address and id_number`() {
        testFetchVerificationPage(
            VERIFICATION_PAGE_TYPE_DOCUMENT_REQUIRE_ADDRESS_AND_ID_NUMBER_JSON_STRING
        ) {
            assertThat(it.requirements.missing).containsExactly(
                Requirement.BIOMETRICCONSENT,
                Requirement.IDDOCUMENTFRONT,
                Requirement.IDDOCUMENTBACK,
                Requirement.ADDRESS,
                Requirement.IDNUMBER,
            )
        }
    }

    @Test
    fun `retrieveVerificationPage - type id`() {
        testFetchVerificationPage(
            VERIFICATION_PAGE_TYPE_ID_NUMBER_JSON_STRING
        ) {
            assertThat(it.requirements.missing).containsExactly(
                Requirement.IDNUMBER,
                Requirement.NAME,
                Requirement.DOB
            )
        }
    }

    @Test
    fun `retrieveVerificationPage - type address`() {
        testFetchVerificationPage(
            VERIFICATION_PAGE_TYPE_ADDRESS_JSON_STRING
        ) {
            assertThat(it.requirements.missing).containsExactly(
                Requirement.ADDRESS,
                Requirement.NAME,
                Requirement.DOB
            )
        }
    }

    @Test
    fun `retrieveVerificationPage - verify individual static page`() {
        testFetchVerificationPage(
            VERIFICATION_PAGE_TYPE_ADDRESS_JSON_STRING
        ) {
            assertThat(it.individual.addressCountries).hasSize(31)
            assertThat(it.individual.idNumberCountries).containsExactly(
                Country(CountryCode("BR"), "Brazil"),
                Country(CountryCode("SG"), "Singapore"),
                Country(CountryCode("US"), "United States"),
            )
            assertThat(it.individual.title).isEqualTo("Provide personal information")
            assertThat(it.individual.addressCountryNotListedTextButtonText).isEqualTo("My country is not listed")
            assertThat(it.individual.idNumberCountryNotListedTextButtonText).isEqualTo("My country is not listed")
        }
    }

    @Test
    fun `retrieveVerificationPage - verify individual welcome static page`() {
        testFetchVerificationPage(
            VERIFICATION_PAGE_TYPE_ADDRESS_JSON_STRING
        ) {
            assertThat(it.individualWelcome.getStartedButtonText).isEqualTo("Get started")
            assertThat(it.individualWelcome.title).isEqualTo(
                "Andrew's Audio works with Stripe to verify your identity"
            )
            assertThat(it.individualWelcome.privacyPolicy).isEqualTo(
                "<a href='https://stripe.com/privacy'>Stripe Privacy Policy</a> • " +
                    "<a href='https://aywang.me'>Andrew's Audio Privacy Policy</a>"
            )
            assertThat(it.individualWelcome.lines).isEqualTo(
                listOf(
                    VerificationPageStaticConsentLineContent(
                        icon = VerificationPageIconType.DOCUMENT,
                        content = "You'll provide personal information including your name and " +
                            "phone number."
                    ),
                    VerificationPageStaticConsentLineContent(
                        icon = VerificationPageIconType.DISPUTE_PROTECTION,
                        content = "The information you provide Stripe will help us <a " +
                            "href='stripe_bottomsheet://open/consent_identity'>" +
                            "confirm your identity</a>."
                    ),
                    VerificationPageStaticConsentLineContent(
                        icon = VerificationPageIconType.LOCK,
                        content = "Andrew's Audio will only have access to this <a " +
                            "href='stripe_bottomsheet://open/consent_verification_data'>" +
                            "verification data</a>."
                    )
                )
            )
        }
    }

    @Test
    fun `retrieveVerificationPage - verify countryNotListed page`() {
        testFetchVerificationPage(
            VERIFICATION_PAGE_TYPE_ADDRESS_JSON_STRING
        ) {
            assertThat(it.countryNotListedPage.title).isEqualTo(
                "We cannot verify your identity"
            )
            assertThat(it.countryNotListedPage.idFromOtherCountryTextButtonText).isEqualTo(
                "Have an ID from another country?"
            )
            assertThat(it.countryNotListedPage.addressFromOtherCountryTextButtonText).isEqualTo(
                "Have an Address from another country?"
            )
            assertThat(it.countryNotListedPage.body).isEqualTo(
                "The countries not listed are not supported yet. Unfortunately, we cannot verify your identity."
            )
            assertThat(it.countryNotListedPage.cancelButtonText).isEqualTo(
                "Cancel verification"
            )
        }
    }

    @Test
    fun `postVerificationPageData returns VerificationPageData`() {
        val collectedDataParam = CollectedDataParam(
            biometricConsent = false
        )
        val clearDataParam = ClearDataParam()
        verifyPostVerificationPageData(
            targetPath = "$BASE_URL/$IDENTITY_VERIFICATION_PAGES/$TEST_ID/$DATA",
            apiCall = {
                identityRepository.postVerificationPageData(
                    TEST_ID,
                    TEST_EPHEMERAL_KEY,
                    collectedDataParam,
                    clearDataParam
                )
            }
        ) { request ->
            assertThat((request as ApiRequest).params).isEqualTo(
                mapOf(
                    collectedDataParam.createCollectedDataParamEntry(identityRepository.json),
                    clearDataParam.createCollectedDataParamEntry(identityRepository.json)
                )
            )
        }
    }

    @Test
    fun `postVerificationPageSubmit returns VerificationPageData`() {
        verifyPostVerificationPageData(
            targetPath = "$BASE_URL/$IDENTITY_VERIFICATION_PAGES/$TEST_ID/$SUBMIT",
            apiCall = {
                identityRepository.postVerificationPageSubmit(
                    TEST_ID,
                    TEST_EPHEMERAL_KEY
                )
            }
        ) { request ->
            assertThat((request as ApiRequest).params).isNull()
        }
    }

    @Test
    fun testVerifyTestVerificationSessionWithoutDelay() {
        testVerifyEndpoint(
            verify = true,
            simulateDelay = false
        )
    }

    @Test
    fun testVerifyTestVerificationSessionWithDelay() {
        testVerifyEndpoint(
            verify = true,
            simulateDelay = true
        )
    }

    @Test
    fun testUnverifyTestVerificationSessionWithoutDelay() {
        testVerifyEndpoint(
            verify = false,
            simulateDelay = false
        )
    }

    @Test
    fun testUnverifyTestVerificationSessionWithDelay() {
        testVerifyEndpoint(
            verify = false,
            simulateDelay = true
        )
    }

    @Test
    fun testGeneratePhoneOtpEndpoint() {
        verifyPostVerificationPageData(
            targetPath = "$BASE_URL/$IDENTITY_VERIFICATION_PAGES/$TEST_ID/$PHONE_OTP/$GENERATE",
            apiCall = {
                identityRepository.generatePhoneOtp(
                    id = TEST_ID,
                    ephemeralKey = TEST_EPHEMERAL_KEY
                )
            }
        )
    }

    @Test
    fun testCannotVerifyPhoneOtpEndpoint() {
        verifyPostVerificationPageData(
            targetPath = "$BASE_URL/$IDENTITY_VERIFICATION_PAGES/$TEST_ID/$PHONE_OTP/$CANNOT_VERIFY",
            apiCall =
            {
                identityRepository.cannotVerifyPhoneOtp(
                    id = TEST_ID,
                    ephemeralKey = TEST_EPHEMERAL_KEY
                )
            }
        )
    }

    @Test
    fun `retrieveVerificationPage with error response throws APIException from executeRequestWithKSerializer`() {
        runBlocking {
            whenever(mockStripeNetworkClient.executeRequest(any())).thenReturn(
                StripeResponse(
                    code = HTTP_UNAUTHORIZED,
                    body = ERROR_JSON_STRING
                )
            )
            assertFailsWith<APIException> {
                identityRepository.retrieveVerificationPage(
                    TEST_ID,
                    TEST_EPHEMERAL_KEY
                )
            }.let { apiException ->
                assertThat(apiException.stripeError).isEqualTo(
                    StripeErrorJsonParser().parse(JSONObject(ERROR_JSON_STRING))
                )
                assertThat(apiException.statusCode).isEqualTo(HTTP_UNAUTHORIZED)
            }
        }
    }

    @Test
    fun `retrieveVerificationPage with network failure throws APIConnectionException from executeRequestWithKSerializer`() {
        runBlocking {
            val networkException = APIConnectionException()
            whenever(mockStripeNetworkClient.executeRequest(any())).thenThrow(networkException)
            assertFailsWith<APIConnectionException> {
                identityRepository.retrieveVerificationPage(
                    TEST_ID,
                    TEST_EPHEMERAL_KEY
                )
            }.let { apiConnectionException ->
                verify(mockStripeNetworkClient).executeRequest(requestCaptor.capture())
                assertThat(apiConnectionException.message).isEqualTo("Failed to execute ${requestCaptor.firstValue}")
                assertThat(apiConnectionException.cause).isSameInstanceAs(networkException)
            }
        }
    }

    @Test
    fun `uploadImage returns InternalStripeFile`() {
        runBlocking {
            whenever(mockStripeNetworkClient.executeRequest(any())).thenReturn(
                StripeResponse(
                    code = HTTP_OK,
                    body = FILE_UPLOAD_SUCCESS_JSON_STRING
                )
            )
            val internalStripeFile = identityRepository.uploadImage(
                TEST_ID,
                TEST_EPHEMERAL_KEY,
                mock(),
                mock()
            )

            assertThat(internalStripeFile).isInstanceOf(StripeFile::class.java)
            verify(mockStripeNetworkClient).executeRequest(requestCaptor.capture())

            val request = requestCaptor.firstValue

            assertThat(request).isInstanceOf(IdentityFileUploadRequest::class.java)
            assertThat((request as IdentityFileUploadRequest).verificationId).isEqualTo(TEST_ID)
            assertThat(request.headers[HEADER_AUTHORIZATION]).isEqualTo("Bearer $TEST_EPHEMERAL_KEY")
        }
    }

    @Test
    fun `uploadImage with error response throws APIException from executeRequestWithModelJsonParser`() {
        runBlocking {
            whenever(mockStripeNetworkClient.executeRequest(any())).thenReturn(
                StripeResponse(
                    code = HTTP_UNAUTHORIZED,
                    body = ERROR_JSON_STRING
                )
            )
            assertFailsWith<APIException> {
                identityRepository.uploadImage(
                    TEST_ID,
                    TEST_EPHEMERAL_KEY,
                    mock(),
                    mock()
                )
            }.let { apiException ->
                assertThat(apiException.stripeError).isEqualTo(
                    StripeErrorJsonParser().parse(JSONObject(ERROR_JSON_STRING))
                )
                assertThat(apiException.statusCode).isEqualTo(HTTP_UNAUTHORIZED)
            }
        }
    }

    @Test
    fun `uploadImage with network failure throws APIConnectionException from executeRequestWithModelJsonParser`() {
        runBlocking {
            val networkException = APIConnectionException()
            whenever(mockStripeNetworkClient.executeRequest(any())).thenThrow(networkException)
            assertFailsWith<APIConnectionException> {
                identityRepository.uploadImage(
                    TEST_ID,
                    TEST_EPHEMERAL_KEY,
                    mock(),
                    mock()
                )
            }.let { apiConnectionException ->
                verify(mockStripeNetworkClient).executeRequest(requestCaptor.capture())
                assertThat(apiConnectionException.message).isEqualTo("Failed to execute ${requestCaptor.firstValue}")
                assertThat(apiConnectionException.cause).isSameInstanceAs(networkException)
            }
        }
    }

    @Test
    fun `downloadModel returns File`() {
        runBlocking {
            val mockFile = mock<File>()
            whenever(mockStripeNetworkClient.executeRequestForFile(any(), any())).thenReturn(
                StripeResponse(
                    code = HTTP_OK,
                    body = mockFile
                )
            )
            val downloadedModel = identityRepository.downloadModel(TEST_URL)

            assertThat(downloadedModel).isSameInstanceAs(mockFile)
            verify(mockStripeNetworkClient).executeRequestForFile(requestCaptor.capture(), any())

            val request = requestCaptor.firstValue

            assertThat(request).isInstanceOf(IdentityFileDownloadRequest::class.java)
            assertThat((request as IdentityFileDownloadRequest).url).isEqualTo(TEST_URL)
        }
    }

    @Test
    fun `downloadModel with error response throws APIException`() {
        runBlocking {
            whenever(mockStripeNetworkClient.executeRequestForFile(any(), any())).thenReturn(
                StripeResponse(
                    code = HTTP_UNAUTHORIZED,
                    body = mock()
                )
            )
            assertFailsWith<APIException> {
                identityRepository.downloadModel(TEST_URL)
            }.let { apiException ->
                assertThat(apiException.statusCode).isEqualTo(HTTP_UNAUTHORIZED)
                assertThat(apiException.message).isEqualTo(
                    "Downloading from $TEST_URL returns error response"
                )
            }
        }
    }

    @Test
    fun `downloadModel with network failure throws APIConnectionException`() {
        runBlocking {
            val networkException = APIConnectionException()
            whenever(mockStripeNetworkClient.executeRequestForFile(any(), any())).thenThrow(
                networkException
            )
            assertFailsWith<APIConnectionException> {
                identityRepository.downloadModel(TEST_URL)
            }.let { apiConnectionException ->
                verify(mockStripeNetworkClient).executeRequestForFile(
                    requestCaptor.capture(),
                    any()
                )
                assertThat(apiConnectionException.message).isEqualTo("Fail to download file at $TEST_URL")
                assertThat(apiConnectionException.cause).isSameInstanceAs(networkException)
            }
        }
    }

    private fun testFetchVerificationPage(
        body: String,
        verificationPageBlock: (VerificationPage) -> Unit
    ) {
        runBlocking {
            whenever(mockStripeNetworkClient.executeRequest(any())).thenReturn(
                StripeResponse(
                    code = HTTP_OK,
                    body = body
                )
            )
            val verificationPage = identityRepository.retrieveVerificationPage(
                TEST_ID,
                TEST_EPHEMERAL_KEY
            )

            assertThat(verificationPage).isInstanceOf(VerificationPage::class.java)
            verify(mockStripeNetworkClient).executeRequest(requestCaptor.capture())

            val request = requestCaptor.firstValue

            assertThat(request).isInstanceOf(ApiRequest::class.java)
            assertThat(request.method).isEqualTo(StripeRequest.Method.GET)
            assertThat(request.url).isEqualTo("$BASE_URL/$IDENTITY_VERIFICATION_PAGES/$TEST_ID")
            assertThat(request.headers[HEADER_AUTHORIZATION]).isEqualTo("Bearer $TEST_EPHEMERAL_KEY")

            verificationPageBlock(verificationPage)
        }
    }

    private fun testVerifyEndpoint(
        verify: Boolean,
        simulateDelay: Boolean
    ) {
        verifyPostVerificationPageData(
            targetPath = if (verify) {
                "$BASE_URL/$IDENTITY_VERIFICATION_PAGES/$TEST_ID/$TESTING/$VERIFY"
            } else {
                "$BASE_URL/$IDENTITY_VERIFICATION_PAGES/$TEST_ID/$TESTING/$UNVERIFY"
            },
            apiCall = {
                if (verify) {
                    identityRepository.verifyTestVerificationSession(
                        id = TEST_ID,
                        ephemeralKey = TEST_EPHEMERAL_KEY,
                        simulateDelay = simulateDelay
                    )
                } else {
                    identityRepository.unverifyTestVerificationSession(
                        id = TEST_ID,
                        ephemeralKey = TEST_EPHEMERAL_KEY,
                        simulateDelay = simulateDelay
                    )
                }
            }
        ) { request ->
            assertThat((request as ApiRequest).params).isEqualTo(
                mapOf(
                    SIMULATE_DELAY to simulateDelay
                )
            )
        }
    }

    private fun verifyPostVerificationPageData(
        targetPath: String,
        apiCall: suspend () -> VerificationPageData,
        additionalChecks: (StripeRequest) -> Unit = {}
    ) {
        runBlocking {
            whenever(mockStripeNetworkClient.executeRequest(any())).thenReturn(
                StripeResponse(
                    code = HTTP_OK,
                    body = VERIFICATION_PAGE_DATA_JSON_STRING
                )
            )
            val verificationPage = apiCall()

            assertThat(verificationPage).isInstanceOf(VerificationPageData::class.java)
            verify(mockStripeNetworkClient).executeRequest(requestCaptor.capture())

            val request = requestCaptor.firstValue

            assertThat(request).isInstanceOf(ApiRequest::class.java)
            assertThat(request.method).isEqualTo(StripeRequest.Method.POST)
            assertThat(request.url).isEqualTo(targetPath)

            assertThat(request.headers[HEADER_AUTHORIZATION]).isEqualTo("Bearer $TEST_EPHEMERAL_KEY")
            additionalChecks(request)
        }
    }

    private companion object {
        const val TEST_EPHEMERAL_KEY = "ek_test_12345"
        const val TEST_ID = "vs_12345"
        const val TEST_URL = "http://url/to/model.tflite"
    }
}
