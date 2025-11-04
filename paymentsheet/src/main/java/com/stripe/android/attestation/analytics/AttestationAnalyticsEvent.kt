package com.stripe.android.attestation.analytics

import com.stripe.android.core.networking.AnalyticsEvent

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
        )
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
        )
    }

    companion object {
        private const val FIELD_ERROR_MESSAGE = "error_message"
        private const val FIELD_DURATION = "duration"
    }
}
