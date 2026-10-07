package com.stripe.android.crypto.onramp.ui

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

internal fun copyAdditionalKycFile(
    input: InputStream,
    output: OutputStream,
    maximumFileSizeBytes: Long?,
) {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var copiedBytes = 0L

    while (true) {
        val bytesRead = input.read(buffer)
        if (bytesRead == -1) {
            return
        }

        copiedBytes += bytesRead
        if (maximumFileSizeBytes != null && copiedBytes > maximumFileSizeBytes) {
            throw AdditionalKycFileTooLargeException()
        }
        output.write(buffer, 0, bytesRead)
    }
}

internal class AdditionalKycFileTooLargeException : IOException()
