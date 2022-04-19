package com.stripe.android.financialconnections

/**
 * Callback that is invoked when a [ConnectionsSheetResult] is available.
 */
fun interface ConnectionsSheetResultCallback {
    fun onConnectionsSheetResult(connectionsSheetResult: ConnectionsSheetResult)
}
