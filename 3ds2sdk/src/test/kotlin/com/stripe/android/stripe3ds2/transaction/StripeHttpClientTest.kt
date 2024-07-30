package com.stripe.android.stripe3ds2.transaction

import com.google.common.truth.Truth.assertThat
import com.stripe.android.stripe3ds2.exceptions.SDKRuntimeException
import com.stripe.android.stripe3ds2.observability.FakeErrorReporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.HttpURLConnection
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class StripeHttpClientTest {
    private val testDispatcher = StandardTestDispatcher()
    private val errorReporter = FakeErrorReporter()
    private val client = StripeHttpClient(
        URL,
        errorReporter = errorReporter,
        workContext = testDispatcher
    )
    private val conn = mock<HttpURLConnection>()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun handlePostResponse_withSuccessfulResponse() {
        whenever(conn.responseCode)
            .thenReturn(HttpURLConnection.HTTP_OK)
        whenever(conn.contentType)
            .thenReturn("application/json")
        whenever(conn.inputStream)
            .thenReturn(ByteArrayInputStream(JSON.toByteArray()))
        val httpResponse = client.handlePostResponse(conn)
        assertThat(httpResponse.content)
            .isEqualTo(JSON)
        assertThat(httpResponse.isJsonContentType)
            .isTrue()
    }

    @Test
    fun handlePostResponse_withUnsuccessfulRequest() {
        whenever(conn.responseCode)
            .thenReturn(HttpURLConnection.HTTP_BAD_REQUEST)
        val exception = assertFailsWith<SDKRuntimeException> {
            client.handlePostResponse(conn)
        }
        assertThat(exception.message)
            .isEqualTo("Unsuccessful response code from https://example.com: 400")
    }

    @Test
    fun `doGetRequest when connection throws an exception should return null`() {
        val failingConnectionFactory = object : StripeHttpClient.ConnectionFactory {
            override fun create(url: String): HttpURLConnection {
                throw IOException("Connection failure")
            }
        }

        val client = StripeHttpClient(
            URL,
            connectionFactory = failingConnectionFactory,
            errorReporter = errorReporter,
            workContext = testDispatcher
        )

        runTest {
            assertThat(client.doGetRequest())
                .isNull()
        }
    }

    private companion object {
        private const val URL = "https://example.com"
        private const val JSON = "{\"hello\": \"world\"}"
    }
}
