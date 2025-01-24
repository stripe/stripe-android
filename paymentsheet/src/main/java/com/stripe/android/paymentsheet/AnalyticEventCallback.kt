package com.stripe.android.paymentsheet

fun interface AnalyticEventCallback {
    suspend fun onEvent(event: AnalyticEvent)
}

sealed class AnalyticEvent {
    data object PresentPaymentSheet : AnalyticEvent()
}