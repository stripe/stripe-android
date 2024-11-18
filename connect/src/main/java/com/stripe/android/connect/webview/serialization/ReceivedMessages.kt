package com.stripe.android.connect.webview.serialization

import kotlinx.serialization.Serializable

@Serializable
data class AccountSessionClaimedMessage(
    val merchantId: String,
)

@Serializable
data class PageLoadMessage(
    val pageViewId: String
)

@Serializable
data class SetterMessage(
    val setter: String,
    val value: SetterMessageValue,
)

@Serializable
data class SetterMessageValue(
    val elementTagName: String,
    val message: String? = null,
)

@Serializable
data class SecureWebViewMessage(
    val id: String,
    val url: String
)
