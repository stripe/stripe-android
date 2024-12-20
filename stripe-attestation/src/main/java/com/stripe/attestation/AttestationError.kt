package com.stripe.attestation

import androidx.annotation.RestrictTo
import com.google.android.play.core.integrity.StandardIntegrityException
import com.google.android.play.core.integrity.model.StandardIntegrityErrorCode

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
            return when (exception) {
                is StandardIntegrityException -> AttestationError(
                    errorType = mapErrorCodeToErrorType(exception.errorCode),
                    message = exception.message ?: "Integrity error occurred",
                    cause = exception
                )
                else -> AttestationError(
                    errorType = ErrorType.UNKNOWN,
                    message = "An unknown error occurred",
                    cause = exception
                )
            }
        }

        private fun mapErrorCodeToErrorType(errorCode: Int): ErrorType = when (errorCode) {
            StandardIntegrityErrorCode.API_NOT_AVAILABLE -> ErrorType.API_NOT_AVAILABLE
            StandardIntegrityErrorCode.APP_NOT_INSTALLED -> ErrorType.APP_NOT_INSTALLED
            StandardIntegrityErrorCode.APP_UID_MISMATCH -> ErrorType.APP_UID_MISMATCH
            StandardIntegrityErrorCode.CANNOT_BIND_TO_SERVICE -> ErrorType.CANNOT_BIND_TO_SERVICE
            StandardIntegrityErrorCode.CLIENT_TRANSIENT_ERROR -> ErrorType.CLIENT_TRANSIENT_ERROR
            StandardIntegrityErrorCode.CLOUD_PROJECT_NUMBER_IS_INVALID -> ErrorType.CLOUD_PROJECT_NUMBER_IS_INVALID
            StandardIntegrityErrorCode.GOOGLE_SERVER_UNAVAILABLE -> ErrorType.GOOGLE_SERVER_UNAVAILABLE
            StandardIntegrityErrorCode.INTEGRITY_TOKEN_PROVIDER_INVALID -> ErrorType.INTEGRITY_TOKEN_PROVIDER_INVALID
            StandardIntegrityErrorCode.INTERNAL_ERROR -> ErrorType.INTERNAL_ERROR
            StandardIntegrityErrorCode.NETWORK_ERROR -> ErrorType.NETWORK_ERROR
            StandardIntegrityErrorCode.NO_ERROR -> ErrorType.NO_ERROR
            StandardIntegrityErrorCode.PLAY_SERVICES_NOT_FOUND -> ErrorType.PLAY_SERVICES_NOT_FOUND
            StandardIntegrityErrorCode.PLAY_SERVICES_VERSION_OUTDATED -> ErrorType.PLAY_SERVICES_VERSION_OUTDATED
            StandardIntegrityErrorCode.PLAY_STORE_NOT_FOUND -> ErrorType.PLAY_STORE_NOT_FOUND
            StandardIntegrityErrorCode.PLAY_STORE_VERSION_OUTDATED -> ErrorType.PLAY_STORE_VERSION_OUTDATED
            StandardIntegrityErrorCode.REQUEST_HASH_TOO_LONG -> ErrorType.REQUEST_HASH_TOO_LONG
            StandardIntegrityErrorCode.TOO_MANY_REQUESTS -> ErrorType.TOO_MANY_REQUESTS
            else -> ErrorType.UNKNOWN
        }
    }
}
