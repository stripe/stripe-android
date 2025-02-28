package com.stripe.android.identity.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebView.setWebContentsDebuggingEnabled
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.identity.VerificationFlowFinishable
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_WEB_VIEW
import com.stripe.android.identity.navigation.navigateToErrorScreenWithDefaultValues
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.viewmodel.IdentityViewModel
import androidx.browser.customtabs.CustomTabsIntent

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun VerificationWebViewScreen(
    navController: NavController,
    identityViewModel: IdentityViewModel,
    verificationFlowFinishable: VerificationFlowFinishable
) {
    val verificationPageState by identityViewModel.verificationPage.observeAsState(Resource.loading())
    val context = LocalContext.current

    CheckVerificationPageAndCompose(
        verificationPageResource = verificationPageState,
        onError = {
            identityViewModel.errorCause.postValue(it)
            navController.navigateToErrorScreenWithDefaultValues(context)
        }
    ) { verificationPage ->
        ScreenTransitionLaunchedEffect(
            identityViewModel = identityViewModel,
            screenName = SCREEN_NAME_WEB_VIEW
        )

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        loadWithOverviewMode = true
                    }

                    webChromeClient = object : android.webkit.WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage): Boolean {
                            android.util.Log.d(
                                "VerificationWebView",
                                consoleMessage.message() + " -- From line " +
                                    consoleMessage.lineNumber() + " of " +
                                    consoleMessage.sourceId()
                            )
                            return true
                        }

                        override fun onJsBeforeUnload(
                            view: WebView?,
                            url: String?,
                            message: String?,
                            result: android.webkit.JsResult
                        ): Boolean {
                            result.confirm()
                            return true
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest
                        ): Boolean {
                            val url = request.url.toString()
                            android.util.Log.d("VerificationWebView", "Attempting to load URL: $url")

                            return when {
                                // Let WebView handle verification URLs
                                url.startsWith(verificationPage.fallbackUrl) -> {
                                    when {
                                        url.contains("/success") -> {
                                            identityViewModel.identityAnalyticsRequestFactory.verificationSucceeded(
                                                isFromFallbackUrl = true
                                            )
                                            verificationFlowFinishable.finishWithResult(
                                                IdentityVerificationSheet.VerificationFlowResult.Completed
                                            )
                                            true
                                        }
                                        url.contains("/canceled") -> {
                                            identityViewModel.identityAnalyticsRequestFactory.verificationCanceled(
                                                isFromFallbackUrl = true
                                            )
                                            verificationFlowFinishable.finishWithResult(
                                                IdentityVerificationSheet.VerificationFlowResult.Canceled
                                            )
                                            true
                                        }
                                        else -> false
                                    }
                                }
                                // Open all other URLs in Custom Tabs
                                else -> {
                                    CustomTabsIntent.Builder()
                                        .build()
                                        .launchUrl(context, request.url)
                                    true
                                }
                            }
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            android.util.Log.e("VerificationWebView", "Error loading URL: ${request?.url}, error: ${error?.description}")
                            identityViewModel.identityAnalyticsRequestFactory.verificationFailed(
                                isFromFallbackUrl = true,
                                throwable = IllegalStateException("WebView error: ${error?.description}")
                            )
                            verificationFlowFinishable.finishWithResult(
                                IdentityVerificationSheet.VerificationFlowResult.Failed(
                                    IllegalStateException("WebView error: ${error?.description}")
                                )
                            )
                        }
                    }
                }
            },
            update = { webView ->
                android.util.Log.d("VerificationWebView", "Loading initial URL: ${verificationPage.fallbackUrl}")
                webView.clearCache(true)
                webView.loadUrl(verificationPage.fallbackUrl)
            }
        )
    }
} 