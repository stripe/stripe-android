package com.stripe.android.stripeconnect

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

internal class StripeConnectActivity : ComponentActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val component = intent.extras?.get("component-type") as StripeConnectComponent

        setContent {
            val stripeConnectWebViewClient = remember { StripeConnectWebViewClient() }
            var refresh: () -> Unit by remember { mutableStateOf({}) }
            Scaffold { paddingValues ->
                Column(modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)) {
                    Row(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(onClick = refresh) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Button(onClick = { this@StripeConnectActivity.finish() }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    Divider()
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context: Context ->
                            WebView(context).apply {
                                refresh = ::reload
                                settings.javaScriptEnabled = true
                                webViewClient = stripeConnectWebViewClient
                                setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
                                    // TODO - add download support here
                                }
                                addJavascriptInterface(stripeConnectWebViewClient.WebLoginJsInterface(), "Android")
                                loadUrl(StripeConnect.uri(component.componentName()).toString())
                            }
                        }
                    )
                }
            }
        }
    }

    companion object {
        fun createIntent(context: Context, component: StripeConnectComponent): Intent {
            return Intent(context, StripeConnectActivity::class.java).apply {
                putExtra("component-type", component)
            }
        }
    }


    private fun StripeConnectComponent.componentName(): String {
        return when (this) {
            StripeConnectComponent.AccountManagement -> "account-management"
            StripeConnectComponent.AccountOnboarding -> "account-onboarding"
            StripeConnectComponent.Documents -> "documents"
            StripeConnectComponent.Payments -> "payments"
            StripeConnectComponent.PaymentDetails -> "payment-details"
            StripeConnectComponent.Payouts -> "payouts"
            StripeConnectComponent.PayoutsList -> "payouts-list"
        }
    }
}