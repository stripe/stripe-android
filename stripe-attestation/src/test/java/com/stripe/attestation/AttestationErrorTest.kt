package com.stripe.attestation

import com.google.android.play.core.integrity.FakeStandardIntegrityException
import com.google.android.play.core.integrity.model.StandardIntegrityErrorCode
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
internal class AttestationErrorTest {

    @Test
    fun `fromException returns existing AttestationError`() {
        val error = AttestationError(
            errorType = AttestationError.ErrorType.NETWORK_ERROR,
            message = "Network error"
        )

        assertThat(AttestationError.fromException(error)).isSameInstanceAs(error)
    }

    @Test
    fun `fromException maps StandardIntegrityException error code`(
        @TestParameter(valuesProvider = StandardIntegrityErrorMappingProvider::class)
        testCase: StandardIntegrityErrorMapping,
    ) {
        val exception = FakeStandardIntegrityException(
            errorCode = testCase.errorCode,
            message = "Integrity error",
        )

        val error = AttestationError.fromException(exception)

        assertThat(error.errorType).isEqualTo(testCase.expectedErrorType)
        assertThat(error.message).isEqualTo("Integrity error")
        assertThat(error.cause).isSameInstanceAs(exception)
    }

    @Test
    fun `fromException maps unrecognized StandardIntegrityException error code to unknown`() {
        val exception = FakeStandardIntegrityException(
            errorCode = Int.MIN_VALUE,
            message = "Unrecognized integrity error",
        )

        val error = AttestationError.fromException(exception)

        assertThat(error.errorType).isEqualTo(AttestationError.ErrorType.UNKNOWN)
        assertThat(error.message).isEqualTo("Unrecognized integrity error")
        assertThat(error.cause).isSameInstanceAs(exception)
    }

    @Test
    fun `fromException uses default message when StandardIntegrityException has no message`() {
        val exception = FakeStandardIntegrityException(
            errorCode = StandardIntegrityErrorCode.INTERNAL_ERROR,
            message = null,
        )

        val error = AttestationError.fromException(exception)

        assertThat(error.errorType).isEqualTo(AttestationError.ErrorType.INTERNAL_ERROR)
        assertThat(error.message).isEqualTo("Integrity error occurred")
        assertThat(error.cause).isSameInstanceAs(exception)
    }

    @Test
    fun `fromException wraps other exceptions as unknown`() {
        val exception = IllegalStateException("Unexpected error")

        val error = AttestationError.fromException(exception)

        assertThat(error.errorType).isEqualTo(AttestationError.ErrorType.UNKNOWN)
        assertThat(error.message).isEqualTo("An unknown error occurred")
        assertThat(error.cause).isSameInstanceAs(exception)
    }

    @Test
    fun `isRetriable has expected value`(
        @TestParameter errorType: AttestationError.ErrorType
    ) {
        assertThat(errorType.isRetriable).isEqualTo(expectedRetryability(errorType))
    }

    private fun expectedRetryability(errorType: AttestationError.ErrorType): Boolean {
        // This table intentionally duplicates ErrorType.isRetriable so the external retry contract is
        // encoded independently from production values. Changes and new error types require explicit review.
        return when (errorType) {
            AttestationError.ErrorType.CLIENT_TRANSIENT_ERROR,
            AttestationError.ErrorType.GOOGLE_SERVER_UNAVAILABLE,
            AttestationError.ErrorType.INTERNAL_ERROR,
            AttestationError.ErrorType.NETWORK_ERROR,
            AttestationError.ErrorType.TOO_MANY_REQUESTS -> true
            AttestationError.ErrorType.API_NOT_AVAILABLE,
            AttestationError.ErrorType.APP_NOT_INSTALLED,
            AttestationError.ErrorType.APP_UID_MISMATCH,
            AttestationError.ErrorType.CANNOT_BIND_TO_SERVICE,
            AttestationError.ErrorType.CLOUD_PROJECT_NUMBER_IS_INVALID,
            AttestationError.ErrorType.INTEGRITY_TOKEN_PROVIDER_INVALID,
            AttestationError.ErrorType.NO_ERROR,
            AttestationError.ErrorType.PLAY_SERVICES_NOT_FOUND,
            AttestationError.ErrorType.PLAY_SERVICES_VERSION_OUTDATED,
            AttestationError.ErrorType.PLAY_STORE_NOT_FOUND,
            AttestationError.ErrorType.PLAY_STORE_VERSION_OUTDATED,
            AttestationError.ErrorType.REQUEST_HASH_TOO_LONG,
            AttestationError.ErrorType.BACKEND_VERDICT_FAILED,
            AttestationError.ErrorType.UNKNOWN -> false
        }
    }
}

internal object StandardIntegrityErrorMappingProvider : TestParameterValuesProvider() {
    override fun provideValues(
        context: Context?,
    ): List<StandardIntegrityErrorMapping> = mappings

    private val mappings = listOf(
        StandardIntegrityErrorMapping(
            name = "API_NOT_AVAILABLE",
            errorCode = StandardIntegrityErrorCode.API_NOT_AVAILABLE,
            expectedErrorType = AttestationError.ErrorType.API_NOT_AVAILABLE,
        ),
        StandardIntegrityErrorMapping(
            name = "APP_NOT_INSTALLED",
            errorCode = StandardIntegrityErrorCode.APP_NOT_INSTALLED,
            expectedErrorType = AttestationError.ErrorType.APP_NOT_INSTALLED,
        ),
        StandardIntegrityErrorMapping(
            name = "APP_UID_MISMATCH",
            errorCode = StandardIntegrityErrorCode.APP_UID_MISMATCH,
            expectedErrorType = AttestationError.ErrorType.APP_UID_MISMATCH,
        ),
        StandardIntegrityErrorMapping(
            name = "CANNOT_BIND_TO_SERVICE",
            errorCode = StandardIntegrityErrorCode.CANNOT_BIND_TO_SERVICE,
            expectedErrorType = AttestationError.ErrorType.CANNOT_BIND_TO_SERVICE,
        ),
        StandardIntegrityErrorMapping(
            name = "CLIENT_TRANSIENT_ERROR",
            errorCode = StandardIntegrityErrorCode.CLIENT_TRANSIENT_ERROR,
            expectedErrorType = AttestationError.ErrorType.CLIENT_TRANSIENT_ERROR,
        ),
        StandardIntegrityErrorMapping(
            name = "CLOUD_PROJECT_NUMBER_IS_INVALID",
            errorCode = StandardIntegrityErrorCode.CLOUD_PROJECT_NUMBER_IS_INVALID,
            expectedErrorType = AttestationError.ErrorType.CLOUD_PROJECT_NUMBER_IS_INVALID,
        ),
        StandardIntegrityErrorMapping(
            name = "GOOGLE_SERVER_UNAVAILABLE",
            errorCode = StandardIntegrityErrorCode.GOOGLE_SERVER_UNAVAILABLE,
            expectedErrorType = AttestationError.ErrorType.GOOGLE_SERVER_UNAVAILABLE,
        ),
        StandardIntegrityErrorMapping(
            name = "INTEGRITY_TOKEN_PROVIDER_INVALID",
            errorCode = StandardIntegrityErrorCode.INTEGRITY_TOKEN_PROVIDER_INVALID,
            expectedErrorType = AttestationError.ErrorType.INTEGRITY_TOKEN_PROVIDER_INVALID,
        ),
        StandardIntegrityErrorMapping(
            name = "INTERNAL_ERROR",
            errorCode = StandardIntegrityErrorCode.INTERNAL_ERROR,
            expectedErrorType = AttestationError.ErrorType.INTERNAL_ERROR,
        ),
        StandardIntegrityErrorMapping(
            name = "NETWORK_ERROR",
            errorCode = StandardIntegrityErrorCode.NETWORK_ERROR,
            expectedErrorType = AttestationError.ErrorType.NETWORK_ERROR,
        ),
        StandardIntegrityErrorMapping(
            name = "PLAY_SERVICES_NOT_FOUND",
            errorCode = StandardIntegrityErrorCode.PLAY_SERVICES_NOT_FOUND,
            expectedErrorType = AttestationError.ErrorType.PLAY_SERVICES_NOT_FOUND,
        ),
        StandardIntegrityErrorMapping(
            name = "PLAY_SERVICES_VERSION_OUTDATED",
            errorCode = StandardIntegrityErrorCode.PLAY_SERVICES_VERSION_OUTDATED,
            expectedErrorType = AttestationError.ErrorType.PLAY_SERVICES_VERSION_OUTDATED,
        ),
        StandardIntegrityErrorMapping(
            name = "PLAY_STORE_NOT_FOUND",
            errorCode = StandardIntegrityErrorCode.PLAY_STORE_NOT_FOUND,
            expectedErrorType = AttestationError.ErrorType.PLAY_STORE_NOT_FOUND,
        ),
        StandardIntegrityErrorMapping(
            name = "PLAY_STORE_VERSION_OUTDATED",
            errorCode = StandardIntegrityErrorCode.PLAY_STORE_VERSION_OUTDATED,
            expectedErrorType = AttestationError.ErrorType.PLAY_STORE_VERSION_OUTDATED,
        ),
        StandardIntegrityErrorMapping(
            name = "REQUEST_HASH_TOO_LONG",
            errorCode = StandardIntegrityErrorCode.REQUEST_HASH_TOO_LONG,
            expectedErrorType = AttestationError.ErrorType.REQUEST_HASH_TOO_LONG,
        ),
        StandardIntegrityErrorMapping(
            name = "TOO_MANY_REQUESTS",
            errorCode = StandardIntegrityErrorCode.TOO_MANY_REQUESTS,
            expectedErrorType = AttestationError.ErrorType.TOO_MANY_REQUESTS,
        ),
    )
}

internal data class StandardIntegrityErrorMapping(
    val name: String,
    val errorCode: Int,
    val expectedErrorType: AttestationError.ErrorType,
) {
    override fun toString(): String = name
}
