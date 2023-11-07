package com.stripe.android.financialconnections

import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Metadata
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Name
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEventListener

object FinancialConnections {

    private var eventListener: FinancialConnectionsEventListener? = null

    /**
     * Set the event listener to be notified of events that occur during the Financial
     * Connections Auth Flow.
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

    internal fun emitEvent(
        name: Name,
        metadata: Metadata = Metadata()
    ) = runCatching {
        eventListener?.onEvent(
            FinancialConnectionsEvent(
                name = name,
                metadata = metadata
            )
        )
    }
}
