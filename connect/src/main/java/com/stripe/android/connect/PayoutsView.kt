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
    defStyleAttr: Int = 0,
    embeddedComponentManager: EmbeddedComponentManager? = null
) : FrameLayout(context, attrs, defStyleAttr) {

    private var stripeWebViewClient: StripeConnectWebViewClient? = null

    init {
        inflate(getContext(), R.layout.stripe_connect_webview, this)

        embeddedComponentManager?.let {
            configureWebView(it)
        }
    }

    /**
     * Handles the back button press event.
     * Returns true if the back press was handled by the [PayoutsView],
     * false otherwise.
     */
    fun onBackPressed(): Boolean {
        val webView = findViewById<WebView>(R.id.stripe_web_view)
        return webView.canGoBack().also { canGoBack ->
            if (canGoBack) {
                webView.goBack()
            }
        }
    }

    /**
     * Set the [EmbeddedComponentManager] to use for this view.
     * Must be called when this view is created via XML.
     * Cannot be called more than once per instance.
     */
    fun setEmbeddedComponentManager(embeddedComponentManager: EmbeddedComponentManager) {
        if (stripeWebViewClient != null) {
            throw IllegalStateException("EmbeddedComponentManager already set")
        }
        configureWebView(embeddedComponentManager)
    }

    private fun configureWebView(embeddedComponentManager: EmbeddedComponentManager) {
        val webView = findViewById<WebView>(R.id.stripe_web_view)
        stripeWebViewClient = StripeConnectWebViewClient(
            embeddedComponentManager,
            StripeEmbeddedComponent.PAYOUTS,
        ).apply {
            configureAndLoadWebView(webView)
        }
    }
}
