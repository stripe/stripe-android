package com.stripe.android.financialconnections.webview

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult
import com.stripe.android.financialconnections.webview.FinancialConnectionsWebviewViewModel.ViewEffect.FinishWithResult
import com.stripe.android.financialconnections.webview.FinancialConnectionsWebviewViewModel.ViewEffect.OpenAuthFlowWithUrl
import kotlinx.coroutines.launch

internal class FinancialConnectionsWebviewActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    private val viewModel: FinancialConnectionsWebviewViewModel by viewModels {
        FinancialConnectionsWebviewViewModelFactory(application)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = getArgs(intent)
        if (args == null) {
            finish()
            return
        }

        val frameLayout = FrameLayout(this)
        webView = setupWebView()
        frameLayout.addView(webView)
        setContentView(frameLayout)

        // observe viewmodel
        lifecycleScope.launch {
            viewModel.viewEffects.collect { viewEffect ->
                when (viewEffect) {
                    is OpenAuthFlowWithUrl -> webView.loadUrl(viewEffect.url)
                    is FinishWithResult -> finishWithResult(viewEffect.result)
                }
            }
        }

    }

    private fun setupWebView(): WebView {
        return WebView(this).also {
            val webSettings = it.settings
            webSettings.javaScriptEnabled = true
            webSettings.useWideViewPort = true
            webSettings.loadWithOverviewMode = true

            it.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val url = request.url.toString()

                    when {
                        url.startsWith("stripe-auth://link-accounts/${application.packageName}") -> {
                            // Handle the redirect from the auth flow finish deep links
                            viewModel.onAuthFlowFinished(url)
                        }
                        else -> {
                            // Open external deep links on the system browser
                            CustomTabsIntent.Builder()
                                .build()
                                .launchUrl(
                                    /* context = */ this@FinancialConnectionsWebviewActivity,
                                    /* url = */ Uri.parse(request.url.toString())
                                )
                        }
                    }

                    return true
                }
            }
        }
    }

    private fun finishWithResult(result: FinancialConnectionsSheetActivityResult) {
        setResult(RESULT_OK, Intent().putExtras(result.toBundle()))
        finish()
    }

    companion object {
        private const val EXTRA_ARGS = "FinancialConnectionsWebviewActivityArgs"
        fun intent(context: Context, args: FinancialConnectionsSheetActivityArgs): Intent {
            return Intent(context, FinancialConnectionsWebviewActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                putExtra(EXTRA_ARGS, args)
            }
        }

        fun getArgs(intent: Intent): FinancialConnectionsSheetActivityArgs? {
            return intent.getParcelableExtra(EXTRA_ARGS)
        }
    }
}

class FinancialConnectionsWebviewViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        // Obtain SavedStateHandle from CreationExtras
        val savedStateHandle = extras.createSavedStateHandle()

        if (modelClass.isAssignableFrom(FinancialConnectionsWebviewViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FinancialConnectionsWebviewViewModel(application, savedStateHandle) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

