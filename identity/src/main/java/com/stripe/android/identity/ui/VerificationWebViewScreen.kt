package com.stripe.android.identity.ui

import android.annotation.SuppressLint
import android.os.Build
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.stripe.android.identity.IdentityVerificationSheet.VerificationFlowResult
import com.stripe.android.identity.VerificationFlowFinishable
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_WEB_VIEW
import com.stripe.android.identity.navigation.navigateToErrorScreenWithDefaultValues
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.viewmodel.IdentityViewModel

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

                    webChromeClient = object : WebChromeClient() {
                        override fun onJsBeforeUnload(
                            view: WebView?,
                            url: String?,
                            message: String?,
                            result: JsResult
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
                            
                            return when {
                                url.startsWith(verificationPage.fallbackUrl) -> {
                                    when {
                                        url.contains("/success") -> {
                                            identityViewModel.identityAnalyticsRequestFactory
                                                .verificationSucceeded(isFromFallbackUrl = true)
                                            verificationFlowFinishable.finishWithResult(
                                                VerificationFlowResult.Completed
                                            )
                                            true
                                        }
                                        url.contains("/canceled") -> {
                                            identityViewModel.identityAnalyticsRequestFactory
                                                .verificationCanceled(isFromFallbackUrl = true)
                                            verificationFlowFinishable.finishWithResult(
                                                VerificationFlowResult.Canceled
                                            )
                                            true
                                        }
                                        else -> false
                                    }
                                }
                                else -> {
                                    CustomTabsIntent.Builder()
                                        .build()
                                        .launchUrl(context, request.url)
                                    true
                                }
                            }
                        }

                        @RequiresApi(Build.VERSION_CODES.M)
                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            val errorMessage = "WebView error: ${error?.description}"
                            identityViewModel.identityAnalyticsRequestFactory.verificationFailed(
                                isFromFallbackUrl = true,
                                throwable = IllegalStateException(errorMessage)
                            )
                            verificationFlowFinishable.finishWithResult(
                                VerificationFlowResult.Failed(IllegalStateException(errorMessage))
                            )
                        }
                    }
                }
            },
            update = { webView ->
                webView.clearCache(true)
                webView.loadUrl(verificationPage.fallbackUrl)
            }
        )
    }
} 