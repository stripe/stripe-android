package com.stripe.android.paymentsheet.events

@ExperimentalEventsApi
object EventRegistry {
    @Volatile
    private var eventListener: EventListener = NoOpEventListener

    fun setEventHandler(eventListener: EventListener) {
        this.eventListener = eventListener
    }

    internal fun sendEvent(event: Any) {
        eventListener.onEvent(event)
    }
}
