package com.stripe.android.financialconnections.analytics

import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.utils.filterNotNullValues

/**
 * Event definitions for Financial Connections.
 */
internal sealed class FinancialConnectionsEvent(
    val name: String,
    val params: Map<String, String>? = null
) {
    class PaneLaunched(
        pane: FinancialConnectionsSessionManifest.NextPane,
    ) : FinancialConnectionsEvent(
        "pane.launched",
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class PaneLoaded(
        pane: FinancialConnectionsSessionManifest.NextPane,
    ) : FinancialConnectionsEvent(
        "pane.loaded",
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    override fun toString(): String {
        return "FinancialConnectionsEvent(name='$name', params=$params)"
    }
}
