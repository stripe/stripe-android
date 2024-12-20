package com.stripe.attestation;

import com.google.android.play.core.integrity.StandardIntegrityException
import com.google.android.play.core.integrity.model.StandardIntegrityErrorCode
import com.stripe.attestation.AttestationError.ErrorType.*

class AttestationError(
    val errorType: ErrorType,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {

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
                    StandardIntegrityErrorCode.API_NOT_AVAILABLE -> API_NOT_AVAILABLE
                    StandardIntegrityErrorCode.APP_NOT_INSTALLED -> APP_NOT_INSTALLED
                    StandardIntegrityErrorCode.APP_UID_MISMATCH -> APP_UID_MISMATCH
                    StandardIntegrityErrorCode.CANNOT_BIND_TO_SERVICE -> CANNOT_BIND_TO_SERVICE
                    StandardIntegrityErrorCode.CLIENT_TRANSIENT_ERROR -> CLIENT_TRANSIENT_ERROR
                    StandardIntegrityErrorCode.CLOUD_PROJECT_NUMBER_IS_INVALID -> CLOUD_PROJECT_NUMBER_IS_INVALID
                    StandardIntegrityErrorCode.GOOGLE_SERVER_UNAVAILABLE -> GOOGLE_SERVER_UNAVAILABLE
                    StandardIntegrityErrorCode.INTEGRITY_TOKEN_PROVIDER_INVALID -> INTEGRITY_TOKEN_PROVIDER_INVALID
                    StandardIntegrityErrorCode.INTERNAL_ERROR -> INTERNAL_ERROR
                    StandardIntegrityErrorCode.NETWORK_ERROR -> NETWORK_ERROR
                    StandardIntegrityErrorCode.NO_ERROR -> NO_ERROR
                    StandardIntegrityErrorCode.PLAY_SERVICES_NOT_FOUND -> PLAY_SERVICES_NOT_FOUND
                    StandardIntegrityErrorCode.PLAY_SERVICES_VERSION_OUTDATED -> PLAY_SERVICES_VERSION_OUTDATED
                    StandardIntegrityErrorCode.PLAY_STORE_NOT_FOUND -> PLAY_STORE_NOT_FOUND
                    StandardIntegrityErrorCode.PLAY_STORE_VERSION_OUTDATED -> PLAY_STORE_VERSION_OUTDATED
                    StandardIntegrityErrorCode.REQUEST_HASH_TOO_LONG -> REQUEST_HASH_TOO_LONG
                    StandardIntegrityErrorCode.TOO_MANY_REQUESTS -> TOO_MANY_REQUESTS
                    else -> UNKNOWN
                }
                AttestationError(
                    errorType = errorType,
                    message = exception.message ?: "Integrity error occurred",
                    cause = exception
                )
            } else {
                // Handle non-standard exceptions as unknown errors
                AttestationError(
                    errorType = UNKNOWN,
                    message = "An unknown error occurred",
                    cause = exception
                )
            }
        }
    }
}