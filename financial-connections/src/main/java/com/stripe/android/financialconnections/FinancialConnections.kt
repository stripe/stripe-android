package com.stripe.android.financialconnections

object FinancialConnections {

    private var eventListener: FinancialConnectionsEventListener? = null

    fun setEventListener(listener: FinancialConnectionsEventListener) {
        this.eventListener = listener
    }

    internal fun emitEvent(event: FinancialConnectionsPublicEvent) {
        eventListener?.onEvent(event)
    }

}

interface FinancialConnectionsEventListener {
    fun onEvent(event: FinancialConnectionsPublicEvent)
}

sealed class FinancialConnectionsPublicEvent {
    class Launched(pane: String) : FinancialConnectionsPublicEvent()
}