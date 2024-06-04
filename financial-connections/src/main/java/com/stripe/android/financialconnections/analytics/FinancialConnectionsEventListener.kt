package com.stripe.android.financialconnections.analytics

/**
 * Listener for events that occur during the Financial Connections Auth Flow.
 */
fun interface FinancialConnectionsEventListener {
    fun onEvent(event: FinancialConnectionsEvent)
}
