package com.stripe.android.crypto.onramp

import com.stripe.android.crypto.onramp.model.AdditionalKycSubmission

internal fun interface AdditionalKycSubmissionHandler {
    suspend fun submit(submission: AdditionalKycSubmission): Result<Unit>
}

internal object AdditionalKycSubmissionHandlerRegistry {
    private val handlers = mutableMapOf<String, AdditionalKycSubmissionHandler>()

    operator fun get(key: String): AdditionalKycSubmissionHandler? {
        return handlers[key]
    }

    operator fun set(key: String, handler: AdditionalKycSubmissionHandler) {
        handlers[key] = handler
    }

    fun remove(key: String) {
        handlers.remove(key)
    }
}
