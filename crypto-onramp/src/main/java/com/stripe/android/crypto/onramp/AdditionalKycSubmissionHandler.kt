package com.stripe.android.crypto.onramp

import com.stripe.android.crypto.onramp.model.AdditionalKycSubmission
import java.io.File

internal fun interface AdditionalKycSubmissionHandler {
    suspend fun submit(submission: AdditionalKycSubmission): Result<Unit>
}

internal fun interface AdditionalKycDocumentUploader {
    suspend fun upload(file: File): Result<String>
}

internal object AdditionalKycSubmissionHandlerRegistry {
    private val uploaders = mutableMapOf<String, AdditionalKycDocumentUploader>()

    fun uploader(key: String): AdditionalKycDocumentUploader? = uploaders[key]

    fun setUploader(key: String, uploader: AdditionalKycDocumentUploader) {
        uploaders[key] = uploader
    }

    private val handlers = mutableMapOf<String, AdditionalKycSubmissionHandler>()

    operator fun get(key: String): AdditionalKycSubmissionHandler? {
        return handlers[key]
    }

    operator fun set(key: String, handler: AdditionalKycSubmissionHandler) {
        handlers[key] = handler
    }

    fun remove(key: String) {
        handlers.remove(key)
        uploaders.remove(key)
    }
}
