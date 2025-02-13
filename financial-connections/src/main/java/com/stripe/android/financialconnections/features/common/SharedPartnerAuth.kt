package com.stripe.android.financialconnections.features.common

import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthPreviewParameterProvider
import com.stripe.android.financialconnections.features.partnerauth.SharedPartnerAuthState
import com.stripe.android.financialconnections.features.partnerauth.SharedPartnerAuthState.AuthenticationStatus
import com.stripe.android.financialconnections.features.partnerauth.SharedPartnerAuthState.AuthenticationStatus.Action
import com.stripe.android.financialconnections.features.partnerauth.SharedPartnerAuthState.ViewEffect
import com.stripe.android.financialconnections.model.Entry
import com.stripe.android.financialconnections.model.OauthPrepane
import com.stripe.android.financialconnections.presentation.Async
import com.stripe.android.financialconnections.presentation.Async.Fail
import com.stripe.android.financialconnections.presentation.Async.Loading
import com.stripe.android.financialconnections.presentation.Async.Success
import com.stripe.android.financialconnections.presentation.Async.Uninitialized
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeState
import com.stripe.android.financialconnections.presentation.WebAuthFlowState
import com.stripe.android.financialconnections.presentation.collectAsState
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.LocalImageLoader
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton.Type
import com.stripe.android.financialconnections.ui.sdui.fromHtml
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.colors
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.typography
import com.stripe.android.financialconnections.ui.theme.LazyLayout
import com.stripe.android.uicore.image.StripeImage

@Composable
internal fun SharedPartnerAuth(
    state: SharedPartnerAuthState,
    onContinueClick: () -> Unit,
    onCancelClick: () -> Unit,
    onClickableTextClick: (String) -> Unit,
    onWebAuthFlowFinished: (WebAuthFlowState) -> Unit,
    onViewEffectLaunched: () -> Unit,
    inModal: Boolean
) {
    val viewModel = parentViewModel()

    val webAuthFlow = viewModel.collectAsState(FinancialConnectionsSheetNativeState::webAuthFlow)
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(webAuthFlow.value) {
        onWebAuthFlowFinished(webAuthFlow.value)
    }

    state.viewEffect?.let { viewEffect ->
        LaunchedEffect(viewEffect) {
            when (viewEffect) {
                is ViewEffect.OpenUrl -> uriHandler.openUri(viewEffect.url)
                is ViewEffect.OpenPartnerAuth -> viewModel.openPartnerAuthFlowInBrowser(viewEffect.url)
            }
            onViewEffectLaunched()
        }
    }

    SharedPartnerAuthContent(
        inModal = inModal,
        state = state,
        onClickableTextClick = onClickableTextClick,
        onContinueClick = onContinueClick,
        onCancelClick = onCancelClick,
    )
}

@Composable
private fun SharedPartnerAuthContent(
    state: SharedPartnerAuthState,
    inModal: Boolean,
    onClickableTextClick: (String) -> Unit,
    onContinueClick: () -> Unit,
    onCancelClick: () -> Unit,
) {
    SharedPartnerAuthBody(
        inModal = inModal,
        state = state,
        onClickableTextClick = onClickableTextClick,
        onCancelClick = onCancelClick,
        onContinueClick = onContinueClick,
    )
}

@Composable
private fun SharedPartnerLoading(inModal: Boolean) {
    LoadingShimmerEffect { shimmerBrush ->
        Column(
            Modifier.padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.size(24.dp))
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(shimmerBrush, RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.size(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .background(shimmerBrush, RoundedCornerShape(8.dp))

            )
            Spacer(modifier = Modifier.size(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .background(shimmerBrush, RoundedCornerShape(8.dp))

            )
            Spacer(modifier = Modifier.size(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(.50f)
                    .height(16.dp)
                    .background(shimmerBrush, RoundedCornerShape(8.dp))

            )
            if (inModal) {
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(shimmerBrush, RoundedCornerShape(8.dp))

            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(shimmerBrush, RoundedCornerShape(8.dp))

            )
            Spacer(modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun SharedPartnerAuthBody(
    state: SharedPartnerAuthState,
    inModal: Boolean,
    onCancelClick: () -> Unit,
    onContinueClick: () -> Unit,
    onClickableTextClick: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        state.payload()?.let {
            LoadedContent(
                showInModal = inModal,
                authenticationStatus = state.authenticationStatus,
                payload = it,
                onContinueClick = onContinueClick,
                onCancelClick = onCancelClick,
                onClickableTextClick = onClickableTextClick,
            )
        } ?: SharedPartnerLoading(inModal)
    }
}

@Composable
private fun LoadedContent(
    showInModal: Boolean,
    authenticationStatus: Async<AuthenticationStatus>,
    payload: SharedPartnerAuthState.Payload,
    onContinueClick: () -> Unit,
    onCancelClick: () -> Unit,
    onClickableTextClick: (String) -> Unit
) {
    when (authenticationStatus) {
        is Uninitialized,
        is Loading,
        is Fail,
        is Success -> when (payload.authSession.isOAuth) {
            true -> PrePaneContent(
                // show loading prepane when authenticationStatus
                // is Loading or Success (completing auth after redirect)
                authenticationStatus = authenticationStatus,
                showInModal = showInModal,
                onContinueClick = onContinueClick,
                onCancelClick = onCancelClick,
                content = requireNotNull(payload.authSession.display?.text?.oauthPrepane),
                onClickableTextClick = onClickableTextClick,
            )

            false -> SharedPartnerLoading(showInModal)
        }
    }
}

@Composable
private fun PrePaneContent(
    showInModal: Boolean,
    content: OauthPrepane,
    authenticationStatus: Async<AuthenticationStatus>,
    onContinueClick: () -> Unit,
    onCancelClick: () -> Unit,
    onClickableTextClick: (String) -> Unit,
) {
    val bodyPadding = remember(showInModal) {
        PaddingValues(top = if (showInModal) 0.dp else 24.dp)
    }

    LazyLayout(
        inModal = showInModal,
        // Overrides padding values to allow full-span prepane image background
        bodyPadding = bodyPadding,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        body = {
            item {
                PrepaneHeader(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    content = content
                )
            }
            items(content.body.entries) { bodyItem ->
                when (bodyItem) {
                    is Entry.Image -> PrepaneImage(bodyItem)

                    is Entry.Text -> AnnotatedText(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        text = TextResource.Text(fromHtml(bodyItem.content)),
                        onClickableTextClick = onClickableTextClick,
                        defaultStyle = typography.bodyMedium.copy(
                            color = colors.textDefault,
                        ),
                    )
                }
            }
        },
        footer = {
            PrepaneFooter(
                onContinueClick = onContinueClick,
                onCancelClick = onCancelClick,
                status = authenticationStatus,
                oAuthPrepane = content,
                showInModal = showInModal,
            )
        }
    )
}

@Composable
internal fun PrepaneImage(bodyItem: Entry.Image) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(PHONE_BACKGROUND_HEIGHT_DP.dp)
    ) {
        // left gradient
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            colors.backgroundSecondary,
                            colors.borderNeutral,
                        ),
                    )
                )
                .weight(1f)
                .fillMaxHeight()
        )

        // left separator
        Box(
            modifier = Modifier
                .background(color = colors.backgroundSecondary)
                .width(8.dp)
                .fillMaxHeight()
        )
        // image / gif
        GifWebView(
            modifier = Modifier
                .width(PHONE_BACKGROUND_WIDTH_DP.dp)
                .fillMaxHeight(),
            bodyItem.content.default!!
        )
        // right separator
        Box(
            modifier = Modifier
                .background(color = colors.backgroundSecondary)
                .width(8.dp)
                .fillMaxHeight()
        )
        // right gradient
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            colors.borderNeutral,
                            colors.backgroundSecondary,
                        ),
                    )
                )
                .weight(1f)
                .fillMaxHeight()
        )
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun PrepaneFooter(
    onContinueClick: () -> Unit,
    onCancelClick: () -> Unit,
    status: Async<AuthenticationStatus>,
    oAuthPrepane: OauthPrepane,
    showInModal: Boolean,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FinancialConnectionsButton(
            onClick = onContinueClick,
            type = Type.Primary,
            loading = status is Loading && status()?.action == Action.AUTHENTICATING,
            enabled = status !is Loading,
            modifier = Modifier
                .semantics { testTagsAsResourceId = true }
                .testTag("prepane_cta")
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = oAuthPrepane.cta.text,
                    textAlign = TextAlign.Center
                )
                oAuthPrepane.cta.icon?.default?.let {
                    Spacer(modifier = Modifier.size(12.dp))
                    StripeImage(
                        url = it,
                        contentDescription = null,
                        imageLoader = LocalImageLoader.current,
                        errorContent = { },
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        FinancialConnectionsButton(
            onClick = onCancelClick,
            type = Type.Secondary,
            enabled = status !is Loading,
            modifier = Modifier
                .semantics { testTagsAsResourceId = true }
                .testTag("cancel_cta")
                .fillMaxWidth()
        ) {
            Text(
                text = stringResource(
                    id = if (showInModal) {
                        R.string.stripe_prepane_cancel_cta
                    } else {
                        R.string.stripe_prepane_choose_different_bank_cta
                    }
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PrepaneHeader(
    content: OauthPrepane,
    modifier: Modifier = Modifier
) {
    val title = remember(content.title) { TextResource.Text(fromHtml(content.title)) }
    val subtitle = remember(content.subtitle) { TextResource.Text(fromHtml(content.subtitle)) }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        content.institutionIcon?.default?.let {
            InstitutionIcon(institutionIcon = it)
        }
        AnnotatedText(
            text = title,
            onClickableTextClick = { },
            defaultStyle = typography.headingLarge.copy(
                color = colors.textDefault
            ),
        )
        AnnotatedText(
            text = subtitle,
            onClickableTextClick = { },
            defaultStyle = typography.bodyMedium.copy(
                color = colors.textDefault
            ),
        )
    }
}

@Composable
private fun GifWebView(
    modifier: Modifier,
    gifUrl: String
) {
    val isPreview = LocalInspectionMode.current
    val htmlContent = remember(gifUrl) {
        buildString {
            append("<html><head><style>img{width:100%; height:auto;}</style></head>")
            append("<body style=\"margin: 0; padding: 0\">")
            append("<img src=\"$gifUrl\" style=\"width:100%;height:auto;\" />")
            append("</body></html>")
        }
    }
    val backgroundColor = colors.backgroundSecondary.toArgb()
    AndroidView(
        modifier = modifier.background(Color.Transparent),
        factory = {
            WebView(it).apply {
                /**
                 * WebView crashes when leaving the composition. Adding alpha acts as a workaround.
                 * https://stackoverflow.com/questions/74829526/
                 */
                setBackgroundColor(backgroundColor)
                alpha = WEBVIEW_ALPHA
                layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                if (!isPreview) {
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    isVerticalFadingEdgeEnabled = false
                }
                loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
            }
        },
        update = {
            it.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        }
    )
}

@Preview(
    group = "SharedPartnerAuth"
)
@Composable
internal fun PartnerAuthPreview(
    @PreviewParameter(PartnerAuthPreviewParameterProvider::class)
    state: SharedPartnerAuthState
) {
    FinancialConnectionsPreview {
        SharedPartnerAuthContent(
            state = state,
            inModal = false,
            onClickableTextClick = {},
            onContinueClick = {},
            onCancelClick = {}
        )
    }
}

@Preview(
    group = "SharedPartnerAuth - Drawer"
)
@Composable
internal fun PartnerAuthDrawerPreview(
    @PreviewParameter(PartnerAuthPreviewParameterProvider::class)
    state: SharedPartnerAuthState
) {
    FinancialConnectionsPreview {
        Box(modifier = Modifier.background(colors.background)) {
            SharedPartnerAuthContent(
                state = state,
                inModal = true,
                onClickableTextClick = {},
                onContinueClick = {},
                onCancelClick = {}
            )
        }
    }
}

private const val PHONE_BACKGROUND_WIDTH_DP = 240
private const val PHONE_BACKGROUND_HEIGHT_DP = 200
private const val WEBVIEW_ALPHA = 0.99f
