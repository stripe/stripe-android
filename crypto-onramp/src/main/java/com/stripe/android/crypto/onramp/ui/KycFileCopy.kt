package com.stripe.android.crypto.onramp.ui

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

internal suspend fun copyKycFile(
    input: InputStream,
    output: OutputStream,
    maximumFileSizeBytes: Long?,
) {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var copiedBytes = 0L

    while (true) {
        currentCoroutineContext().ensureActive()
        val bytesRead = input.read(buffer)
        currentCoroutineContext().ensureActive()
        if (bytesRead == -1) {
            return
        }

        copiedBytes += bytesRead
        if (maximumFileSizeBytes != null && copiedBytes > maximumFileSizeBytes) {
            throw KycFileTooLargeException()
        }
        output.write(buffer, 0, bytesRead)
    }
}

internal class KycFileTooLargeException : IOException()
