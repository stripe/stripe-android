package com.stripe.android.challenge.confirmation

import android.content.Context
import android.view.ViewGroup
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.stripe.android.uicore.R

@Composable
internal fun IntentConfirmationChallengeUI(
    modifier: Modifier = Modifier,
    hostUrl: String,
    userAgent: String,
    bridgeHandler: ConfirmationChallengeBridgeHandler,
    showProgressIndicator: Boolean,
    closeClicked: () -> Unit,
    webViewClientFactory: () -> WebViewClient,
    webViewFactory: (Context) -> IntentConfirmationChallengeWebView = { context ->
        IntentConfirmationChallengeWebView(
            context = context
        )
    }
) {
    Box(
        modifier = modifier.fillMaxSize()
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
                    updateUserAgent(userAgent)
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

        Box(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth(),
        ) {
            IconButton(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .testTag(INTENT_CONFIRMATION_CHALLENGE_CLOSE_BUTTON_TAG),
                onClick = closeClicked
            ) {
                Icon(
                    painter = painterResource(R.drawable.stripe_ic_material_close),
                    contentDescription = stringResource(com.stripe.android.R.string.stripe_close),
                    tint = Color.White
                )
            }
        }
    }
}

internal const val INTENT_CONFIRMATION_CHALLENGE_LOADER_TAG = "intent_confirmation_challenge_loader"
internal const val INTENT_CONFIRMATION_CHALLENGE_WEB_VIEW_TAG = "intent_confirmation_challenge_webview"
internal const val INTENT_CONFIRMATION_CHALLENGE_CLOSE_BUTTON_TAG = "intent_confirmation_challenge_close_button"
