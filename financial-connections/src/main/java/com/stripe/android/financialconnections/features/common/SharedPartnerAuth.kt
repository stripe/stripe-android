package com.stripe.android.financialconnections.features.common

import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.webkit.WebView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ModalBottomSheetLayout
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
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
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.components.StringAnnotation
import com.stripe.android.financialconnections.ui.sdui.fromHtml
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.uicore.image.StripeImage
import kotlinx.coroutines.launch

@Composable
internal fun SharedPartnerAuth(
    state: SharedPartnerAuthState,
    onContinueClick: () -> Unit,
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
        bottomSheetState = bottomSheetState,
        state = state,
        onClickableTextClick = onClickableTextClick,
        onSelectAnotherBank = onSelectAnotherBank,
        onEnterDetailsManually = onEnterDetailsManually,
        onCloseClick = { viewModel.onCloseWithConfirmationClick(state.pane) },
        onContinueClick = onContinueClick,
        onCloseFromErrorClick = viewModel::onCloseFromErrorClick
    )
}

@Composable
private fun SharedPartnerAuthContent(
    bottomSheetState: ModalBottomSheetState,
    state: SharedPartnerAuthState,
    onClickableTextClick: (String) -> Unit,
    onSelectAnotherBank: () -> Unit,
    onEnterDetailsManually: () -> Unit,
    onContinueClick: () -> Unit,
    onCloseClick: () -> Unit,
    onCloseFromErrorClick: (Throwable) -> Unit
) {
    val scope = rememberCoroutineScope()
    ModalBottomSheetLayout(
        sheetState = bottomSheetState,
        sheetBackgroundColor = FinancialConnectionsTheme.colors.backgroundSurface,
        sheetShape = RoundedCornerShape(8.dp),
        scrimColor = FinancialConnectionsTheme.colors.textSecondary.copy(alpha = 0.5f),
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
                onContinueClick = onContinueClick,
            )
        }
    )
}

@Composable
private fun SharedPartnerAuthBody(
    state: SharedPartnerAuthState,
    onCloseClick: () -> Unit,
    onSelectAnotherBank: () -> Unit,
    onEnterDetailsManually: () -> Unit,
    onCloseFromErrorClick: (Throwable) -> Unit,
    onContinueClick: () -> Unit,
    onClickableTextClick: (String) -> Unit
) {
    FinancialConnectionsScaffold(
        topBar = {
            FinancialConnectionsTopAppBar(
                showBack = state.canNavigateBack,
                onCloseClick = onCloseClick
            )
        }
    ) {
        when (val payload = state.payload) {
            Uninitialized, is Loading -> LoadingContent(
                title = stringResource(id = R.string.stripe_partnerauth_loading_title),
                content = stringResource(id = R.string.stripe_partnerauth_loading_desc)
            )

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
    onSelectAnotherBank: () -> Unit,
    onClickableTextClick: (String) -> Unit
) {
    when (authenticationStatus) {
        is Uninitialized -> when (payload.authSession.isOAuth) {
            true -> InstitutionalPrePaneContent(
                onContinueClick = onContinueClick,
                content = requireNotNull(payload.authSession.display?.text?.oauthPrepane),
                onClickableTextClick = onClickableTextClick,
            )

            false -> LoadingContent(
                title = stringResource(id = R.string.stripe_partnerauth_loading_title),
                content = stringResource(id = R.string.stripe_partnerauth_loading_desc)
            )
        }

        is Loading -> FullScreenGenericLoading()

        is Success -> LoadingContent(
            title = stringResource(R.string.stripe_account_picker_loading_title),
            content = stringResource(R.string.stripe_account_picker_loading_desc)
        )

        is Fail -> {
            // TODO@carlosmuvi translate error type to specific error screen.
            InstitutionUnknownErrorContent(onSelectAnotherBank)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
@Suppress("LongMethod")
private fun InstitutionalPrePaneContent(
    onContinueClick: () -> Unit,
    content: OauthPrepane,
    onClickableTextClick: (String) -> Unit
) {
    val title = remember(content.title) {
        TextResource.Text(fromHtml(content.title))
    }
    val scrollState = rememberScrollState()
    Column(
        Modifier
            .fillMaxSize()
            .padding(
                top = 16.dp,
                start = 24.dp,
                end = 24.dp,
                bottom = 24.dp
            )
    ) {
        content.institutionIcon?.default?.let {
            val institutionIconModifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(6.dp))
            StripeImage(
                url = it,
                contentDescription = null,
                imageLoader = LocalImageLoader.current,
                errorContent = { InstitutionPlaceholder(institutionIconModifier) },
                modifier = institutionIconModifier
            )
            Spacer(modifier = Modifier.size(16.dp))
        }
        AnnotatedText(
            text = title,
            onClickableTextClick = { },
            defaultStyle = FinancialConnectionsTheme.typography.subtitle,
            annotationStyles = mapOf(
                StringAnnotation.BOLD to FinancialConnectionsTheme.typography.subtitleEmphasized.toSpanStyle()
            )
        )
        Column(
            modifier = Modifier
                .padding(top = 16.dp, bottom = 16.dp)
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            // CONTENT
            content.body.entries.forEachIndexed { index, bodyItem ->
                when (bodyItem) {
                    is Entry.Image -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    shape = RoundedCornerShape(8.dp),
                                    color = FinancialConnectionsTheme.colors.backgroundContainer
                                )

                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.stripe_prepane_phone_bg),
                                contentDescription = "Test",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .width(PHONE_BACKGROUND_HEIGHT_DP.dp)
                                    .height(PHONE_BACKGROUND_WIDTH_DP.dp)
                            )
                            GifWebView(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .width(PHONE_BACKGROUND_HEIGHT_DP.dp)
                                    .height(PHONE_BACKGROUND_WIDTH_DP.dp)
                                    .padding(horizontal = 16.dp),
                                bodyItem.content.default!!
                            )
                        }
                    }

                    is Entry.Text -> AnnotatedText(
                        text = TextResource.Text(fromHtml(bodyItem.content)),
                        onClickableTextClick = onClickableTextClick,
                        defaultStyle = FinancialConnectionsTheme.typography.body,
                        annotationStyles = mapOf(
                            StringAnnotation.BOLD to FinancialConnectionsTheme.typography.bodyEmphasized.toSpanStyle(),
                            StringAnnotation.CLICKABLE to FinancialConnectionsTheme.typography.bodyEmphasized
                                .toSpanStyle()
                                .copy(color = FinancialConnectionsTheme.colors.textBrand)
                        )
                    )
                }
                if (index != content.body.entries.lastIndex) {
                    Spacer(modifier = Modifier.size(16.dp))
                }
            }
            Box(modifier = Modifier.weight(1f))
            content.partnerNotice?.let {
                Spacer(modifier = Modifier.size(16.dp))
                PartnerCallout(
                    partnerNotice = content.partnerNotice,
                    onClickableTextClick = onClickableTextClick
                )
            }
        }
        FinancialConnectionsButton(
            onClick = onContinueClick,
            modifier = Modifier
                .semantics { testTagsAsResourceId = true }
                .testTag("prepane_cta")
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = content.cta.text,
                    textAlign = TextAlign.Center
                )
                content.cta.icon?.default?.let {
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
    }
}

@Composable
private fun GifWebView(
    modifier: Modifier,
    gifUrl: String
) {
    val body = "<html><body><img style=\"width: 100%\" src=\"$gifUrl\"></body></html>"
    AndroidView(
        modifier = modifier,
        factory = {
            WebView(it).apply {
                /**
                 * WebView crashes when leaving the composition. Adding alpha acts as a workaround.
                 * https://stackoverflow.com/questions/74829526/
                 */
                alpha = WEBVIEW_ALPHA
                layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                isVerticalScrollBarEnabled = false
                isVerticalFadingEdgeEnabled = false
                loadData(body, null, "UTF-8")
            }
        },
        update = {
            it.loadData(body, null, "UTF-8")
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
            onCloseFromErrorClick = {}
        )
    }
}

private const val PHONE_BACKGROUND_WIDTH_DP = 272
private const val PHONE_BACKGROUND_HEIGHT_DP = 264
private const val WEBVIEW_ALPHA = 0.99f
