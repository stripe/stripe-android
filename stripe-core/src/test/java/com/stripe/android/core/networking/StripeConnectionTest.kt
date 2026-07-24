package com.stripe.android.core.networking

import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.FileReader
import kotlin.test.AfterTest

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
        val response = buildResponse(expectedStringValue)

        val connection = StripeConnection.Default(response)

        assertThat(connection.response.body).isEqualTo(expectedStringValue)
    }

    @Test
    fun `FileConnection correctly reads File from responseStream`() {
        val expectedFileContent = "test file content"
        val response = buildResponse(expectedFileContent)

        val outputFile =
            File(InstrumentationRegistry.getInstrumentation().context.cacheDir, TEST_FILE_NAME)
        if (outputFile.exists()) {
            outputFile.delete()
        }

        val connection = StripeConnection.FileConnection(response, outputFile)
        assertThat(connection.response.body).isSameInstanceAs(outputFile)
        assertThat(FileReader(outputFile).readText()).isEqualTo(expectedFileContent)
    }

    private fun buildResponse(body: String, code: Int = 200): Response {
        return Response.Builder()
            .request(Request.Builder().url("https://api.stripe.com/v1/test").build())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("OK")
            .body(body.toResponseBody("application/json".toMediaType()))
            .build()
    }

    private companion object {
        const val TEST_FILE_NAME = "testFile"
    }
}
