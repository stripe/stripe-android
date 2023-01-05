@file:OptIn(ExperimentalMaterialApi::class, ExperimentalMaterialApi::class)
@file:Suppress("LongMethod", "TooManyFunctions")

package com.stripe.android.financialconnections.features.consent

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.features.common.DataAccessBottomSheetContent
import com.stripe.android.financialconnections.features.common.LegalDetailsBottomSheetContent
import com.stripe.android.financialconnections.features.common.LoadingContent
import com.stripe.android.financialconnections.features.common.ModalBottomSheetBullet
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.features.consent.ConsentState.ViewEffect.OpenBottomSheet
import com.stripe.android.financialconnections.features.consent.ConsentState.ViewEffect.OpenUrl
import com.stripe.android.financialconnections.model.ConsentPane
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.components.StringAnnotation
import com.stripe.android.financialconnections.ui.components.elevation
import com.stripe.android.financialconnections.ui.sdui.BulletUI
import com.stripe.android.financialconnections.ui.sdui.fromHtml
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.colors
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.typography
import kotlinx.coroutines.launch

@ExperimentalMaterialApi
@Composable
internal fun ConsentScreen() {
    // update step state when manifest changes
    val viewModel: ConsentViewModel = mavericksViewModel()
    val parentViewModel = parentViewModel()
    val state = viewModel.collectAsState()

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
    ) { parentViewModel.onCloseNoConfirmationClick(Pane.CONSENT) }
}

@Composable
private fun ConsentContent(
    state: ConsentState,
    bottomSheetState: ModalBottomSheetState,
    onContinueClick: () -> Unit,
    onClickableTextClick: (String) -> Unit,
    onConfirmModalClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    when (val consent = state.consent) {
        Uninitialized, is Loading -> LoadingContent()
        is Success -> LoadedContent(
            consent = consent(),
            bottomSheetMode = state.currentBottomSheet,
            acceptConsent = state.acceptConsent,
            bottomSheetState = bottomSheetState,
            onClickableTextClick = onClickableTextClick,
            onCloseClick = onCloseClick,
            onConfirmModalClick = onConfirmModalClick,
            onContinueClick = onContinueClick
        )

        is Fail -> UnclassifiedErrorContent(error = consent.error, onCloseFromErrorClick = {})
    }
}

@Composable
private fun ConsentMainContent(
    consent: ConsentPane,
    acceptConsent: Async<Unit>,
    onClickableTextClick: (String) -> Unit,
    onContinueClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    val title = remember(consent.title) {
        TextResource.Text(fromHtml(consent.title))
    }
    val bullets = remember(consent.body.bullets) {
        consent.body.bullets.map { bullet -> BulletUI.from(bullet) }
    }
    FinancialConnectionsScaffold(
        topBar = {
            FinancialConnectionsTopAppBar(
                onCloseClick = onCloseClick,
                elevation = scrollState.elevation
            )
        }
    ) {
        Column(
            Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(
                        top = 16.dp,
                        start = 24.dp,
                        end = 24.dp,
                        bottom = 24.dp
                    )
            ) {
                AnnotatedText(
                    text = title,
                    onClickableTextClick = { onClickableTextClick(it) },
                    defaultStyle = typography.subtitle,
                    annotationStyles = mapOf(
                        StringAnnotation.CLICKABLE to typography.subtitle
                            .toSpanStyle()
                            .copy(color = colors.textBrand),
                    )
                )
                Spacer(modifier = Modifier.size(24.dp))
                bullets.forEach { bullet ->
                    Spacer(modifier = Modifier.size(16.dp))
                    ModalBottomSheetBullet(
                        bullet,
                        onClickableTextClick = onClickableTextClick
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }
            ConsentFooter(
                consent = consent,
                acceptConsent = acceptConsent,
                onClickableTextClick = onClickableTextClick,
                onContinueClick = onContinueClick
            )
        }
    }
}

@Composable
private fun LoadedContent(
    consent: ConsentPane,
    bottomSheetState: ModalBottomSheetState,
    acceptConsent: Async<Unit>,
    onContinueClick: () -> Unit,
    onCloseClick: () -> Unit,
    onClickableTextClick: (String) -> Unit,
    onConfirmModalClick: () -> Unit,
    bottomSheetMode: ConsentState.BottomSheetContent?,
) {
    ModalBottomSheetLayout(
        sheetState = bottomSheetState,
        sheetBackgroundColor = colors.backgroundSurface,
        sheetShape = RoundedCornerShape(8.dp),
        scrimColor = colors.textSecondary.copy(alpha = 0.5f),
        sheetContent = {
            when (bottomSheetMode) {
                ConsentState.BottomSheetContent.LEGAL -> LegalDetailsBottomSheetContent(
                    legalDetails = consent.legalDetailsNotice,
                    onConfirmModalClick = onConfirmModalClick,
                    onClickableTextClick = onClickableTextClick
                )

                ConsentState.BottomSheetContent.DATA -> DataAccessBottomSheetContent(
                    dataDialog = consent.dataAccessNotice,
                    onConfirmModalClick = onConfirmModalClick,
                    onClickableTextClick = onClickableTextClick
                )

                null -> {}
            }
        },
        content = {
            ConsentMainContent(
                acceptConsent = acceptConsent,
                consent = consent,
                onClickableTextClick = onClickableTextClick,
                onContinueClick = onContinueClick,
                onCloseClick = onCloseClick
            )
        }
    )
}

@Composable
private fun ConsentFooter(
    acceptConsent: Async<Unit>,
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
    Column(
        modifier = Modifier.padding(
            start = 24.dp,
            end = 24.dp,
            top = 16.dp,
            bottom = 24.dp
        )
    ) {
        AnnotatedText(
            text = aboveCta,
            onClickableTextClick = onClickableTextClick,
            defaultStyle = typography.detail.copy(
                textAlign = TextAlign.Center,
                color = colors.textSecondary
            ),
            annotationStyles = mapOf(
                StringAnnotation.CLICKABLE to typography.detailEmphasized
                    .toSpanStyle()
                    .copy(color = colors.textBrand),
                StringAnnotation.BOLD to typography.detailEmphasized
                    .toSpanStyle()
                    .copy(color = colors.textSecondary)
            )
        )
        Spacer(modifier = Modifier.size(16.dp))
        FinancialConnectionsButton(
            loading = acceptConsent is Loading,
            onClick = onContinueClick,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(text = consent.cta)
        }
        if (belowCta != null) {
            Spacer(modifier = Modifier.size(24.dp))
            AnnotatedText(
                modifier = Modifier.fillMaxWidth(),
                text = belowCta,
                onClickableTextClick = onClickableTextClick,
                defaultStyle = typography.detail.copy(
                    textAlign = TextAlign.Center,
                    color = colors.textSecondary
                ),
                annotationStyles = mapOf(
                    StringAnnotation.CLICKABLE to typography.detailEmphasized
                        .toSpanStyle()
                        .copy(color = colors.textBrand),
                    StringAnnotation.BOLD to typography.detailEmphasized
                        .toSpanStyle()
                        .copy(color = colors.textSecondary)
                )
            )
            Spacer(modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
@Preview(group = "Consent Pane", name = "canonical")
internal fun ContentPreview(
    state: ConsentState = ConsentStates.canonical()
) {
    FinancialConnectionsPreview {
        ConsentContent(
            state = state,
            bottomSheetState = rememberModalBottomSheetState(
                ModalBottomSheetValue.Hidden,
                skipHalfExpanded = true
            ),
            onContinueClick = {},
            onClickableTextClick = {},
            onConfirmModalClick = {},
        ) {}
    }
}

@Composable
@Preview(group = "Consent Pane", name = "requested data")
// TODO@carlosmuvi add proper preview with expanded bottom sheet once related Compose bug gets fixed.
// https://issuetracker.google.com/issues/241895902
internal fun ContentRequestedDataPreview() {
    FinancialConnectionsPreview {
        Box(
            Modifier.background(colors.backgroundSurface)
        ) {
            DataAccessBottomSheetContent(
                dataDialog = ConsentStates.sampleConsent().dataAccessNotice,
                onClickableTextClick = {},
                onConfirmModalClick = {},
            )
        }
    }
}

@Composable
@Preview(group = "Consent Pane", name = "requested data")
// TODO@carlosmuvi add proper preview with expanded bottom sheet once related Compose bug gets fixed.
// https://issuetracker.google.com/issues/241895902
internal fun ContentLegalDetailsPreview() {
    FinancialConnectionsPreview {
        Box(
            Modifier.background(colors.backgroundSurface)
        ) {
            LegalDetailsBottomSheetContent(
                legalDetails = ConsentStates.sampleConsent().legalDetailsNotice,
                onClickableTextClick = {},
                onConfirmModalClick = {},
            )
        }
    }
}

@Composable
@Preview(group = "Consent Pane", name = "manual entry + microdeposits")
internal fun ContentManualEntryPlusMicrodeposits(
    state: ConsentState = ConsentStates.manualEntryPlusMicrodeposits()
) {
    FinancialConnectionsPreview {
        ConsentContent(
            state = state,
            bottomSheetState = rememberModalBottomSheetState(
                ModalBottomSheetValue.Hidden,
                skipHalfExpanded = true
            ),
            onContinueClick = {},
            onClickableTextClick = {},
            onConfirmModalClick = {},
        ) {}
    }
}
