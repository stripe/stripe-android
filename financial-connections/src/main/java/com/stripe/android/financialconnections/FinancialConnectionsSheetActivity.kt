package com.stripe.android.financialconnections

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.viewModel
import com.airbnb.mvrx.withState
import com.stripe.android.financialconnections.FinancialConnectionsSheetViewEffect.FinishWithResult
import com.stripe.android.financialconnections.FinancialConnectionsSheetViewEffect.OpenAuthFlowWithUrl
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult
import com.stripe.android.financialconnections.presentation.CreateBrowserIntentForUrl

internal class FinancialConnectionsSheetActivity :
    AppCompatActivity(R.layout.activity_financialconnections_sheet), MavericksView {

    val viewModel: FinancialConnectionsSheetViewModel by viewModel()
    val webView: WebView by lazy { findViewById<WebView>(R.id.webview) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WebView.setWebContentsDebuggingEnabled(true)
        setupWebView()
        viewModel.onEach { postInvalidate() }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        with(webView) {
            settings.javaScriptEnabled = true
            settings.setSupportMultipleWindows(true)
            settings.javaScriptCanOpenWindowsAutomatically = true
            webChromeClient = object : WebChromeClient() {
                override fun onCreateWindow(
                    view: WebView?,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: Message?
                ): Boolean {
                    WebView(context).also { it ->
                        with(it.settings) {
                            javaScriptEnabled = true
                            setSupportMultipleWindows(true)
                        }
                        it.webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                request?.url?.let {
                                    Log.d("StripeSdk", "webview: $it")
                                    startActivity(
                                        CreateBrowserIntentForUrl(
                                            context = this@FinancialConnectionsSheetActivity,
                                            uri = it
                                        )
                                    )
                                }
                                return true
                            }
                        }
                        webView.addView(it)
                        val transport = resultMsg!!.obj as WebView.WebViewTransport
                        transport.webView = it
                        resultMsg.sendToTarget()
                    }
                    return true
                }
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finishWithResult(FinancialConnectionsSheetActivityResult.Canceled)
    }

    /**
     * Handles new intents in the form of the redirect from the custom tab hosted auth flow
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // webView.loadUrl( "javascript:window.location.reload( true )" );
        viewModel.handleOnNewIntent(intent)
    }

    /**
     * handle state changes here.
     */
    override fun invalidate() {
        withState(viewModel) { state ->
            state.viewEffect?.let { viewEffect ->
                when (viewEffect) {
                    is OpenAuthFlowWithUrl -> {
                        webView.loadUrl(viewEffect.url)
//                        startForResult.launch(
//                            CreateBrowserIntentForUrl(
//                                context = this,
//                                uri = Uri.parse(viewEffect.url)
//                            )
//                        )
                    }
                    is FinishWithResult -> finishWithResult(
                        viewEffect.result
                    )
                }
                viewModel.onViewEffectLaunched()
            }
        }
    }

    private fun finishWithResult(result: FinancialConnectionsSheetActivityResult) {
        setResult(RESULT_OK, Intent().putExtras(result.toBundle()))
        finish()
    }
}
