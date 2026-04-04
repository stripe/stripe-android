package com.stripe.android.core.networking

internal data class RequestHeadersPlatformData(
    val osName: String,
    val osVersion: String,
    val deviceType: String,
    val deviceModel: String,
    val isStripeLiveMode: Boolean,
)

internal expect fun defaultRequestHeadersPlatformData(): RequestHeadersPlatformData

internal expect fun defaultRequestHeadersLanguageTag(): String?

internal expect fun defaultSystemProperty(name: String): String
