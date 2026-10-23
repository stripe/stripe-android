package com.stripe.android.crypto.onramp

import com.stripe.android.crypto.onramp.model.KycSubmission
import java.io.File

internal fun interface KycSubmissionHandler {
    suspend fun submit(submission: KycSubmission): Result<Unit>
}

internal fun interface KycDocumentUploader {
    suspend fun upload(file: File): Result<String>
}

internal object KycSubmissionHandlerRegistry {
    private val uploaders = mutableMapOf<String, KycDocumentUploader>()

    fun uploader(key: String): KycDocumentUploader? = uploaders[key]

    fun setUploader(key: String, uploader: KycDocumentUploader) {
        uploaders[key] = uploader
    }

    private val handlers = mutableMapOf<String, KycSubmissionHandler>()

    operator fun get(key: String): KycSubmissionHandler? {
        return handlers[key]
    }

    operator fun set(key: String, handler: KycSubmissionHandler) {
        handlers[key] = handler
    }

    fun remove(key: String) {
        handlers.remove(key)
        uploaders.remove(key)
    }
}
