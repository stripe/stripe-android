package com.stripe.android.core.networking

import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.model.StripeModel
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import kotlinx.coroutines.test.runTest
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.net.HttpURLConnection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@Serializable
private data class TestModel(val id: String, val value: Int)

@Parcelize
private data class TestStripeModel(val data: String) : StripeModel

internal class RequestExecutorTest {

    private val mockNetworkClient = mock<StripeNetworkClient>()
    private val mockErrorParser = mock<StripeErrorJsonParser>()
    private val mockModelParser = mock<ModelJsonParser<TestStripeModel>>()
    private val testRequest = FakeStripeRequest()

    private val successResponse = StripeResponse<String>(
        code = HttpURLConnection.HTTP_OK,
        body = """{"id": "test_id", "value": 42}""",
        headers = mapOf("Request-Id" to listOf("req_test"))
    )

    private val errorResponse = StripeResponse<String>(
        code = HttpURLConnection.HTTP_BAD_REQUEST,
        body = """{"error": {"message": "Test error"}}""",
        headers = mapOf("Request-Id" to listOf("req_error"))
    )

    private val testStripeError = StripeError(
        message = "Test error",
        type = "api_error"
    )

    @Test
    fun `executeRequestWithModelJsonParser should return parsed model on success`() = runTest {
        val expectedModel = TestStripeModel("test_data")
        whenever(mockNetworkClient.executeRequest(testRequest)).thenReturn(successResponse)
        whenever(mockModelParser.parse(any())).thenReturn(expectedModel)

        val result = executeRequestWithModelJsonParser(
            stripeNetworkClient = mockNetworkClient,
            stripeErrorJsonParser = mockErrorParser,
            request = testRequest,
            responseJsonParser = mockModelParser
        )

        assertEquals(expectedModel, result)
    }

    @Test
    fun `executeRequestWithModelJsonParser should throw APIException on error response`() = runTest {
        whenever(mockNetworkClient.executeRequest(testRequest)).thenReturn(errorResponse)
        whenever(mockErrorParser.parse(any())).thenReturn(testStripeError)

        val exception = assertFailsWith<APIException> {
            executeRequestWithModelJsonParser(
                stripeNetworkClient = mockNetworkClient,
                stripeErrorJsonParser = mockErrorParser,
                request = testRequest,
                responseJsonParser = mockModelParser
            )
        }

        assertEquals("req_error", exception.requestId)
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, exception.statusCode)
        assertEquals(testStripeError, exception.stripeError)
    }

    @Test
    fun `executeRequestWithModelJsonParser should throw APIException on null parse result`() = runTest {
        whenever(mockNetworkClient.executeRequest(testRequest)).thenReturn(successResponse)
        whenever(mockModelParser.parse(any())).thenReturn(null)

        val exception = assertFailsWith<APIException> {
            executeRequestWithModelJsonParser(
                stripeNetworkClient = mockNetworkClient,
                stripeErrorJsonParser = mockErrorParser,
                request = testRequest,
                responseJsonParser = mockModelParser
            )
        }

        assertTrue(exception.message!!.contains("returns null for"))
    }

    @Test
    fun `executeRequestWithModelJsonParser should throw APIConnectionException on network failure`() = runTest {
        val networkException = RuntimeException("Network error")
        whenever(mockNetworkClient.executeRequest(testRequest)).thenThrow(networkException)

        val exception = assertFailsWith<APIConnectionException> {
            executeRequestWithModelJsonParser(
                stripeNetworkClient = mockNetworkClient,
                stripeErrorJsonParser = mockErrorParser,
                request = testRequest,
                responseJsonParser = mockModelParser
            )
        }

        assertTrue(exception.message!!.contains("Failed to execute"))
        assertEquals(networkException, exception.cause)
    }

    @Test
    fun `executeRequestWithResultParser should return success result with parsed model`() = runTest {
        val expectedModel = TestStripeModel("test_data")
        whenever(mockNetworkClient.executeRequest(testRequest)).thenReturn(successResponse)
        whenever(mockModelParser.parse(any())).thenReturn(expectedModel)

        val result = executeRequestWithResultParser(
            stripeNetworkClient = mockNetworkClient,
            stripeErrorJsonParser = mockErrorParser,
            request = testRequest,
            responseJsonParser = mockModelParser
        )

        assertTrue(result.isSuccess)
        assertEquals(expectedModel, result.getOrThrow())
    }

    @Test
    fun `executeRequestWithResultParser should return failure result on error response`() = runTest {
        whenever(mockNetworkClient.executeRequest(testRequest)).thenReturn(errorResponse)
        whenever(mockErrorParser.parse(any())).thenReturn(testStripeError)

        val result = executeRequestWithResultParser(
            stripeNetworkClient = mockNetworkClient,
            stripeErrorJsonParser = mockErrorParser,
            request = testRequest,
            responseJsonParser = mockModelParser
        )

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as APIException
        assertEquals("req_error", exception.requestId)
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, exception.statusCode)
        assertEquals(testStripeError, exception.stripeError)
    }

    @Test
    fun `executeRequestWithResultParser should return failure result on null parse result`() = runTest {
        whenever(mockNetworkClient.executeRequest(testRequest)).thenReturn(successResponse)
        whenever(mockModelParser.parse(any())).thenReturn(null)

        val result = executeRequestWithResultParser(
            stripeNetworkClient = mockNetworkClient,
            stripeErrorJsonParser = mockErrorParser,
            request = testRequest,
            responseJsonParser = mockModelParser
        )

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as APIException
        assertTrue(exception.message!!.contains("returns null for"))
    }

    @Test
    fun `executeRequestWithResultParser should return failure result on network failure`() = runTest {
        val networkException = RuntimeException("Network error")
        whenever(mockNetworkClient.executeRequest(testRequest)).thenThrow(networkException)

        val result = executeRequestWithResultParser(
            stripeNetworkClient = mockNetworkClient,
            stripeErrorJsonParser = mockErrorParser,
            request = testRequest,
            responseJsonParser = mockModelParser
        )

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as APIConnectionException
        assertTrue(exception.message!!.contains("Failed to execute"))
        assertEquals(networkException, exception.cause)
    }

    @Test
    fun `executeRequestWithErrorParsing should return success result on successful response`() = runTest {
        whenever(mockNetworkClient.executeRequest(testRequest)).thenReturn(successResponse)

        val result = executeRequestWithErrorParsing(
            stripeNetworkClient = mockNetworkClient,
            stripeErrorJsonParser = mockErrorParser,
            request = testRequest
        )

        assertTrue(result.isSuccess)
        assertEquals(Unit, result.getOrThrow())
    }

    @Test
    fun `executeRequestWithErrorParsing should return failure result on error response`() = runTest {
        whenever(mockNetworkClient.executeRequest(testRequest)).thenReturn(errorResponse)
        whenever(mockErrorParser.parse(any())).thenReturn(testStripeError)

        val result = executeRequestWithErrorParsing(
            stripeNetworkClient = mockNetworkClient,
            stripeErrorJsonParser = mockErrorParser,
            request = testRequest
        )

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as APIException
        assertEquals("req_error", exception.requestId)
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, exception.statusCode)
        assertEquals(testStripeError, exception.stripeError)
    }

    @Test
    fun `executeRequestWithErrorParsing should return failure result on network failure`() = runTest {
        val networkException = RuntimeException("Network error")
        whenever(mockNetworkClient.executeRequest(testRequest)).thenThrow(networkException)

        val result = executeRequestWithErrorParsing(
            stripeNetworkClient = mockNetworkClient,
            stripeErrorJsonParser = mockErrorParser,
            request = testRequest
        )

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as APIConnectionException
        assertTrue(exception.message!!.contains("Failed to execute"))
        assertEquals(networkException, exception.cause)
    }

    @Test
    fun `executeRequestWithKSerializerParser should return success result with deserialized model`() = runTest {
        whenever(mockNetworkClient.executeRequest(testRequest)).thenReturn(successResponse)

        val result = executeRequestWithKSerializerParser(
            stripeNetworkClient = mockNetworkClient,
            stripeErrorJsonParser = mockErrorParser,
            request = testRequest,
            responseSerializer = TestModel.serializer()
        )

        assertTrue(result.isSuccess)
        val model = result.getOrThrow()
        assertEquals("test_id", model.id)
        assertEquals(42, model.value)
    }

    @Test
    fun `executeRequestWithKSerializerParser should return failure result on error response`() = runTest {
        whenever(mockNetworkClient.executeRequest(testRequest)).thenReturn(errorResponse)
        whenever(mockErrorParser.parse(any())).thenReturn(testStripeError)

        val result = executeRequestWithKSerializerParser(
            stripeNetworkClient = mockNetworkClient,
            stripeErrorJsonParser = mockErrorParser,
            request = testRequest,
            responseSerializer = TestModel.serializer()
        )

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as APIException
        assertEquals("req_error", exception.requestId)
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, exception.statusCode)
        assertEquals(testStripeError, exception.stripeError)
    }

    @Test
    fun `executeRequestWithKSerializerParser should return failure result on deserialization failure`() = runTest {
        val invalidJsonResponse = StripeResponse(
            code = HttpURLConnection.HTTP_OK,
            body = """{"invalid": "json_structure"}""",
            headers = mapOf("Request-Id" to listOf("req_test"))
        )
        whenever(mockNetworkClient.executeRequest(testRequest)).thenReturn(invalidJsonResponse)

        val result = executeRequestWithKSerializerParser(
            stripeNetworkClient = mockNetworkClient,
            stripeErrorJsonParser = mockErrorParser,
            request = testRequest,
            responseSerializer = TestModel.serializer()
        )

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as APIException
        assertTrue(exception.message!!.contains("Failed to parse response JSON for"))
    }

    @Test
    fun `executeRequestWithKSerializerParser should return failure result on null response body`() = runTest {
        val nullBodyResponse = StripeResponse<String>(
            code = HttpURLConnection.HTTP_OK,
            body = null,
            headers = mapOf("Request-Id" to listOf("req_test"))
        )
        whenever(mockNetworkClient.executeRequest(testRequest)).thenReturn(nullBodyResponse)

        val result = executeRequestWithKSerializerParser(
            stripeNetworkClient = mockNetworkClient,
            stripeErrorJsonParser = mockErrorParser,
            request = testRequest,
            responseSerializer = TestModel.serializer()
        )

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as APIException
        assertTrue(exception.message!!.contains("Failed to parse response JSON for null"))
    }

    @Test
    fun `executeRequestWithKSerializerParser should return failure result on network failure`() = runTest {
        val networkException = RuntimeException("Network error")
        whenever(mockNetworkClient.executeRequest(testRequest)).thenThrow(networkException)

        val result = executeRequestWithKSerializerParser(
            stripeNetworkClient = mockNetworkClient,
            stripeErrorJsonParser = mockErrorParser,
            request = testRequest,
            responseSerializer = TestModel.serializer()
        )

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as APIConnectionException
        assertTrue(exception.message!!.contains("Failed to execute"))
        assertEquals(networkException, exception.cause)
    }

    @Test
    fun `executeRequestWithKSerializerParser should work with custom Json instance`() = runTest {
        val customJson = Json { ignoreUnknownKeys = true }
        val responseWithExtraFields = StripeResponse(
            code = HttpURLConnection.HTTP_OK,
            body = """{"id": "test_id", "value": 42, "extra_field": "ignored"}""",
            headers = mapOf("Request-Id" to listOf("req_test"))
        )
        whenever(mockNetworkClient.executeRequest(testRequest)).thenReturn(responseWithExtraFields)

        val result = executeRequestWithKSerializerParser(
            stripeNetworkClient = mockNetworkClient,
            stripeErrorJsonParser = mockErrorParser,
            request = testRequest,
            responseSerializer = TestModel.serializer(),
            json = customJson
        )

        assertTrue(result.isSuccess)
        val model = result.getOrThrow()
        assertEquals("test_id", model.id)
        assertEquals(42, model.value)
    }
}
