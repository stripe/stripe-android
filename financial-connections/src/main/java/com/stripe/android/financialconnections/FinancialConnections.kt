package com.stripe.android.financialconnections

import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEventListener

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

    internal fun emitEvent(event: FinancialConnectionsEvent) {
        eventListener?.invoke(event)
    }
}
