package com.stripe.android.connect.webview

internal object StripeConnectURL {
    internal fun getStripeURL(component: Component, publishableKey: String): String {
        return "https://connect-js.stripe.com/v1.0/android_webview.html" +
            "#component=${component.urlComponent}" +
            "&publicKey=$publishableKey"
    }

    internal enum class Component(val urlComponent: String) {
        PAYOUTS("payouts"),
    }
}
