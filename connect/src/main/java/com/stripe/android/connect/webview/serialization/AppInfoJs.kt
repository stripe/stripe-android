package com.stripe.android.connect.webview.serialization

import kotlinx.serialization.Serializable

@Serializable
internal data class AppInfoJs(
    val applicationId: String
)
