package com.stripe.android.crypto.onramp.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

internal class KycFileCopyTest {
    @Test
    fun `file at size limit is copied`() {
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
    fun `file above size limit stops before writing beyond limit`() {
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
    fun `file is copied when no size limit is configured`() {
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
