package com.stripe.android.financialconnections

import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEventListener

object FinancialConnections {

    private var eventListener: FinancialConnectionsEventListener? = null

    /**
     * Set the event listener to be notified of events that occur during the Financial
     * Connections Auth Flow.
     *
     * Events are emitted after the Financial Connections session ID is available. If the flow
     * fails before a session ID is available, its completion callback still reports the failure.
     */
    @JvmStatic
    @Synchronized
    fun setEventListener(listener: FinancialConnectionsEventListener) {
        this.eventListener = listener
    }

    /**
     * Clear the event listener.
     */
    @JvmStatic
    @Synchronized
    fun clearEventListener() {
        this.eventListener = null
    }

    internal fun emitEvent(event: FinancialConnectionsEvent) = runCatching {
        eventListener?.onEvent(event)
    }
}
