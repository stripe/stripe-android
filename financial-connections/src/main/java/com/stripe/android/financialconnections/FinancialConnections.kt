package com.stripe.android.financialconnections

object FinancialConnections {

    private var eventListener: FinancialConnectionsEventListener? = null

    @JvmStatic
    @Synchronized
    fun setEventListener(listener: FinancialConnectionsEventListener) {
        this.eventListener = listener
    }

    @JvmStatic
    @Synchronized
    fun clearEventListener() {
        this.eventListener = null
    }

    internal fun emitEvent(event: FinancialConnectionsPublicEvent) {
        eventListener?.onEvent(event)
    }

}

fun interface FinancialConnectionsEventListener {
    fun onEvent(event: FinancialConnectionsPublicEvent)
}

sealed class FinancialConnectionsPublicEvent {
    class Launched(pane: String) : FinancialConnectionsPublicEvent()
}