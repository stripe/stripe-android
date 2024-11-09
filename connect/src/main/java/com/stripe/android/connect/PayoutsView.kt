package com.stripe.android.connect

import android.content.Context
import android.util.AttributeSet
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.annotation.RestrictTo
import com.stripe.android.connect.webview.StripeConnectWebViewClient

@PrivateBetaConnectSDK
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PayoutsView @JvmOverloads internal constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    embeddedComponentManager: EmbeddedComponentManager? = null
) : FrameLayout(context, attrs, defStyleAttr) {

    init {
        checkNotNull(embeddedComponentManager) {
            "EmbeddedComponentManager must not be null." +
                "Currently only programmatic creation of PayoutsView is supported."
        }

        inflate(getContext(), R.layout.stripe_connect_webview, this)

        val webView = findViewById<WebView>(R.id.stripe_web_view)
        val stripeWebViewClient = StripeConnectWebViewClient(
            embeddedComponentManager,
            StripeEmbeddedComponent.PAYOUTS,
        )
        stripeWebViewClient.configureAndLoadWebView(webView)
    }
}
