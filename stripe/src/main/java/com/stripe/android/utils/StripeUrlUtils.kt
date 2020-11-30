package com.stripe.android.utils

import android.net.Uri

internal object StripeUrlUtils {
    internal fun isStripeUrl(url: String): Boolean {
        val uri = Uri.parse(url)
        if (uri.scheme != "https") {
            return false
        }
        val host = uri.host
        return host == "stripe.com" || host?.endsWith(".stripe.com") ?: false
    }
}
