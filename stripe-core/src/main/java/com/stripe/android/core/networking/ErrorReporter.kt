package com.stripe.android.core.networking

interface ErrorReporter {
    fun report(error : ErrorEvent, errorCode : Int?)

    // TODO: add other error params here and then set them as additional params on the analytics event
    enum class ErrorEvent(override val eventName : String) : AnalyticsEvent {
        INCORRECT_NEXT_ACTION_TYPE(eventName = "boleto_incorrect_next_action_type"),
        MISSING_HOSTED_VOUCHER_URL(eventName = "boleto_missing_hosted_voucher_url"),;
    }
}