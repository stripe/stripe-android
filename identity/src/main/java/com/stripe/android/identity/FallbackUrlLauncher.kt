package com.stripe.android.identity

internal interface FallbackUrlLauncher {
    fun launchFallbackUrl(fallbackUrl: String)
}
