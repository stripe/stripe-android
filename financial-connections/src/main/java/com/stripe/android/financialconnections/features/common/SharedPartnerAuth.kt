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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.compose.collectAsState
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.exception.InstitutionPlannedDowntimeError
import com.stripe.android.financialconnections.exception.InstitutionUnplannedDowntimeError
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthPreviewParameterProvider
import com.stripe.android.financialconnections.features.partnerauth.SharedPartnerAuthState
import com.stripe.android.financialconnections.features.partnerauth.SharedPartnerAuthState.ViewEffect
import com.stripe.android.financialconnections.model.Entry
import com.stripe.android.financialconnections.model.OauthPrepane
import com.stripe.android.financialconnections.presentation.WebAuthFlowState
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.LocalImageLoader
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton.Type
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsModalBottomSheetLayout
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.sdui.fromHtml
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.v3Colors
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.v3Typography
import com.stripe.android.financialconnections.ui.theme.Layout
import com.stripe.android.uicore.image.StripeImage
import kotlinx.coroutines.launch

@Composable
internal fun SharedPartnerAuth(
    state: SharedPartnerAuthState,
    onContinueClick: () -> Unit,
    onCancelClick: () -> Unit,
    onSelectAnotherBank: () -> Unit,
    onClickableTextClick: (String) -> Unit,
    onEnterDetailsManually: () -> Unit,
    onWebAuthFlowFinished: (WebAuthFlowState) -> Unit,
    onViewEffectLaunched: () -> Unit
) {
    val viewModel = parentViewModel()

    val webAuthFlow = viewModel.collectAsState { it.webAuthFlow }
    val uriHandler = LocalUriHandler.current

    val bottomSheetState = rememberModalBottomSheetState(
        ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )

    LaunchedEffect(webAuthFlow.value) {
        onWebAuthFlowFinished(webAuthFlow.value)
    }

    state.viewEffect?.let { viewEffect ->
        LaunchedEffect(viewEffect) {
            when (viewEffect) {
                is ViewEffect.OpenBottomSheet -> bottomSheetState.show()
                is ViewEffect.OpenUrl -> uriHandler.openUri(viewEffect.url)
                is ViewEffect.OpenPartnerAuth -> viewModel.openPartnerAuthFlowInBrowser(viewEffect.url)
            }
            onViewEffectLaunched()
        }
    }

    SharedPartnerAuthContent(
        state = state,
        bottomSheetState = bottomSheetState,
        onClickableTextClick = onClickableTextClick,
        onSelectAnotherBank = onSelectAnotherBank,
        onEnterDetailsManually = onEnterDetailsManually,
        onCloseClick = { viewModel.onCloseWithConfirmationClick(state.pane) },
        onContinueClick = onContinueClick,
        onCancelClick = onCancelClick,
        onCloseFromErrorClick = viewModel::onCloseFromErrorClick
    )
}

@Composable
private fun SharedPartnerAuthContent(
    state: SharedPartnerAuthState,
    bottomSheetState: ModalBottomSheetState,
    onClickableTextClick: (String) -> Unit,
    onSelectAnotherBank: () -> Unit,
    onEnterDetailsManually: () -> Unit,
    onContinueClick: () -> Unit,
    onCloseClick: () -> Unit,
    onCancelClick: () -> Unit,
    onCloseFromErrorClick: (Throwable) -> Unit
) {
    val scope = rememberCoroutineScope()
    FinancialConnectionsModalBottomSheetLayout(
        sheetState = bottomSheetState,
        sheetContent = {
            state.dataAccess?.let {
                DataAccessBottomSheetContent(
                    dataDialog = it,
                    onConfirmModalClick = { scope.launch { bottomSheetState.hide() } },
                    onClickableTextClick = onClickableTextClick
                )
            } ?: Spacer(modifier = Modifier.size(16.dp))
        },
        content = {
            SharedPartnerAuthBody(
                state = state,
                onCloseClick = onCloseClick,
                onSelectAnotherBank = onSelectAnotherBank,
                onEnterDetailsManually = onEnterDetailsManually,
                onCloseFromErrorClick = onCloseFromErrorClick,
                onClickableTextClick = onClickableTextClick,
                onCancelClick = onCancelClick,
                onContinueClick = onContinueClick,
            )
        }
    )
}

@Composable
@Suppress("MagicNumber")
private fun SharedPartnerLoading() {
    LoadingShimmerEffect { shimmerBrush ->
        Column(
            Modifier.padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.size(16.dp))
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
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(shimmerBrush, RoundedCornerShape(8.dp))

            )
            Spacer(modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun SharedPartnerAuthBody(
    state: SharedPartnerAuthState,
    onCloseClick: () -> Unit,
    onCancelClick: () -> Unit,
    onSelectAnotherBank: () -> Unit,
    onEnterDetailsManually: () -> Unit,
    onCloseFromErrorClick: (Throwable) -> Unit,
    onContinueClick: () -> Unit,
    onClickableTextClick: (String) -> Unit
) {
    FinancialConnectionsScaffold(
        topBar = {
            if (state.payload()?.showInModal == true) {
                FinancialConnectionsTopAppBar(
                    showBack = state.canNavigateBack,
                    onCloseClick = onCloseClick
                )
            }
        }
    ) {
        when (val payload = state.payload) {
            Uninitialized, is Loading -> SharedPartnerLoading()

            is Fail -> ErrorContent(
                error = payload.error,
                onSelectAnotherBank = onSelectAnotherBank,
                onEnterDetailsManually = onEnterDetailsManually,
                onCloseFromErrorClick = onCloseFromErrorClick
            )

            is Success -> LoadedContent(
                authenticationStatus = state.authenticationStatus,
                payload = payload(),
                onClickableTextClick = onClickableTextClick,
                onContinueClick = onContinueClick,
                onCancelClick = onCancelClick,
                onSelectAnotherBank = onSelectAnotherBank,
            )
        }
    }
}

@Composable
private fun ErrorContent(
    error: Throwable,
    onSelectAnotherBank: () -> Unit,
    onEnterDetailsManually: () -> Unit,
    onCloseFromErrorClick: (Throwable) -> Unit
) {
    when (error) {
        is InstitutionPlannedDowntimeError -> InstitutionPlannedDowntimeErrorContent(
            exception = error,
            onSelectAnotherBank = onSelectAnotherBank,
            onEnterDetailsManually = onEnterDetailsManually
        )

        is InstitutionUnplannedDowntimeError -> InstitutionUnplannedDowntimeErrorContent(
            exception = error,
            onSelectAnotherBank = onSelectAnotherBank,
            onEnterDetailsManually = onEnterDetailsManually
        )

        else -> UnclassifiedErrorContent(error, onCloseFromErrorClick)
    }
}

@Composable
private fun LoadedContent(
    authenticationStatus: Async<String>,
    payload: SharedPartnerAuthState.Payload,
    onContinueClick: () -> Unit,
    onCancelClick: () -> Unit,
    onSelectAnotherBank: () -> Unit,
    onClickableTextClick: (String) -> Unit
) {
    when (authenticationStatus) {
        is Uninitialized,
        is Loading,
        is Success -> when (payload.authSession.isOAuth) {
            true -> PrePaneContent(
                // show loading prepane when authenticationStatus
                // is Loading or Success (completing auth after redirect)
                loading = authenticationStatus is Loading || authenticationStatus is Success,
                showInDrawer = payload.showInModal,
                onContinueClick = onContinueClick,
                onCancelClick = onCancelClick,
                content = requireNotNull(payload.authSession.display?.text?.oauthPrepane),
                onClickableTextClick = onClickableTextClick,
            )

            false -> SharedPartnerLoading()
        }

        is Fail -> {
            // TODO@carlosmuvi translate error type to specific error screen.
            InstitutionUnknownErrorContent(onSelectAnotherBank)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
@Suppress("LongMethod")
private fun PrePaneContent(
    content: OauthPrepane,
    loading: Boolean,
    onContinueClick: () -> Unit,
    onCancelClick: () -> Unit,
    onClickableTextClick: (String) -> Unit,
    showInDrawer: Boolean
) {
    Layout(
        inModal = showInDrawer,
        // Overrides padding values to allow full-span prepane image background
        verticalArrangement = Arrangement.spacedBy(24.dp),
        bodyPadding = PaddingValues(horizontal = 0.dp, vertical = 16.dp),
        body = {
            item {
                PrepaneHeader(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    content = content
                )
            }
            itemsIndexed(content.body.entries) { index, bodyItem ->
                when (bodyItem) {
                    is Entry.Image -> PrepaneImage(bodyItem)

                    is Entry.Text -> AnnotatedText(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        text = TextResource.Text(fromHtml(bodyItem.content)),
                        onClickableTextClick = onClickableTextClick,
                        defaultStyle = v3Typography.bodyMedium
                    )
                }
            }
        },
        footer = {
            PrepaneFooter(
                onContinueClick = onContinueClick,
                onCancelClick = onCancelClick,
                loading = loading,
                oAuthPrepane = content
            )
        }
    )
}

@Composable
private fun PrepaneImage(bodyItem: Entry.Image) {
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
                            v3Colors.backgroundOffset,
                            v3Colors.border,
                        ),
                    )
                )
                .weight(1f)
                .fillMaxHeight()
        )

        // left separator
        Box(
            modifier = Modifier
                .background(color = v3Colors.backgroundOffset)
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
                .background(color = v3Colors.backgroundOffset)
                .width(8.dp)
                .fillMaxHeight()
        )
        // right gradient
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            v3Colors.border,
                            v3Colors.backgroundOffset,
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
    loading: Boolean,
    oAuthPrepane: OauthPrepane
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FinancialConnectionsButton(
            onClick = onContinueClick,
            type = Type.Primary,
            loading = loading,
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
            enabled = loading.not(),
            modifier = Modifier
                .semantics { testTagsAsResourceId = true }
                .testTag("cancel_cta")
                .fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.stripe_prepane_cancel_cta),
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
            defaultStyle = v3Typography.headingLarge.copy(
                color = v3Colors.textDefault
            ),
        )
        AnnotatedText(
            text = subtitle,
            onClickableTextClick = { },
            defaultStyle = v3Typography.bodyMedium.copy(
                color = v3Colors.textDefault
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
    val backgroundColor = v3Colors.backgroundOffset.toArgb()
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
    group = "Shared Partner Auth"
)
@Composable
internal fun PartnerAuthPreview(
    @PreviewParameter(PartnerAuthPreviewParameterProvider::class)
    state: SharedPartnerAuthState
) {
    FinancialConnectionsPreview {
        SharedPartnerAuthContent(
            state = state,
            bottomSheetState = rememberModalBottomSheetState(
                ModalBottomSheetValue.Hidden,
            ),
            onContinueClick = {},
            onSelectAnotherBank = {},
            onEnterDetailsManually = {},
            onClickableTextClick = {},
            onCloseClick = {},
            onCancelClick = {},
            onCloseFromErrorClick = {}
        )
    }
}

private const val PHONE_BACKGROUND_WIDTH_DP = 240
private const val PHONE_BACKGROUND_HEIGHT_DP = 200
private const val WEBVIEW_ALPHA = 0.99f
