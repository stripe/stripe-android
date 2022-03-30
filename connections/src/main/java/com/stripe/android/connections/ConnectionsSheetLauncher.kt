package com.stripe.android.connections

internal interface ConnectionsSheetLauncher {
    fun present(configuration: ConnectionsSheet.Configuration)
}
