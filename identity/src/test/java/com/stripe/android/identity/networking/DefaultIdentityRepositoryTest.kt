package com.stripe.android.identity.networking

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.model.InternalStripeFile
import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import com.stripe.android.core.networking.HEADER_AUTHORIZATION
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeRequest
import com.stripe.android.core.networking.StripeResponse
import com.stripe.android.identity.networking.PostVerificationPageDataRequest.Companion.DATA
import com.stripe.android.identity.networking.PostVerificationPageSubmitRequest.Companion.SUBMIT
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.CollectedDataParam.Companion.createCollectedDataParam
import com.stripe.android.identity.networking.models.ConsentParam
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageData
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

    private val mockStripeNetworkClient: StripeNetworkClient = mock()
    private val identityRepository = DefaultIdentityRepository(
        ApplicationProvider.getApplicationContext(),
        mockStripeNetworkClient
    )

    private val requestCaptor: KArgumentCaptor<StripeRequest> = argumentCaptor()

    @Test
    fun `retrieveVerificationPage returns VerificationPage`() {
        runBlocking {
            whenever(mockStripeNetworkClient.executeRequest(any())).thenReturn(
                StripeResponse(
                    code = HTTP_OK,
                    body = VERIFICATION_PAGE_JSON_STRING
                )
            )
            val verificationPage = identityRepository.retrieveVerificationPage(
                TEST_ID,
                TEST_EPHEMERAL_KEY
            )

            assertThat(verificationPage).isInstanceOf(VerificationPage::class.java)
            verify(mockStripeNetworkClient).executeRequest(requestCaptor.capture())

            val request = requestCaptor.firstValue

            assertThat(request).isInstanceOf(RetrieveVerificationPageRequest::class.java)
            assertThat(request.url).isEqualTo("$BASE_URL/$IDENTITY_VERIFICATION_PAGES/$TEST_ID")
            assertThat(request.headers[HEADER_AUTHORIZATION]).isEqualTo("Bearer $TEST_EPHEMERAL_KEY")
        }
    }

    @Test
    fun `postVerificationPageData returns VerificationPageData`() {
        runBlocking {
            whenever(mockStripeNetworkClient.executeRequest(any())).thenReturn(
                StripeResponse(
                    code = HTTP_OK,
                    body = VERIFICATION_PAGE_DATA_JSON_STRING
                )
            )
            val collectedDataParam = CollectedDataParam(
                consent = ConsentParam(
                    biometric = false
                )
            )
            val verificationPage = identityRepository.postVerificationPageData(
                TEST_ID,
                TEST_EPHEMERAL_KEY,
                collectedDataParam
            )

            assertThat(verificationPage).isInstanceOf(VerificationPageData::class.java)
            verify(mockStripeNetworkClient).executeRequest(requestCaptor.capture())

            val request = requestCaptor.firstValue

            assertThat(request).isInstanceOf(PostVerificationPageDataRequest::class.java)
            assertThat(request.url).isEqualTo("$BASE_URL/$IDENTITY_VERIFICATION_PAGES/$TEST_ID/$DATA")
            assertThat(request.headers[HEADER_AUTHORIZATION]).isEqualTo("Bearer $TEST_EPHEMERAL_KEY")
            assertThat((request as PostVerificationPageDataRequest).encodedData).isEqualTo(
                collectedDataParam.createCollectedDataParam(identityRepository.json)
            )
        }
    }

    @Test
    fun `postVerificationPageSubmit returns VerificationPageData`() {
        runBlocking {
            whenever(mockStripeNetworkClient.executeRequest(any())).thenReturn(
                StripeResponse(
                    code = HTTP_OK,
                    body = VERIFICATION_PAGE_DATA_JSON_STRING
                )
            )
            val verificationPage = identityRepository.postVerificationPageSubmit(
                TEST_ID,
                TEST_EPHEMERAL_KEY
            )

            assertThat(verificationPage).isInstanceOf(VerificationPageData::class.java)
            verify(mockStripeNetworkClient).executeRequest(requestCaptor.capture())

            val request = requestCaptor.firstValue

            assertThat(request).isInstanceOf(PostVerificationPageSubmitRequest::class.java)
            assertThat(request.url).isEqualTo("$BASE_URL/$IDENTITY_VERIFICATION_PAGES/$TEST_ID/$SUBMIT")
            assertThat(request.headers[HEADER_AUTHORIZATION]).isEqualTo("Bearer $TEST_EPHEMERAL_KEY")
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

            assertThat(internalStripeFile).isInstanceOf(InternalStripeFile::class.java)
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

            assertThat(request).isInstanceOf(IdentityModelDownloadRequest::class.java)
            assertThat((request as IdentityModelDownloadRequest).url).isEqualTo(TEST_URL)
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

    private companion object {
        const val TEST_EPHEMERAL_KEY = "ek_test_12345"
        const val TEST_ID = "vs_12345"
        const val TEST_URL = "http://url/to/model.tflite"
    }
}
