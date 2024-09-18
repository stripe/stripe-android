package com.stripe.android.connectsdk

import StripeConnectWebViewClient
import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView

/**
 *
 */
@OptIn(PrivateBetaConnectSDK::class)
open class EmbeddedComponentFragment internal constructor(
    private val component: EmbeddedComponent
) : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.stripe_embedded_component_fragment, container, false)
    }

    private lateinit var webView: WebView
    private lateinit var configuration: EmbeddedComponentManager.Configuration

    @OptIn(PrivateBetaConnectSDK::class)
    fun load(configuration: EmbeddedComponentManager.Configuration) {
        webView = requireView().findViewById(R.id.web_view)
        this.configuration = configuration
        setupWebView()
        loadWebView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val stripeWebViewClient = StripeConnectWebViewClient()

        webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true

            webViewClient = stripeWebViewClient
            addJavascriptInterface(stripeWebViewClient.WebLoginJsInterfaceInternal(), "AndroidInternal")
            addJavascriptInterface(stripeWebViewClient.WebLoginJsInterface(), "Android")
        }
    }

    private fun loadWebView() {
        val url = getUrl()
        webView.loadUrl(url)
    }

    private fun getUrl(): String {
        return "https://connect-js.stripe.com/v1.0/android_webview.html#component=${component.urlName}" +
            "&publicKey=${configuration.publishableKey}"
    }
}