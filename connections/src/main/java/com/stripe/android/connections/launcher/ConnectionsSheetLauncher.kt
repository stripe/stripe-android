package com.stripe.android.connections.launcher

import com.stripe.android.connections.ConnectionsSheet

internal interface ConnectionsSheetLauncher {
    fun present(configuration: ConnectionsSheet.Configuration)
}
