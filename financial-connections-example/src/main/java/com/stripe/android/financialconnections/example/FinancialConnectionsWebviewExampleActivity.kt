package com.stripe.android.financialconnections.example

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


class FinancialConnectionsWebviewExampleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FinancialConnectionsWebViewScreen {
                val intent = Intent(
                    this,
                    FinancialConnectionsWebviewExampleRedirectActivity::class.java
                )
                intent.putExtra("url", "https://night-discreet-femur.glitch.me/")
                startActivity(intent)
            }
        }
    }

    @Composable
    private fun FinancialConnectionsWebViewScreen(onButtonClick: () -> Unit) {
        Column(modifier = Modifier.padding(horizontal = 10.dp)) {
            Button(
                onClick = { onButtonClick() },
            ) {
                Text("Connect Accounts!")
            }
            Divider(modifier = Modifier.padding(vertical = 5.dp))
        }
    }

}


class FinancialConnectionsWebviewExampleRedirectActivity : Activity() {
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        webView.webViewClient = buildWebviewClient()
        webView.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        )
        val frameLayout = FrameLayout(this)
        frameLayout.addView(webView)
        webView.loadUrl(intent.getStringExtra("url")!!)
        setContentView(frameLayout)
    }

    private fun buildWebviewClient() = object : WebViewClient() {
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
