package com.stripe.android.connect.webview.serialization

import kotlinx.serialization.Serializable

@Serializable
internal data class AccountSessionClaimedMessage(
    val merchantId: String,
)

@Serializable
internal data class OpenFinancialConnectionsMessage(
    val id: String,
    val clientSecret: String,
    val connectedAccountId: String,
)

@Serializable
internal data class PageLoadMessage(
    val pageViewId: String
)

@Serializable
internal data class SetterMessage(
    val setter: String,
    val value: SetterMessageValue,
)

@Serializable
internal data class SetterMessageValue(
    val elementTagName: String,
    val message: String? = null,
)

@Serializable
internal data class SecureWebViewMessage(
    val id: String,
    val url: String
)
