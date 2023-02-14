package com.stripe.android.financialconnections.network

internal object NetworkConstants {
    internal const val PARAMS_CLIENT_SECRET = "client_secret"
    internal const val PARAMS_ID = "id"
    internal const val PARAMS_APPLICATION_ID = "application_id"
    internal const val PARAM_SELECTED_ACCOUNTS: String = "selected_accounts"

    // TODO@carlosmuvi update consumer surface to be android specific.
    internal const val CONSUMER_SURFACE: String = "web_connections"
}
