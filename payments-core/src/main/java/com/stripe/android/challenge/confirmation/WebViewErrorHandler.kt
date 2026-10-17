package com.stripe.android.challenge.confirmation

internal fun interface WebViewErrorHandler {
    operator fun invoke(error: WebViewError)
}
