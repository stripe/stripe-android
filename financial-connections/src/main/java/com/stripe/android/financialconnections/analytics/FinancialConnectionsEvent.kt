package com.stripe.android.financialconnections.analytics

import com.stripe.android.financialconnections.utils.filterNotNullValues

/**
 * Event definitions for Financial Connections.
 */
internal sealed class FinancialConnectionsEvent(
    val name: String,
    val params: Map<String, String>? = null
) {
    class PaneLaunched(
        pane: String,
    ) : FinancialConnectionsEvent(
        "pane.launched",
        mapOf(
            "pane" to pane,
        ).filterNotNullValues()
    )

    override fun toString(): String {
        return "FinancialConnectionsEvent(name='$name', params=$params)"
    }
}
