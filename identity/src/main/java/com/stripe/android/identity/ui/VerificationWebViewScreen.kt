package com.stripe.android.identity.ui

import android.annotation.SuppressLint
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
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
                    settings.javaScriptEnabled = true
                    webViewClient = object : WebViewClient() {
                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
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

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            // Check if the URL indicates completion or cancellation
                            when {
                                url?.contains("/success") == true -> {
                                    identityViewModel.identityAnalyticsRequestFactory.verificationSucceeded(
                                        isFromFallbackUrl = true
                                    )
                                    verificationFlowFinishable.finishWithResult(
                                        IdentityVerificationSheet.VerificationFlowResult.Completed
                                    )
                                }
                                url?.contains("/canceled") == true -> {
                                    identityViewModel.identityAnalyticsRequestFactory.verificationCanceled(
                                        isFromFallbackUrl = true
                                    )
                                    verificationFlowFinishable.finishWithResult(
                                        IdentityVerificationSheet.VerificationFlowResult.Canceled
                                    )
                                }
                            }
                        }
                    }
                }
            },
            update = { webView ->
                webView.loadUrl(verificationPage.fallbackUrl)
            }
        )
    }
} 