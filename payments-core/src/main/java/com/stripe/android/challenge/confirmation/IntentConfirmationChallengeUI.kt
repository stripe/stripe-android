package com.stripe.android.challenge.confirmation

import android.content.Context
import android.view.ViewGroup
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView

@Composable
internal fun IntentConfirmationChallengeUI(
    hostUrl: String,
    bridgeHandler: ConfirmationChallengeBridgeHandler,
    showProgressIndicator: Boolean,
    webViewClientFactory: () -> WebViewClient,
    webViewFactory: (Context) -> IntentConfirmationChallengeWebView = { context ->
        IntentConfirmationChallengeWebView(
            context = context
        )
    }
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (showProgressIndicator) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .testTag(INTENT_CONFIRMATION_CHALLENGE_LOADER_TAG)
            )
        }

        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .testTag(INTENT_CONFIRMATION_CHALLENGE_WEB_VIEW_TAG),
            factory = { context ->
                webViewFactory(context).apply {
                    this.webViewClient = webViewClientFactory()
                    addBridgeHandler(bridgeHandler)
                    loadUrl(hostUrl)
                }
            },
            update = { view ->
                view.layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
            }
        )
    }
}

internal const val INTENT_CONFIRMATION_CHALLENGE_LOADER_TAG = "intent_confirmation_challenge_loader"
internal const val INTENT_CONFIRMATION_CHALLENGE_WEB_VIEW_TAG = "intent_confirmation_challenge_webview"
