package com.stripe.android.financialconnections.features.consent

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
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import com.stripe.android.financialconnections.features.common.ListItem
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.features.consent.ConsentPreviewParameterProvider.ConsentPreviewState
import com.stripe.android.financialconnections.features.consent.ConsentState.ViewEffect.OpenUrl
import com.stripe.android.financialconnections.features.consent.ui.ConsentLogoHeader
import com.stripe.android.financialconnections.model.ConsentPane
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.presentation.Async
import com.stripe.android.financialconnections.presentation.Async.Fail
import com.stripe.android.financialconnections.presentation.Async.Loading
import com.stripe.android.financialconnections.presentation.Async.Success
import com.stripe.android.financialconnections.presentation.Async.Uninitialized
import com.stripe.android.financialconnections.presentation.paneViewModel
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.sdui.BulletUI
import com.stripe.android.financialconnections.ui.sdui.fromHtml
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.colors
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.typography
import com.stripe.android.financialconnections.ui.theme.LazyLayout
import com.stripe.android.uicore.utils.collectAsState

@ExperimentalMaterialApi
@Composable
internal fun ConsentScreen() {
    val viewModel: ConsentViewModel = paneViewModel { ConsentViewModel.factory(it) }
    val parentViewModel = parentViewModel()
    val state = viewModel.stateFlow.collectAsState()

    val uriHandler = LocalUriHandler.current

    state.value.viewEffect?.let { viewEffect ->
        LaunchedEffect(viewEffect) {
            when (viewEffect) {
                is OpenUrl -> uriHandler.openUri(viewEffect.url)
            }
            viewModel.onViewEffectLaunched()
        }
    }

    ConsentContent(
        state = state.value,
        onContinueClick = viewModel::onContinueClick,
        onClickableTextClick = viewModel::onClickableTextClick,
        onCloseFromErrorClick = parentViewModel::onCloseFromErrorClick,
    )
}

@Composable
private fun ConsentContent(
    state: ConsentState,
    onContinueClick: () -> Unit,
    onClickableTextClick: (String) -> Unit,
    onCloseFromErrorClick: (Throwable) -> Unit
) {
    when (val result = state.consent) {
        Uninitialized,
        is Loading -> ConsentLoadingContent()

        is Success -> LoadedContent(
            payload = result(),
            acceptConsent = state.acceptConsent,
            onClickableTextClick = onClickableTextClick,
            onContinueClick = onContinueClick,
        )

        is Fail -> UnclassifiedErrorContent { onCloseFromErrorClick(result.error) }
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
private fun LoadedContent(
    payload: ConsentState.Payload,
    acceptConsent: Async<FinancialConnectionsSessionManifest>,
    onClickableTextClick: (String) -> Unit,
    onContinueClick: () -> Unit,
) {
    val scrollState = rememberLazyListState()
    val title = remember(payload.consent.title) {
        TextResource.Text(fromHtml(payload.consent.title))
    }
    val bullets = remember(payload.consent.body.bullets) {
        payload.consent.body.bullets.map { bullet -> BulletUI.from(bullet) }
    }
    Box {
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
            showDots = payload.showAnimatedDots,
        )
        Spacer(modifier = Modifier.size(32.dp))
    }
    item {
        AnnotatedText(
            text = title,
            onClickableTextClick = { onClickableTextClick(it) },
            defaultStyle = typography.headingXLarge.copy(
                textAlign = TextAlign.Center,
                color = colors.textDefault,
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ConsentFooter(
    acceptConsent: Async<FinancialConnectionsSessionManifest>,
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
    previewState: ConsentPreviewState,
) {
    FinancialConnectionsPreview(theme = previewState.theme) {
        ConsentContent(
            state = previewState.state,
            onContinueClick = {},
            onClickableTextClick = {},
            onCloseFromErrorClick = {}
        )
    }
}
