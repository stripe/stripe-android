package com.stripe.android.financialconnections.features.consent

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.core.Result
import com.stripe.android.financialconnections.core.Result.Error
import com.stripe.android.financialconnections.core.Result.Loading
import com.stripe.android.financialconnections.core.Result.Success
import com.stripe.android.financialconnections.core.Result.Uninitialized
import com.stripe.android.financialconnections.core.rememberPaneViewModel
import com.stripe.android.financialconnections.features.common.DataAccessBottomSheetContent
import com.stripe.android.financialconnections.features.common.LegalDetailsBottomSheetContent
import com.stripe.android.financialconnections.features.common.ListItem
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.features.consent.ConsentState.ViewEffect.OpenBottomSheet
import com.stripe.android.financialconnections.features.consent.ConsentState.ViewEffect.OpenUrl
import com.stripe.android.financialconnections.features.consent.ui.ConsentLogoHeader
import com.stripe.android.financialconnections.model.ConsentPane
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.LocalReducedBranding
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsModalBottomSheetLayout
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.components.elevation
import com.stripe.android.financialconnections.ui.sdui.BulletUI
import com.stripe.android.financialconnections.ui.sdui.fromHtml
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.colors
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.typography
import com.stripe.android.financialconnections.ui.theme.LazyLayout
import kotlinx.coroutines.launch

@ExperimentalMaterialApi
@Composable
internal fun ConsentScreen() {
    val viewModel: ConsentViewModel = rememberPaneViewModel { ConsentViewModel.factory(it) }
    val parentViewModel = parentViewModel()
    val state = viewModel.stateFlow.collectAsState()

    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(
        ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )

    BackHandler(bottomSheetState.isVisible) {
        scope.launch { bottomSheetState.hide() }
    }

    state.value.viewEffect?.let { viewEffect ->
        LaunchedEffect(viewEffect) {
            when (viewEffect) {
                is OpenUrl -> uriHandler.openUri(viewEffect.url)
                is OpenBottomSheet -> bottomSheetState.show()
            }
            viewModel.onViewEffectLaunched()
        }
    }

    ConsentContent(
        state = state.value,
        bottomSheetState = bottomSheetState,
        onContinueClick = viewModel::onContinueClick,
        onClickableTextClick = viewModel::onClickableTextClick,
        onConfirmModalClick = { scope.launch { bottomSheetState.hide() } },
        onCloseClick = { parentViewModel.onCloseNoConfirmationClick(Pane.CONSENT) },
        onCloseFromErrorClick = parentViewModel::onCloseFromErrorClick
    )
}

@Composable
private fun ConsentContent(
    state: ConsentState,
    bottomSheetState: ModalBottomSheetState,
    onContinueClick: () -> Unit,
    onClickableTextClick: (String) -> Unit,
    onConfirmModalClick: () -> Unit,
    onCloseClick: () -> Unit,
    onCloseFromErrorClick: (Throwable) -> Unit
) {
    when (val result = state.consent) {
        Uninitialized, Loading -> {
            ConsentLoadingContent()
        }

        is Error -> {
            UnclassifiedErrorContent { onCloseFromErrorClick(result.throwable) }
        }

        is Success -> {
            LoadedContent(
                payload = result.value,
                bottomSheetState = bottomSheetState,
                acceptConsent = state.acceptConsent,
                bottomSheetMode = state.currentBottomSheet,
                onClickableTextClick = onClickableTextClick,
                onContinueClick = onContinueClick,
                onCloseClick = onCloseClick,
                onConfirmModalClick = onConfirmModalClick
            )
        }
    }
}

/**
 * Shows an empty screen without loading indicator to avoid flashing,
 * as loading should be super fast.
 */
@Composable
private fun ConsentLoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // empty content.
    }
}

@Composable
private fun ConsentMainContent(
    payload: ConsentState.Payload,
    acceptConsent: Result<FinancialConnectionsSessionManifest>,
    onClickableTextClick: (String) -> Unit,
    onContinueClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    val scrollState = rememberLazyListState()
    val title = remember(payload.consent.title) {
        TextResource.Text(fromHtml(payload.consent.title))
    }
    val bullets = remember(payload.consent.body.bullets) {
        payload.consent.body.bullets.map { bullet -> BulletUI.from(bullet) }
    }
    FinancialConnectionsScaffold(
        topBar = {
            FinancialConnectionsTopAppBar(
                hideStripeLogo = when {
                    payload.shouldShowMerchantLogos -> true
                    else -> LocalReducedBranding.current
                },
                onCloseClick = onCloseClick,
                elevation = scrollState.elevation
            )
        }
    ) {
        LazyLayout(
            lazyListState = scrollState,
            body = {
                consentBody(
                    payload = payload,
                    title = title,
                    onClickableTextClick = onClickableTextClick,
                    bullets = bullets
                )
            },
            footer = {
                ConsentFooter(
                    consent = payload.consent,
                    acceptConsent = acceptConsent,
                    onClickableTextClick = onClickableTextClick,
                    onContinueClick = onContinueClick
                )
            }
        )
    }
}

private fun LazyListScope.consentBody(
    payload: ConsentState.Payload,
    title: TextResource.Text,
    onClickableTextClick: (String) -> Unit,
    bullets: List<BulletUI>
) {
    item {
        Spacer(modifier = Modifier.size(8.dp))
        ConsentLogoHeader(
            modifier = Modifier.fillMaxWidth(),
            logos = payload.merchantLogos,
        )
        Spacer(modifier = Modifier.size(32.dp))
    }
    item {
        AnnotatedText(
            text = title,
            onClickableTextClick = { onClickableTextClick(it) },
            defaultStyle = typography.headingXLarge.copy(
                textAlign = TextAlign.Center
            )
        )
        Spacer(modifier = Modifier.size(32.dp))
    }
    items(bullets) { bullet ->
        ListItem(
            bullet = bullet,
            onClickableTextClick = onClickableTextClick
        )
        Spacer(modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun LoadedContent(
    payload: ConsentState.Payload,
    bottomSheetState: ModalBottomSheetState,
    acceptConsent: Result<FinancialConnectionsSessionManifest>,
    onContinueClick: () -> Unit,
    onCloseClick: () -> Unit,
    onClickableTextClick: (String) -> Unit,
    onConfirmModalClick: () -> Unit,
    bottomSheetMode: ConsentState.BottomSheetContent?,
) {
    FinancialConnectionsModalBottomSheetLayout(
        sheetState = bottomSheetState,
        sheetContent = {
            when (bottomSheetMode) {
                ConsentState.BottomSheetContent.LEGAL -> LegalDetailsBottomSheetContent(
                    legalDetails = payload.consent.legalDetailsNotice,
                    onConfirmModalClick = onConfirmModalClick,
                    onClickableTextClick = onClickableTextClick
                )

                ConsentState.BottomSheetContent.DATA -> DataAccessBottomSheetContent(
                    dataDialog = payload.consent.dataAccessNotice,
                    onConfirmModalClick = onConfirmModalClick,
                    onClickableTextClick = onClickableTextClick
                )

                null -> {}
            }
        },
        content = {
            ConsentMainContent(
                acceptConsent = acceptConsent,
                payload = payload,
                onClickableTextClick = onClickableTextClick,
                onContinueClick = onContinueClick,
                onCloseClick = onCloseClick
            )
        }
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ConsentFooter(
    acceptConsent: Result<FinancialConnectionsSessionManifest>,
    consent: ConsentPane,
    onClickableTextClick: (String) -> Unit,
    onContinueClick: () -> Unit,
) {
    val aboveCta = remember(consent.aboveCta) {
        TextResource.Text(fromHtml(consent.aboveCta))
    }
    val belowCta = remember(consent.belowCta) {
        consent.belowCta?.let { TextResource.Text(fromHtml(consent.belowCta)) }
    }
    Column {
        AnnotatedText(
            modifier = Modifier.fillMaxWidth(),
            text = aboveCta,
            onClickableTextClick = onClickableTextClick,
            defaultStyle = typography.labelSmall.copy(
                textAlign = TextAlign.Center,
                color = colors.textDefault
            )
        )
        Spacer(modifier = Modifier.size(16.dp))
        FinancialConnectionsButton(
            loading = acceptConsent is Loading,
            onClick = onContinueClick,
            modifier = Modifier
                .semantics { testTagsAsResourceId = true }
                .testTag("consent_cta")
                .fillMaxWidth()
        ) {
            Text(text = consent.cta)
        }
        if (belowCta != null) {
            Spacer(modifier = Modifier.size(16.dp))
            AnnotatedText(
                modifier = Modifier.fillMaxWidth(),
                text = belowCta,
                onClickableTextClick = onClickableTextClick,
                defaultStyle = typography.labelSmall.copy(
                    textAlign = TextAlign.Center,
                    color = colors.textDefault
                )
            )
        }
    }
}

@Preview(group = "Consent Pane")
@Composable
internal fun ContentPreview(
    @PreviewParameter(provider = ConsentPreviewParameterProvider::class)
    state: Pair<ModalBottomSheetValue, ConsentState>
) {
    FinancialConnectionsPreview {
        ConsentContent(
            state = state.second,
            bottomSheetState = rememberModalBottomSheetState(
                state.first,
                skipHalfExpanded = true
            ),
            onContinueClick = {},
            onClickableTextClick = {},
            onConfirmModalClick = {},
            onCloseClick = {},
            onCloseFromErrorClick = {}
        )
    }
}
