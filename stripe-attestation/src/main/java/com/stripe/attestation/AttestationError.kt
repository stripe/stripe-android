package com.stripe.attestation;

import com.google.android.play.core.integrity.StandardIntegrityException

class AttestationError(
    val errorType: ErrorType,
    message: String,
    cause: Throwable? = null
) : Throwable(message, cause) {

    /**
     *
     */
    enum class ErrorType(
        val isRetriable: Boolean
    ) {
        API_NOT_AVAILABLE(isRetriable = false),
        APP_NOT_INSTALLED(isRetriable = false),
        APP_UID_MISMATCH(isRetriable = false),
        CANNOT_BIND_TO_SERVICE(isRetriable = true),
        CLIENT_TRANSIENT_ERROR(isRetriable = true),
        CLOUD_PROJECT_NUMBER_IS_INVALID(isRetriable = false),
        GOOGLE_SERVER_UNAVAILABLE(isRetriable = true),
        INTEGRITY_TOKEN_PROVIDER_INVALID(isRetriable = false),
        INTERNAL_ERROR(isRetriable = true),
        NO_ERROR(isRetriable = true),
        NETWORK_ERROR(isRetriable = true),
        PLAY_SERVICES_NOT_FOUND(isRetriable = false),
        PLAY_SERVICES_VERSION_OUTDATED(isRetriable = false),
        PLAY_STORE_NOT_FOUND(isRetriable = true),
        PLAY_STORE_VERSION_OUTDATED(isRetriable = false),
        REQUEST_HASH_TOO_LONG(isRetriable = false),
        TOO_MANY_REQUESTS(isRetriable = true),
        MAX_RETRIES_EXCEEDED(isRetriable = false),
        UNKNOWN(isRetriable = false)
    }

    companion object {
        fun fromException(exception: Throwable): AttestationError {
            return if (exception is StandardIntegrityException) {
                // see https://developer.android.com/google/play/integrity/error-codes#retryable_error_codes
                val errorType = when (exception.errorCode) {
                    -1 -> ErrorType.API_NOT_AVAILABLE
                    -5 -> ErrorType.APP_NOT_INSTALLED
                    -7 -> ErrorType.APP_UID_MISMATCH
                    -9 -> ErrorType.CANNOT_BIND_TO_SERVICE
                    -18 -> ErrorType.CLIENT_TRANSIENT_ERROR
                    -16 -> ErrorType.CLOUD_PROJECT_NUMBER_IS_INVALID
                    -12 -> ErrorType.GOOGLE_SERVER_UNAVAILABLE
                    -19 -> ErrorType.INTEGRITY_TOKEN_PROVIDER_INVALID
                    -100 -> ErrorType.INTERNAL_ERROR
                    -3 -> ErrorType.NETWORK_ERROR
                    0 -> ErrorType.NO_ERROR
                    -6 -> ErrorType.PLAY_SERVICES_NOT_FOUND
                    -15 -> ErrorType.PLAY_SERVICES_VERSION_OUTDATED
                    -2 -> ErrorType.PLAY_STORE_NOT_FOUND
                    -14 -> ErrorType.PLAY_STORE_VERSION_OUTDATED
                    -17 -> ErrorType.REQUEST_HASH_TOO_LONG
                    -8 -> ErrorType.TOO_MANY_REQUESTS
                    else -> ErrorType.UNKNOWN
                }
                AttestationError(
                    errorType = errorType,
                    message = exception.message ?: "Integrity error occurred",
                    cause = exception
                )
            } else {
                // Handle non-standard exceptions as unknown errors
                AttestationError(
                    errorType = ErrorType.UNKNOWN,
                    message = "An unknown error occurred",
                    cause = exception
                )
            }
        }
    }
}