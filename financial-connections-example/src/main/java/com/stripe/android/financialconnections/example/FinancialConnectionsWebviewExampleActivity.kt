package com.stripe.android.financialconnections.example

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.browser.customtabs.CustomTabsIntent

class FinancialConnectionsWebviewExampleActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_financialconnetions_webview_example)
        super.onCreate(savedInstanceState)
        setupToolbar()
        setupWebview()
    }

    private fun setupToolbar() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebview() {
        webView = findViewById(R.id.webview)
        with(webView) {
            // This configuration is required for Financial Connections to work. example to work.
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            webViewClient = buildWebviewClient()
            webChromeClient = buildWebChromeClient()
            loadUrl(GLITCH_EXAMPLE_URL)
        }
    }

    private fun buildWebviewClient() = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(
            view: WebView,
            webResourceRequest: WebResourceRequest
        ): Boolean {
            Log.d("Webview", "url loading: ${webResourceRequest.url}")
            val url = webResourceRequest.url.toString()
            return when {
                // Glitch-only: instance is idle it'll wake up and redirect when ready. This prevents
                // the redirect from opening in an external browser.
                url.startsWith(GLITCH_EXAMPLE_URL) -> false
                else -> {
                    CustomTabsIntent.Builder()
                        .build()
                        .launchUrl(this@FinancialConnectionsWebviewExampleActivity, Uri.parse(url))
                    true
                }
            }
        }
    }

    private fun buildWebChromeClient() = object : WebChromeClient() {

        @Suppress("MagicNumber")
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            toolbar.title = "Loading..."
            if (newProgress == 100) toolbar.title = "My webview-based app"
        }

        override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
            Log.d(
                "Webview",
                consoleMessage.message() + " -- From line " +
                    consoleMessage.lineNumber() + " of " +
                    consoleMessage.sourceId()
            )
            return super.onConsoleMessage(consoleMessage)
        }
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}

private const val GLITCH_EXAMPLE_URL = "https://connections-webview-example.glitch.me/"
