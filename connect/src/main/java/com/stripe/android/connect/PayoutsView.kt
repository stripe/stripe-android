package com.stripe.android.connect

import android.content.Context
import android.util.AttributeSet
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.annotation.RestrictTo
import com.stripe.android.connect.webview.StripeConnectWebViewClient

@PrivateBetaConnectSDK
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PayoutsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    init {
        inflate(getContext(), R.layout.stripe_payouts_fragment, this)

        val webView = findViewById<WebView>(R.id.stripe_web_view)
        val stripeWebViewClient = StripeConnectWebViewClient(StripeEmbeddedComponent.PAYOUTS)
        stripeWebViewClient.configureAndLoadWebView(webView)
    }
}
