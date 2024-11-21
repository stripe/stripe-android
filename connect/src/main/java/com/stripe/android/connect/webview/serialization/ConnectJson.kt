package com.stripe.android.connect.webview.serialization

import kotlinx.serialization.json.Json

internal val ConnectJson: Json =
    Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
