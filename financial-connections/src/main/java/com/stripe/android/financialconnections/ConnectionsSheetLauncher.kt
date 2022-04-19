package com.stripe.android.financialconnections

internal interface ConnectionsSheetLauncher {
    fun present(configuration: ConnectionsSheet.Configuration)
}
