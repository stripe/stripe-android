package com.stripe.android.identity.networking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.model.Country
import com.stripe.android.core.model.CountryCode
import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.HEADER_AUTHORIZATION
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeRequest
import com.stripe.android.core.networking.StripeResponse
import com.stripe.android.identity.networking.models.Requirement
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageIconType
import com.stripe.android.identity.networking.models.VerificationPageStaticConsentLineContent
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
import java.net.HttpURLConnection.HTTP_OK
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
class DefaultIdentityRepositoryTest {
    private val mockStripeNetworkClient: StripeNetworkClient = mock()
    private val identityRepository = DefaultIdentityRepository(
        mockStripeNetworkClient,
        ApplicationProvider.getApplicationContext()
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
            assertThat(request.url).isEqualTo(
                "$BASE_URL/$IDENTITY_VERIFICATION_PAGES/$TEST_ID?" +
                    "$APP_IDENTIFIER=${ApplicationProvider.getApplicationContext<Context>().packageName}"
            )
            assertThat(request.headers[HEADER_AUTHORIZATION]).isEqualTo("Bearer $TEST_EPHEMERAL_KEY")

            verificationPageBlock(verificationPage)
        }
    }

    private companion object {
        const val TEST_EPHEMERAL_KEY = "ek_test_12345"
        const val TEST_ID = "vs_12345"
    }
}
