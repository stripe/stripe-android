package com.stripe.android.stripecardscan.framework.api

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.networking.HEADER_AUTHORIZATION
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeRequest
import com.stripe.android.core.networking.StripeResponse
import com.stripe.android.stripecardscan.framework.api.StripeNetwork.Companion.RESPONSE_CODE_UNSET
import com.stripe.android.stripecardscan.framework.api.dto.CardScanFileDownloadRequest
import com.stripe.android.stripecardscan.framework.api.dto.CardScanRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.net.HttpURLConnection.HTTP_OK
import java.net.URL

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class StripeNetworkTest {
    @Serializable
    private data class TestRequest(@SerialName("request_id") val requestId: Int)

    @Serializable
    private data class TestResponse(@SerialName("response_id") val responseId: Int)

    @Serializable
    private data class TestServerErrorResponse(
        @SerialName("error_response_id") val errorResponseId: Int
    )

    private val mockStripeNetworkClient: StripeNetworkClient = mock()
    private val stripeNetwork = StripeNetwork(
        baseUrl = TEST_BASE_URL,
        retryStatusCodes = RETRY_STATUS_CODES,
        stripeNetworkClient = mockStripeNetworkClient
    )

    @Test
    fun `postForResult with ok response`() = runTest {
        whenever(mockStripeNetworkClient.executeRequest(any())).thenReturn(OK_RESPONSE)
        val networkResult = stripeNetwork.postForResult(
            TEST_PUBLISHABLE_KEY,
            TEST_PATH,
            TestRequest(REQUEST_ID),
            TestRequest.serializer(),
            TestResponse.serializer(),
            TestServerErrorResponse.serializer()
        )

        verify(mockStripeNetworkClient).executeRequest(
            argWhere { request ->
                request is CardScanRequest &&
                    request.method == StripeRequest.Method.POST &&
                    request.url == "$TEST_BASE_URL$TEST_PATH" &&
                    request.retryResponseCodes == RETRY_STATUS_CODES &&
                    request.headers[HEADER_AUTHORIZATION] == "Bearer $TEST_PUBLISHABLE_KEY" &&
                    request.encodedPostData == "request_id=$REQUEST_ID"
            }
        )
        assertThat(networkResult).isInstanceOf(NetworkResult.Success::class.java)

        val successResult = networkResult as NetworkResult.Success

        assertThat(successResult.responseCode).isEqualTo(HTTP_OK)
        assertThat(successResult.body.responseId).isEqualTo(OK_RESPONSE_ID)
    }

    @Test
    fun `postForResult with error response`() = runTest {
        whenever(mockStripeNetworkClient.executeRequest(any())).thenReturn(ERROR_RESPONSE)
        val networkResult = stripeNetwork.postForResult(
            TEST_PUBLISHABLE_KEY,
            TEST_PATH,
            TestRequest(REQUEST_ID),
            TestRequest.serializer(),
            TestResponse.serializer(),
            TestServerErrorResponse.serializer()
        )

        verify(mockStripeNetworkClient).executeRequest(
            argWhere { request ->
                request is CardScanRequest &&
                    request.method == StripeRequest.Method.POST &&
                    request.url == "$TEST_BASE_URL$TEST_PATH" &&
                    request.retryResponseCodes == RETRY_STATUS_CODES &&
                    request.headers[HEADER_AUTHORIZATION] == "Bearer $TEST_PUBLISHABLE_KEY" &&
                    request.encodedPostData == "request_id=$REQUEST_ID"
            }
        )

        assertThat(networkResult).isInstanceOf(NetworkResult.Error::class.java)

        val errorResult = networkResult as NetworkResult.Error

        assertThat(errorResult.responseCode).isEqualTo(HTTP_BAD_REQUEST)
        assertThat(errorResult.error.errorResponseId).isEqualTo(ERROR_RESPONSE_ID)
    }

    @Test
    fun `postForResult with exception thrown`() = runTest {
        val exception = APIConnectionException()
        whenever(mockStripeNetworkClient.executeRequest(any())).thenThrow(exception)

        val networkResult = stripeNetwork.postForResult(
            TEST_PUBLISHABLE_KEY,
            TEST_PATH,
            TestRequest(REQUEST_ID),
            TestRequest.serializer(),
            TestResponse.serializer(),
            TestServerErrorResponse.serializer()
        )

        verify(mockStripeNetworkClient).executeRequest(
            argWhere { request ->
                request is CardScanRequest &&
                    request.method == StripeRequest.Method.POST &&
                    request.url == "$TEST_BASE_URL$TEST_PATH" &&
                    request.retryResponseCodes == RETRY_STATUS_CODES &&
                    request.headers[HEADER_AUTHORIZATION] == "Bearer $TEST_PUBLISHABLE_KEY" &&
                    request.encodedPostData == "request_id=$REQUEST_ID"
            }
        )

        assertThat(networkResult).isInstanceOf(NetworkResult.Exception::class.java)

        val exceptionResult = networkResult as NetworkResult.Exception

        assertThat(exceptionResult.responseCode).isEqualTo(RESPONSE_CODE_UNSET)
        assertThat(exceptionResult.exception).isSameInstanceAs(exception)
    }

    @Test
    fun `postData with ok response`() = runTest {
        whenever(mockStripeNetworkClient.executeRequest(any())).thenReturn(OK_RESPONSE)
        stripeNetwork.postData(
            TEST_PUBLISHABLE_KEY,
            TEST_PATH,
            TestRequest(REQUEST_ID),
            TestRequest.serializer()
        )

        verify(mockStripeNetworkClient).executeRequest(
            argWhere { request ->
                request is CardScanRequest &&
                    request.method == StripeRequest.Method.POST &&
                    request.url == "$TEST_BASE_URL$TEST_PATH" &&
                    request.retryResponseCodes == RETRY_STATUS_CODES &&
                    request.headers[HEADER_AUTHORIZATION] == "Bearer $TEST_PUBLISHABLE_KEY" &&
                    request.encodedPostData == "request_id=$REQUEST_ID"
            }
        )
    }

    @Test
    fun `getForResult with ok response`() = runTest {
        whenever(mockStripeNetworkClient.executeRequest(any())).thenReturn(OK_RESPONSE)
        val networkResult = stripeNetwork.getForResult(
            TEST_PUBLISHABLE_KEY,
            TEST_PATH,
            TestResponse.serializer(),
            TestServerErrorResponse.serializer()
        )

        verify(mockStripeNetworkClient).executeRequest(
            argWhere { request ->
                request is CardScanRequest &&
                    request.method == StripeRequest.Method.GET &&
                    request.url == "$TEST_BASE_URL$TEST_PATH" &&
                    request.retryResponseCodes == RETRY_STATUS_CODES &&
                    request.headers[HEADER_AUTHORIZATION] == "Bearer $TEST_PUBLISHABLE_KEY"
            }
        )
        assertThat(networkResult).isInstanceOf(NetworkResult.Success::class.java)

        val successResult = networkResult as NetworkResult.Success

        assertThat(successResult.responseCode).isEqualTo(HTTP_OK)
        assertThat(successResult.body.responseId).isEqualTo(OK_RESPONSE_ID)
    }

    @Test
    fun `getForResult with error response`() = runTest {
        whenever(mockStripeNetworkClient.executeRequest(any())).thenReturn(ERROR_RESPONSE)
        val networkResult = stripeNetwork.getForResult(
            TEST_PUBLISHABLE_KEY,
            TEST_PATH,
            TestResponse.serializer(),
            TestServerErrorResponse.serializer()
        )

        verify(mockStripeNetworkClient).executeRequest(
            argWhere { request ->
                request is CardScanRequest &&
                    request.method == StripeRequest.Method.GET &&
                    request.url == "$TEST_BASE_URL$TEST_PATH" &&
                    request.retryResponseCodes == RETRY_STATUS_CODES &&
                    request.headers[HEADER_AUTHORIZATION] == "Bearer $TEST_PUBLISHABLE_KEY"
            }
        )

        assertThat(networkResult).isInstanceOf(NetworkResult.Error::class.java)

        val errorResult = networkResult as NetworkResult.Error

        assertThat(errorResult.responseCode).isEqualTo(HTTP_BAD_REQUEST)
        assertThat(errorResult.error.errorResponseId).isEqualTo(ERROR_RESPONSE_ID)
    }

    @Test
    fun `getForResult with exception thrown`() = runTest {
        val exception = APIConnectionException()
        whenever(mockStripeNetworkClient.executeRequest(any())).thenThrow(exception)

        val networkResult = stripeNetwork.getForResult(
            TEST_PUBLISHABLE_KEY,
            TEST_PATH,
            TestResponse.serializer(),
            TestServerErrorResponse.serializer()
        )

        verify(mockStripeNetworkClient).executeRequest(
            argWhere { request ->
                request is CardScanRequest &&
                    request.method == StripeRequest.Method.GET &&
                    request.url == "$TEST_BASE_URL$TEST_PATH" &&
                    request.retryResponseCodes == RETRY_STATUS_CODES &&
                    request.headers[HEADER_AUTHORIZATION] == "Bearer $TEST_PUBLISHABLE_KEY"
            }
        )

        assertThat(networkResult).isInstanceOf(NetworkResult.Exception::class.java)

        val exceptionResult = networkResult as NetworkResult.Exception

        assertThat(exceptionResult.responseCode).isEqualTo(RESPONSE_CODE_UNSET)
        assertThat(exceptionResult.exception).isSameInstanceAs(exception)
    }

    @Test
    fun `downloadFileWithRetries succeeds`() = runTest {
        val mockFile = mock<File>()

        whenever(mockStripeNetworkClient.executeRequestForFile(any(), any())).thenReturn(
            StripeResponse(
                code = HTTP_OK,
                body = mockFile
            )
        )

        val responseCode = stripeNetwork.downloadFileWithRetries(
            TEST_FILE_URL,
            mockFile
        )

        verify(mockStripeNetworkClient).executeRequestForFile(
            argWhere { request ->
                request is CardScanFileDownloadRequest &&
                    request.method == StripeRequest.Method.GET &&
                    request.url == TEST_FILE_URL.path &&
                    request.retryResponseCodes == CARD_SCAN_RETRY_STATUS_CODES &&
                    request.headers.isEmpty()
            },
            argWhere {
                it == mockFile
            }
        )

        assertThat(responseCode).isEqualTo(HTTP_OK)
    }

    @Test
    fun `downloadFileWithRetries with exception thrown`() = runTest {
        val mockFile = mock<File>()

        whenever(mockStripeNetworkClient.executeRequestForFile(any(), any())).thenThrow(
            APIConnectionException()
        )

        val responseCode = stripeNetwork.downloadFileWithRetries(
            TEST_FILE_URL,
            mockFile
        )

        verify(mockStripeNetworkClient).executeRequestForFile(
            argWhere { request ->
                request is CardScanFileDownloadRequest &&
                    request.method == StripeRequest.Method.GET &&
                    request.url == TEST_FILE_URL.path &&
                    request.retryResponseCodes == CARD_SCAN_RETRY_STATUS_CODES &&
                    request.headers.isEmpty()
            },
            argWhere {
                it == mockFile
            }
        )

        assertThat(responseCode).isEqualTo(RESPONSE_CODE_UNSET)
    }

    private companion object {
        const val TEST_BASE_URL = "https://www.test_base_url.com/v1"
        const val TEST_PATH = "/endpoint1"
        const val TEST_PUBLISHABLE_KEY = "pk_test"

        val TEST_FILE_URL = URL("https://www.some_host.com/file_to_download")
        val RETRY_STATUS_CODES = 500..510

        const val REQUEST_ID = 23

        const val OK_RESPONSE_ID = 123
        val OK_RESPONSE = StripeResponse(
            code = HTTP_OK,
            body = "{\"response_id\": $OK_RESPONSE_ID}"
        )

        const val ERROR_RESPONSE_ID = 456
        val ERROR_RESPONSE = StripeResponse(
            code = HTTP_BAD_REQUEST,
            body = "{\"error_response_id\": $ERROR_RESPONSE_ID}"
        )
    }
}
