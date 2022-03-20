package com.stripe.android.connections.example

import com.stripe.android.connections.ConnectionsSheet

data class ConnectionsState(
    val loading: Boolean = false,
    val status: String = ""
)

sealed class ConnectionsViewEffect {
    data class OpenConnectionsSheet(
        val configuration: ConnectionsSheet.Configuration
    ) : ConnectionsViewEffect()
}
