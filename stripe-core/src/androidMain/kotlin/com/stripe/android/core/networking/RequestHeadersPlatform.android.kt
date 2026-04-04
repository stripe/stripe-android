package com.stripe.android.core.networking

import android.os.Build
import android.system.Os
import androidx.core.os.LocaleListCompat

internal actual fun defaultRequestHeadersPlatformData(): RequestHeadersPlatformData {
    return RequestHeadersPlatformData(
        osName = "android",
        osVersion = Build.VERSION.SDK_INT.toString(),
        deviceType = "${Build.MANUFACTURER}_${Build.BRAND}_${Build.MODEL}",
        deviceModel = Build.MODEL,
        isStripeLiveMode = Os.getenv("Stripe-Livemode") != "false",
    )
}

internal actual fun defaultRequestHeadersLanguageTag(): String? {
    return LocaleListCompat.getAdjustedDefault()
        .takeUnless { it.isEmpty }
        ?.get(0)
        ?.toLanguageTag()
}

internal actual fun defaultSystemProperty(name: String): String {
    return System.getProperty(name).orEmpty()
}
