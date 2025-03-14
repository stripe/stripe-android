package com.stripe.android.financialconnections.lite

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.annotation.RestrictTo
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs.Companion.EXTRA_ARGS
import com.stripe.android.financialconnections.lite.FinancialConnectionsLiteViewModel.ViewEffect.OpenAuthFlowWithUrl
import com.stripe.android.financialconnections.lite.di.Di
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult
import com.stripe.android.financialconnections.lite.FinancialConnectionsLiteViewModel.ViewEffect.FinishWithResult

internal class FinancialConnectionsSheetLiteActivity : ComponentActivity() {

    private lateinit var webView: WebView

    private val viewModel: FinancialConnectionsLiteViewModel by viewModels {
        FinancialConnectionsLiteViewModelFactory()
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

        lifecycleScope.launch {
            viewModel.viewEffects.collect { viewEffect ->
                when (viewEffect) {
                    is OpenAuthFlowWithUrl -> webView.loadUrl(viewEffect.url)
                    is FinishWithResult -> finishWithResult(viewEffect.result)
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(): WebView {
        return WebView(this).also {
            val webSettings = it.settings
            webSettings.javaScriptEnabled = true
            webSettings.useWideViewPort = true
            webSettings.loadWithOverviewMode = true
            it.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val url = request?.url
                    return handleUrl(url)
                }
            }
        }
    }

    private fun handleUrl(uri: Uri?): Boolean {
        if (uri != null && uri.scheme == "stripe-auth") {
            viewModel.handleUrl(uri)
            return true
        }
        return false
    }

    private fun finishWithResult(result: FinancialConnectionsSheetActivityResult) {
        setResult(RESULT_OK, Intent().putExtras(result.toBundle()))
        finish()
    }

    private fun openInCustomTab(url: String) {
        val builder = CustomTabsIntent.Builder()
        val customTabsIntent = builder.build()
        customTabsIntent.launchUrl(this, url.toUri())
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

internal class FinancialConnectionsLiteViewModelFactory : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val savedStateHandle = extras.createSavedStateHandle()
        val appContext = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Context

        if (modelClass.isAssignableFrom(FinancialConnectionsLiteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FinancialConnectionsLiteViewModel(
                savedStateHandle = savedStateHandle,
                applicationId = appContext.packageName,
                repository = Di.repository()
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
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
