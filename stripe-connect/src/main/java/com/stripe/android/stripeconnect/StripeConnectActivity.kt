package com.stripe.android.stripeconnect

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView


internal class StripeConnectActivity : ComponentActivity() {

    private val appearance by lazy { intent.extras?.get("appearance") as? Appearance }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WebView.setWebContentsDebuggingEnabled(true)

        val component = intent.extras?.get("component-type") as StripeConnectComponent
        val account = intent.extras?.getString("account")

        setContentView(R.layout.stripe_connect_activity)

        findViewById<ComposeView>(R.id.compose_view).setContent {
            var usingChromeTab: Boolean? by remember { mutableStateOf(null) }
            when (usingChromeTab) {
                true -> ChromeCustomTab(component = component, account = account)
                false -> WebView(component = component, account = account)
                null -> {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center) {
                            Button(onClick = { usingChromeTab = true }) {
                                Text(text = "Chrome Custom Tab")
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Button(onClick = { usingChromeTab = false }) {
                                Text(text = "WebView")
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ChromeCustomTab(component: StripeConnectComponent, account: String?) {
        val context = LocalContext.current
        val locale = LocalConfiguration.current.locale
        LaunchedEffect(Unit) {
            val intent = CustomTabsIntent.Builder().build()
            intent.launchUrl(context, StripeConnect.uri(component.componentName(), account, locale))
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    private fun WebView(component: StripeConnectComponent, account: String?) {
        val locale = LocalConfiguration.current.locale
        val stripeConnectWebViewClient = remember { StripeConnectWebViewClient(appearance) }
        var refresh: () -> Unit by remember { mutableStateOf({}) }
        var goBack: () -> Unit by remember { mutableStateOf({}) }
        Scaffold { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Row(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(onClick = goBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
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
                            goBack = ::goBack

                            settings.javaScriptEnabled = true

                            settings.domStorageEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            setInitialScale(1)

                            isVerticalScrollBarEnabled = false
                            isHorizontalScrollBarEnabled = false
                            isScrollbarFadingEnabled = false
                            scrollBarStyle = WebView.SCROLLBARS_OUTSIDE_OVERLAY

                            addJavascriptInterface(stripeConnectWebViewClient.WebLoginJsInterface(), "Android")

                            webViewClient = stripeConnectWebViewClient
                            setWebChromeClient(object : WebChromeClient() {
                                override fun onPermissionRequest(request: PermissionRequest) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                                        requestPermissions(arrayOf(Manifest.permission.CAMERA), 42)
                                    }
                                    request.grant(request.resources)
                                }
                            })
                            setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
                                // Note - real download functionality would be more complex and native in-SDK
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }

                            loadUrl(StripeConnect.uri(component.componentName(), account, locale).toString())
                        }
                    }
                )
            }
        }
    }

    private var shown = false

    override fun onResume() {
        super.onResume()

        if (shown) {
            finish()
        }
        shown = true
    }

    companion object {
        fun createIntent(
            context: Context,
            component: StripeConnectComponent,
            account: String? = null,
            appearance: Appearance? = null,
        ): Intent {
            return Intent(context, StripeConnectActivity::class.java).apply {
                putExtra("component-type", component)
                account?.let { putExtra("account", it) }
                appearance?.let { putExtra("appearance", it) }
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