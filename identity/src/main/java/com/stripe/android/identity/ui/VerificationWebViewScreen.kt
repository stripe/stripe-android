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
                        setSupportMultipleWindows(true)
                        javaScriptCanOpenWindowsAutomatically = true

                        // Enable remote debugging
                        setWebContentsDebuggingEnabled(true)
                    }

                    // Add WebChromeClient for additional debugging
                    webChromeClient = object : android.webkit.WebChromeClient() {
                        override fun onConsoleMessage(message: android.webkit.ConsoleMessage): Boolean {
                            android.util.Log.d("VerificationWebView", "Console: ${message.message()} -- From line ${message.lineNumber()} of ${message.sourceId()}")
                            return true
                        }

                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            android.util.Log.d("VerificationWebView", "Loading progress: $newProgress%")
                            super.onProgressChanged(view, newProgress)
                        }
                    }

                    webViewClient = object : WebViewClient() {
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

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            request?.url?.let { uri ->
                                val url = uri.toString()
                                android.util.Log.d("VerificationWebView", "Attempting to load URL: $url")
                                android.util.Log.d("VerificationWebView", "Fallback URL is: ${verificationPage.fallbackUrl}")

                                // Always open http/https URLs in Custom Tabs unless they're part of the verification flow
                                if (uri.scheme?.startsWith("http") == true && !url.startsWith(verificationPage.fallbackUrl)) {
                                    android.util.Log.d("VerificationWebView", "Opening external URL in Custom Tabs: $url")
                                    try {
                                        CustomTabsIntent.Builder()
                                            .build()
                                            .launchUrl(context, uri)
                                        return true
                                    } catch (e: Exception) {
                                        android.util.Log.e("VerificationWebView", "Error launching Custom Tabs", e)
                                    }
                                }

                                // Handle verification flow URLs
                                if (url.startsWith(verificationPage.fallbackUrl)) {
                                    android.util.Log.d("VerificationWebView", "Handling verification URL: $url")
                                    when {
                                        url.contains("/success") -> {
                                            android.util.Log.d("VerificationWebView", "Success URL detected")
                                            identityViewModel.identityAnalyticsRequestFactory.verificationSucceeded(
                                                isFromFallbackUrl = true
                                            )
                                            verificationFlowFinishable.finishWithResult(
                                                IdentityVerificationSheet.VerificationFlowResult.Completed
                                            )
                                            return true
                                        }
                                        url.contains("/canceled") -> {
                                            android.util.Log.d("VerificationWebView", "Canceled URL detected")
                                            identityViewModel.identityAnalyticsRequestFactory.verificationCanceled(
                                                isFromFallbackUrl = true
                                            )
                                            verificationFlowFinishable.finishWithResult(
                                                IdentityVerificationSheet.VerificationFlowResult.Canceled
                                            )
                                            return true
                                        }
                                    }
                                }
                            }
                            return false
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            android.util.Log.d("VerificationWebView", "Page finished loading: $url")

                            // Enhanced JavaScript to intercept link clicks and ensure logging works
                            view?.evaluateJavascript("""
                                (function() {
                                    if (window.linkClickHandlerInitialized) return;
                                    window.linkClickHandlerInitialized = true;
                                    
                                    function handleLinkClick(event) {
                                        var link = event.target.closest('a');
                                        if (link) {
                                            var href = link.href;
                                            console.log('Link clicked:', href);
                                            // Send message to Android
                                            window.AndroidInterface.onLinkClicked(href);
                                        }
                                    }
                                    
                                    // Remove any existing listeners to prevent duplicates
                                    document.removeEventListener('click', handleLinkClick, true);
                                    // Add the click listener in the capture phase
                                    document.addEventListener('click', handleLinkClick, true);
                                    
                                    console.log('Link click handler initialized');
                                })();
                            """.trimIndent()) { result ->
                                android.util.Log.d("VerificationWebView", "JavaScript initialization result: $result")
                            }
                        }
                    }

                    // Add JavaScript interface
                    addJavascriptInterface(object : Any() {
                        @android.webkit.JavascriptInterface
                        fun onLinkClicked(url: String) {
                            android.util.Log.d("VerificationWebView", "Link clicked (from JS interface): $url")
                            // Handle the URL on the main thread
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                if (url.startsWith("http") && !url.startsWith(verificationPage.fallbackUrl)) {
                                    try {
                                        android.util.Log.d("VerificationWebView", "Opening URL in Custom Tabs: $url")
                                        androidx.browser.customtabs.CustomTabsIntent.Builder()
                                            .build()
                                            .launchUrl(context, android.net.Uri.parse(url))
                                    } catch (e: Exception) {
                                        android.util.Log.e("VerificationWebView", "Error launching Custom Tabs", e)
                                    }
                                }
                            }
                        }
                    }, "AndroidInterface")
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