package com.stripe.android.connectsdk

import StripeConnectWebViewClient
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity

internal class EmbeddedComponentActivity : ComponentActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.stripe_embedded_component_activity)

        webView = findViewById(R.id.web_view)
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
        val url = getUrl(intent)
        webView.loadUrl(url)
    }

    @OptIn(PrivateBetaConnectSDK::class)
    private fun getUrl(intent: Intent): String {
        val component = intent.extras?.getSerializable(COMPONENT_EXTRA) as EmbeddedComponent
        val configuration = intent.extras?.get(CONFIGURATION_EXTRA) as EmbeddedComponentManager.Configuration
        return "https://connect-js.stripe.com/v1.0/android_webview.html#component=${component.urlName}" +
            "&publicKey=${configuration.publishableKey}"
    }

    internal enum class EmbeddedComponent(val urlName: String) {
        AccountOnboarding("account-onboarding"),
        Payouts("payouts"),
    }

    companion object {
        private const val COMPONENT_EXTRA = "component"
        private const val CONFIGURATION_EXTRA = "configuration"

        @OptIn(PrivateBetaConnectSDK::class)
        internal fun newIntent(
            activity: ComponentActivity,
            component: EmbeddedComponent,
            configuration: EmbeddedComponentManager.Configuration,
        ): Intent {
            return Intent(activity, EmbeddedComponentActivity::class.java).apply {
                putExtra(COMPONENT_EXTRA, component)
                putExtra(CONFIGURATION_EXTRA, configuration)
            }
        }
    }
}
