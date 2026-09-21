package com.stripe.attestation

import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class AttestationErrorTest {

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
