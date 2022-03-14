package com.stripe.android.connections

/**
 * Callback that is invoked when a [ConnectionsSheetResult] is available.
 */
fun interface ConnectionsSheetResultCallback {
    fun onConnectionsSheetResult(connectionsSheetResult: ConnectionsSheetResult)
}
