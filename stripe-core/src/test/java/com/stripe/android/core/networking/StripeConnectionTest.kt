package com.stripe.android.core.networking

import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileReader
import javax.net.ssl.HttpsURLConnection
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
        val mockConnection = mock<HttpsURLConnection>()
        whenever(mockConnection.responseCode).thenReturn(HttpsURLConnection.HTTP_OK)

        val expectedStringValue = "test input stream value"
        whenever(mockConnection.inputStream).thenReturn(ByteArrayInputStream(expectedStringValue.toByteArray()))

        val connection = StripeConnection.Default(mockConnection)

        assertThat(connection.response.body).isEqualTo(expectedStringValue)
    }

    @Test
    fun `FileConnection correctly reads File from responseStream`() {
        val mockConnection = mock<HttpsURLConnection>()
        whenever(mockConnection.responseCode).thenReturn(HttpsURLConnection.HTTP_OK)

        val expectedFileContent = "test file content"
        whenever(mockConnection.inputStream).thenReturn(ByteArrayInputStream(expectedFileContent.toByteArray()))

        val outputFile =
            File(InstrumentationRegistry.getInstrumentation().context.cacheDir, TEST_FILE_NAME)
        if (outputFile.exists()) {
            outputFile.delete()
        }

        val connection = StripeConnection.FileConnection(mockConnection, outputFile)
        assertThat(connection.response.body).isSameInstanceAs(outputFile)
        assertThat(FileReader(outputFile).readText()).isEqualTo(expectedFileContent)
    }

    private companion object {
        const val TEST_FILE_NAME = "testFile"
    }
}
