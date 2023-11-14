package com.stripe.android.paymentsheet.example

import android.util.Log
import com.stripe.android.paymentsheet.events.EventListener
import com.stripe.android.paymentsheet.events.ExperimentalEventsApi
import com.stripe.android.paymentsheet.events.PaymentSuccessEvent

@OptIn(ExperimentalEventsApi::class)
internal object ExampleEventListener : EventListener {
    override fun onEvent(event: Any) {
        when (event) {
            is PaymentSuccessEvent -> {
                Log.d("PaymentSuccessEvent", "Duration: ${event.duration}")
            }
            else -> {
                Log.d("UnknownEvent", "Event: $event")
            }
        }
    }
}
