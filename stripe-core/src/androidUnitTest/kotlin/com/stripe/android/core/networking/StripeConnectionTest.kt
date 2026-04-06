package com.stripe.android.core.networking

import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import kotlin.test.AfterTest
import okio.Path.Companion.toOkioPath

@RunWith(RobolectricTestRunner::class)
class StripeConnectionTest {

    @AfterTest
    fun cleanUpTestFile() {
        File(
            InstrumentationRegistry.getInstrumentation().context.cacheDir,
            TEST_FILE_NAME
        ).deleteOnExit()
    }

    @Test
    fun `Default correctly reads String from responseStream`() {
        val expectedStringValue = "test input stream value"
        withStripeConnection(expectedStringValue) { connection ->
            assertThat(connection.response.body).isEqualTo(expectedStringValue)
        }
    }

    @Test
    fun `Default returns null for empty responseStream`() {
        withStripeConnection("") { connection ->
            assertThat(connection.response.body).isNull()
        }
    }

    @Test
    fun `FileConnection correctly reads File from responseStream`() {
        val expectedFileContent = "test file content"
        val outputFile =
            File(InstrumentationRegistry.getInstrumentation().context.cacheDir, TEST_FILE_NAME)
        if (outputFile.exists()) {
            outputFile.delete()
        }
        val outputPath = outputFile.toOkioPath()

        withStripeKtorConnection(expectedFileContent) { ktorConnection ->
            val connection = StripeConnection.FileConnection(ktorConnection, outputPath)
            assertThat(connection.response.body).isEqualTo(outputPath)
            assertThat(outputFile.readText()).isEqualTo(expectedFileContent)
        }
    }

    private fun withStripeConnection(
        responseBody: String,
        block: (StripeConnection.Default) -> Unit
    ) {
        withStripeKtorConnection(responseBody) { ktorConnection ->
            block(StripeConnection.Default(ktorConnection))
        }
    }

    private fun withStripeKtorConnection(
        responseBody: String,
        block: (ConnectionFactory.StripeKtorConnection) -> Unit
    ) {
        val server = MockWebServer()
        val client = HttpClient(OkHttp)
        try {
            server.start()
            server.enqueue(MockResponse().setBody(responseBody))
            val response = runBlocking {
                client.get(server.url("/").toString())
            }
            block(ConnectionFactory.StripeKtorConnection(client, response))
        } finally {
            client.close()
            server.shutdown()
        }
    }

    private companion object {
        const val TEST_FILE_NAME = "testFile"
    }
}
