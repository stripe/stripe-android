package com.stripe.android.paymentsheet.events

@ExperimentalEventsApi
fun interface EventListener {
    fun onEvent(event: Any)
}

@ExperimentalEventsApi
internal object NoOpEventListener : EventListener {
    override fun onEvent(event: Any) {
    }
}
