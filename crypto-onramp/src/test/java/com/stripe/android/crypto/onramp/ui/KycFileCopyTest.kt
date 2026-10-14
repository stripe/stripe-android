package com.stripe.android.crypto.onramp.ui

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

internal class KycFileCopyTest {
    @Test
    fun `cancellation during a read stops before writing the returned bytes`() = runTest {
        val job = Job()
        val output = ByteArrayOutputStream()
        val input = object : ByteArrayInputStream(ByteArray(20_000)) {
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                return super.read(buffer, offset, length).also { job.cancel() }
            }
        }
        val error = runCatching {
            withContext(job) { copyKycFile(input, output, null) }
        }.exceptionOrNull()
        assertThat(error).isInstanceOf(CancellationException::class.java)
        assertThat(output.size()).isEqualTo(0)
        assertThat(input.available()).isGreaterThan(0)
    }

    @Test
    fun `file at size limit is copied`() = runTest {
        val inputBytes = ByteArray(16) { index -> index.toByte() }
        val output = ByteArrayOutputStream()

        copyKycFile(
            input = ByteArrayInputStream(inputBytes),
            output = output,
            maximumFileSizeBytes = 16,
        )

        assertThat(output.toByteArray()).isEqualTo(inputBytes)
    }

    @Test
    fun `file above size limit stops before writing beyond limit`() = runTest {
        val output = ByteArrayOutputStream()

        val error = runCatching {
            copyKycFile(
                input = ByteArrayInputStream(ByteArray(17)),
                output = output,
                maximumFileSizeBytes = 16,
            )
        }.exceptionOrNull()

        assertThat(error).isInstanceOf(KycFileTooLargeException::class.java)
        assertThat(output.size()).isAtMost(16)
    }

    @Test
    fun `file is copied when no size limit is configured`() = runTest {
        val inputBytes = ByteArray(17)
        val output = ByteArrayOutputStream()

        copyKycFile(
            input = ByteArrayInputStream(inputBytes),
            output = output,
            maximumFileSizeBytes = null,
        )

        assertThat(output.size()).isEqualTo(inputBytes.size)
    }
}
