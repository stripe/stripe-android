package com.stripe.android.attestation.analytics

import com.stripe.android.core.networking.AnalyticsEvent

internal sealed interface AttestationAnalyticsEvent : AnalyticsEvent {
    val params: Map<String, Any?>
        get() = emptyMap()

    data object Prepare : AttestationAnalyticsEvent {
        override val eventName: String
            get() = "elements.attestation.confirmation.prepare"
    }

    class PrepareFailed(error: Throwable?) : AttestationAnalyticsEvent {
        override val eventName: String
            get() = "elements.attestation.confirmation.prepare.failed"

        override val params = mapOf(
            FIELD_ERROR_MESSAGE to error?.message
        )
    }

    data object PrepareSucceeded : AttestationAnalyticsEvent {
        override val eventName: String
            get() = "elements.attestation.confirmation.prepare.succeeded"
    }

    data object RequestToken : AttestationAnalyticsEvent {
        override val eventName: String
            get() = "elements.attestation.confirmation.request_token"
    }

    data object RequestTokenSucceeded : AttestationAnalyticsEvent {
        override val eventName: String
            get() = "elements.attestation.confirmation.request_token.succeeded"
    }

    class RequestTokenFailed(error: Throwable?) : AttestationAnalyticsEvent {
        override val eventName: String
            get() = "elements.attestation.confirmation.request_token.failed"

        override val params = mapOf(
            FIELD_ERROR_MESSAGE to error?.message
        )
    }

    companion object {
        private const val FIELD_ERROR_MESSAGE = "error_message"
    }
}
