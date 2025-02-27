package com.stripe.android.identity.navigation

internal object VerificationWebViewDestination : IdentityTopLevelDestination() {
    const val VERIFICATION_WEBVIEW = "VerificationWebView"
    val ROUTE = object : DestinationRoute() {
        override val routeBase = VERIFICATION_WEBVIEW
    }

    override val destinationRoute = ROUTE
} 