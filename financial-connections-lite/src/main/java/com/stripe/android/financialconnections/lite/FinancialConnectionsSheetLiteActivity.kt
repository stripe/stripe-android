package com.stripe.android.financialconnections.lite

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.annotation.RestrictTo
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs.Companion.EXTRA_ARGS
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs.ForData
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs.ForInstantDebits
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs.ForToken
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult
import com.stripe.android.financialconnections.lite.FinancialConnectionsLiteViewModel.ViewEffect.FinishWithResult
import com.stripe.android.financialconnections.lite.FinancialConnectionsLiteViewModel.ViewEffect.OpenAuthFlowWithUrl
import com.stripe.android.financialconnections.lite.FinancialConnectionsLiteViewModel.ViewEffect.OpenCustomTab
import kotlinx.coroutines.launch

internal class FinancialConnectionsSheetLiteActivity : ComponentActivity(R.layout.stripe_activity_lite) {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar

    private val viewModel: FinancialConnectionsLiteViewModel by viewModels {
        FinancialConnectionsLiteViewModel.Factory()
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.stripe_activity_lite)

        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        setupProgressBar()
        setupWebView()

        lifecycleScope.launch {
            viewModel.viewEffects.collect { viewEffect ->
                when (viewEffect) {
                    is OpenAuthFlowWithUrl -> webView.loadUrl(viewEffect.url)
                    is FinishWithResult -> finishWithResult(viewEffect.result)
                    is OpenCustomTab -> openCustomTab(viewEffect.url)
                }
            }
        }
    }

    private fun setupProgressBar() {
        val color = when (getArgs(intent)) {
            null, is ForData, is ForToken -> R.color.stripe_financial_connections
            is ForInstantDebits -> R.color.stripe_link
        }.let { ContextCompat.getColor(this, it) }
        progressBar.progressDrawable.setTint(color)
        progressBar.indeterminateDrawable.setTint(color)
        progressBar.isVisible = true
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(): WebView {
        return webView.apply {
            settings.javaScriptEnabled = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    progressBar.visibility = if (newProgress < 100) View.VISIBLE else View.GONE
                    progressBar.progress = newProgress
                }
            }
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    return handleUrl(request?.url)
                }
            }
        }
    }

    private fun openCustomTab(uri: String) {
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
            .setBookmarksButtonEnabled(false)
            .build()
            .launchUrl(this, uri.toUri())
    }

    private fun handleUrl(uri: Uri?): Boolean {
        if (uri != null) {
            viewModel.handleUrl(uri.toString())
            return true
        }
        return false
    }

    private fun finishWithResult(result: FinancialConnectionsSheetActivityResult) {
        setResult(RESULT_OK, Intent().putExtras(result.toBundle()))
        finish()
    }

    companion object {
        fun intent(context: Context, args: FinancialConnectionsSheetActivityArgs): Intent {
            return Intent(context, FinancialConnectionsSheetLiteActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                putExtra(EXTRA_ARGS, args)
            }
        }

        fun getArgs(intent: Intent): FinancialConnectionsSheetActivityArgs? {
            return intent.getParcelableExtra(EXTRA_ARGS)
        }
    }
}

/**
 * Creates an [Intent] to launch the [FinancialConnectionsSheetLiteActivity].
 *
 * @param context the context to use for creating the intent
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun intentBuilder(context: Context): (FinancialConnectionsSheetActivityArgs) -> Intent =
    { args: FinancialConnectionsSheetActivityArgs ->
        FinancialConnectionsSheetLiteActivity.intent(
            context = context,
            args = args
        )
    }
