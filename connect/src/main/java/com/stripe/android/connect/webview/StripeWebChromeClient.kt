package com.stripe.android.connect.webview

import android.webkit.WebChromeClient
import android.webkit.WebViewClient

/**
 * A [WebChromeClient] that provides additional functionality for Stripe Connect Embedded Component WebViews.
 *
 * This class is currently empty, but it could be used to add additional functionality in the future
 * Setting a [WebChromeClient] (even an empty one) is necessary for certain functionality, like
 * [WebViewClient.shouldOverrideUrlLoading] to work properly.
 */
class StripeWebChromeClient : WebChromeClient()
