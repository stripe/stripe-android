package com.stripe.android.crypto.onramp

import com.stripe.android.crypto.onramp.model.KycSubmission
import java.io.File

internal fun interface KycSubmissionHandler {
    suspend fun submit(submission: KycSubmission): Result<Unit>
}

internal fun interface KycDocumentUploader {
    suspend fun upload(file: File): Result<String>
}
