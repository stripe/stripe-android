package com.stripe.android.financialconnections.example.settings

import android.net.Uri
import android.util.Log

internal object FinancialConnectionsPlaygroundUrlHelper {
    fun settingsFromUri(uri: Uri?): PlaygroundSettings? {
        Log.d("FinancialConnections", "settingsFromUri: $uri")
        return uri
            ?.takeIf { it.scheme == "stripeconnectionsexample" }
            ?.takeIf { it.host == "playground" }
            ?.let { PlaygroundSettings.createFromDeeplinkUri(it) }
    }
}
