package com.stripe.android.attestation.analytics

import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.attestation.AttestationError

internal sealed interface AttestationAnalyticsEvent : AnalyticsEvent {
    val params: Map<String, Any?>
        get() = emptyMap()

    data object Prepare : AttestationAnalyticsEvent {
        override val eventName: String
            get() = "elements.attestation.confirmation.prepare"
    }

    class PrepareFailed(
        error: Throwable?,
        duration: Float?
    ) : AttestationAnalyticsEvent {
        override val eventName: String
            get() = "elements.attestation.confirmation.prepare.failed"

        override val params = mapOf(
            FIELD_ERROR_MESSAGE to error?.message,
            FIELD_DURATION to duration
        ) + extraAttestationErrorParams(error)
    }

    class PrepareSucceeded(duration: Float?) : AttestationAnalyticsEvent {
        override val eventName: String
            get() = "elements.attestation.confirmation.prepare.succeeded"

        override val params = mapOf(
            FIELD_DURATION to duration
        )
    }

    data object RequestToken : AttestationAnalyticsEvent {
        override val eventName: String
            get() = "elements.attestation.confirmation.request_token"
    }

    class RequestTokenSucceeded(duration: Float?) : AttestationAnalyticsEvent {
        override val eventName: String
            get() = "elements.attestation.confirmation.request_token.succeeded"

        override val params = mapOf(
            FIELD_DURATION to duration
        )
    }

    class RequestTokenFailed(
        error: Throwable?,
        duration: Float?
    ) : AttestationAnalyticsEvent {
        override val eventName: String
            get() = "elements.attestation.confirmation.request_token.failed"

        override val params = mapOf(
            FIELD_ERROR_MESSAGE to error?.message,
            FIELD_DURATION to duration
        ) + extraAttestationErrorParams(error)
    }

    companion object {
        private const val FIELD_ERROR_MESSAGE = "error_message"
        private const val FIELD_DURATION = "duration"
        private const val FIELD_ATTESTATION_ERROR_TYPE = "android_attestation_error_type"
        private const val FIELD_ATTESTATION_ERROR_IS_RETRIABLE = "android_attestation_error_retriable"

        private fun extraAttestationErrorParams(error: Throwable?): Map<String, Any?> {
            if (error !is AttestationError) {
                return emptyMap()
            }
            return mapOf(
                FIELD_ATTESTATION_ERROR_TYPE to error.errorType.name,
                FIELD_ATTESTATION_ERROR_IS_RETRIABLE to error.errorType.isRetriable
            )
        }
    }
}
