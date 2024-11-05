package com.stripe.android.connect

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.annotation.RestrictTo
import androidx.fragment.app.Fragment
import com.stripe.android.connect.webview.StripeConnectURL.Component
import com.stripe.android.connect.webview.StripeConnectWebViewClient

@PrivateBetaConnectSDK
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PayoutsFragment internal constructor() : Fragment() {

    private val stripeWebViewClient by lazy { StripeConnectWebViewClient(Component.PAYOUTS) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.stripe_payouts_fragment, container, false)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val webView = view.findViewById<WebView>(R.id.stripe_web_view)
        stripeWebViewClient.configureAndLoadWebView(webView)
    }

    @PrivateBetaConnectSDK
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {

        @PrivateBetaConnectSDK
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun create(): PayoutsFragment {
            return PayoutsFragment()
        }
    }
}
