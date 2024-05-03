package com.stripe.android.analytics

import com.stripe.android.core.exception.safeAnalyticsMessage
import com.stripe.android.core.networking.AnalyticsEvent
import kotlin.time.Duration
import kotlin.time.DurationUnit

internal sealed class PaymentSessionEvent : AnalyticsEvent {
    abstract val additionalParams: Map<String, Any?>

    class LoadStarted : PaymentSessionEvent() {
        override val eventName: String = "bi_load_started"

        override val additionalParams: Map<String, Any?> = emptyMap()
    }

    class LoadSucceeded(
        code: String?,
        duration: Duration?,
    ) : PaymentSessionEvent() {
        override val eventName: String = "bi_load_succeeded"

        override val additionalParams: Map<String, Any?> = mapOf(
            FIELD_DURATION to duration?.asSeconds,
            FIELD_SELECTED_LPM to code,
        )
    }

    class LoadFailed(
        duration: Duration?,
        error: Throwable,
    ) : PaymentSessionEvent() {
        override val eventName: String = "bi_load_failed"

        override val additionalParams: Map<String, Any?> = mapOf(
            FIELD_DURATION to duration?.asSeconds,
            FIELD_ERROR_MESSAGE to error.safeAnalyticsMessage,
        )
    }

    class ShowPaymentOptions : PaymentSessionEvent() {
        override val eventName: String = "bi_options_shown"

        override val additionalParams: Map<String, Any?> = mapOf()
    }

    class ShowPaymentOptionForm(
        code: String,
    ) : PaymentSessionEvent() {
        override val eventName: String = "bi_form_shown"

        override val additionalParams: Map<String, Any?> = mapOf(
            FIELD_SELECTED_LPM to code,
        )
    }

    class PaymentOptionFormInteraction(
        code: String,
    ) : PaymentSessionEvent() {
        override val eventName: String = "bi_form_interacted"

        override val additionalParams: Map<String, Any?> = mapOf(
            FIELD_SELECTED_LPM to code,
        )
    }

    class CardNumberCompleted : PaymentSessionEvent() {
        override val eventName: String = "bi_card_number_completed"

        override val additionalParams: Map<String, Any?> = mapOf()
    }

    class TapDoneButton(
        code: String,
        duration: Duration?,
    ) : PaymentSessionEvent() {
        override val eventName: String = "bi_done_button_tapped"

        override val additionalParams: Map<String, Any?> = mapOf(
            FIELD_SELECTED_LPM to code,
            FIELD_DURATION to duration?.asSeconds,
        )
    }

    internal companion object {
        private val Duration.asSeconds: Float
            get() = toDouble(DurationUnit.SECONDS).toFloat()

        const val FIELD_DURATION = "duration"
        const val FIELD_ERROR_MESSAGE = "error_message"
        const val FIELD_SELECTED_LPM = "selected_lpm"
    }
}
