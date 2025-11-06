package com.stripe.android.challenge.confirmation

import android.content.Context
import android.webkit.WebView

internal class FakeIntentConfirmationChallengeWebView(
    context: Context,
) : WebView(context) {

    var loadedUrl: String? = null
        private set

    override fun loadUrl(url: String) {
        loadedUrl = url
        // Don't call super.loadUrl() to avoid actual WebView operations in tests
    }
}
