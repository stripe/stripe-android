package com.stripe.android.connections.example

import com.stripe.android.connections.ConnectionsSheet

sealed class ConnectionsExampleViewEffect {
    data class OpenConnectionsSheetExample(
        val configuration: ConnectionsSheet.Configuration
    ) : ConnectionsExampleViewEffect()

    data class OpenConnectionsSheetForTokenExample(
        val configuration: ConnectionsSheet.Configuration
    ) : ConnectionsExampleViewEffect()
}