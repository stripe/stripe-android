package com.stripe.android.financialconnections.example

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class FinancialConnectionsWebviewExampleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WebView.setWebContentsDebuggingEnabled(true)
        setContent {
            Column(modifier = Modifier.padding(horizontal = 10.dp)) {
                Button(
                    onClick = { onButtonClick() },
                ) {
                    Text("Launch Webview-based app")
                }
                Divider(modifier = Modifier.padding(vertical = 5.dp))
            }
        }
    }

    private fun onButtonClick() {
        val intent = Intent(
            this,
            WebviewContainerActivity::class.java
        )
        intent.putExtra("url", GLITCH_EXAMPLE_URL)
        startActivity(intent)
    }
}

class WebviewContainerActivity : Activity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this).also {
            it.settings.javaScriptEnabled = true
            it.settings.loadWithOverviewMode = true
            it.webViewClient = buildWebviewClient()
            it.settings.domStorageEnabled = true
            it.webChromeClient = buildWebChromeClient()
            it.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        val frameLayout = FrameLayout(this)
        frameLayout.addView(webView)
        webView.loadUrl(requireNotNull(intent.getStringExtra("url")))
        setContentView(frameLayout)
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
                        .launchUrl(this@WebviewContainerActivity, Uri.parse(url))
                    true
                }
            }
        }
    }

    private fun buildWebChromeClient() = object : WebChromeClient() {
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
