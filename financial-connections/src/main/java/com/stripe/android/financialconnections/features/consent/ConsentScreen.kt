@file:OptIn(ExperimentalMaterialApi::class, ExperimentalMaterialApi::class)
@file:Suppress("LongMethod", "TooManyFunctions")

package com.stripe.android.financialconnections.features.consent

import android.net.Uri
import android.os.Build
import android.text.Html
import android.text.Spanned
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
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
import com.stripe.android.financialconnections.features.common.InstitutionPlaceholder
import com.stripe.android.financialconnections.features.common.LoadingContent
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.features.consent.ConsentState.ViewEffect
import com.stripe.android.financialconnections.model.ConsentPane
import com.stripe.android.financialconnections.model.DataAccessNotice
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.NextPane
import com.stripe.android.financialconnections.presentation.CreateBrowserIntentForUrl
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.LocalImageLoader
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.components.StringAnnotation
import com.stripe.android.financialconnections.ui.components.elevation
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.colors
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.typography
import com.stripe.android.uicore.image.StripeImage
import kotlinx.coroutines.launch

@ExperimentalMaterialApi
@Composable
internal fun ConsentScreen() {
    // update step state when manifest changes
    val viewModel: ConsentViewModel = mavericksViewModel()
    val parentViewModel = parentViewModel()
    val state = viewModel.collectAsState()

    // create bottom sheet state.
    val context = LocalContext.current
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
                is ViewEffect.OpenBottomSheet -> bottomSheetState.show()
                is ViewEffect.OpenUrl -> context.startActivity(
                    CreateBrowserIntentForUrl(
                        context = context,
                        uri = Uri.parse(viewEffect.url)
                    )
                )
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
    ) { parentViewModel.onCloseNoConfirmationClick(NextPane.CONSENT) }
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
        consent.body.bullets.map { it.icon to TextResource.Text(fromHtml(it.content)) }
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
                bullets.forEach { (iconUrl, text) ->
                    ConsentBullet(iconUrl.default, text) { onClickableTextClick(it) }
                    Spacer(modifier = Modifier.size(16.dp))
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
) {
    ModalBottomSheetLayout(
        sheetState = bottomSheetState,
        sheetBackgroundColor = colors.backgroundSurface,
        sheetShape = RoundedCornerShape(8.dp),
        scrimColor = colors.textSecondary.copy(alpha = 0.5f),
        sheetContent = {
            ConsentPermissionsBottomSheetContent(
                dataDialog = consent.dataAccessNotice,
                onConfirmModalClick = onConfirmModalClick,
                onClickableTextClick = onClickableTextClick
            )
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
private fun ConsentPermissionsBottomSheetContent(
    dataDialog: DataAccessNotice,
    onClickableTextClick: (String) -> Unit,
    onConfirmModalClick: () -> Unit
) {
    val title = remember(dataDialog.title) {
        TextResource.Text(fromHtml(dataDialog.title))
    }
    val learnMore = remember(dataDialog.learnMore) {
        TextResource.Text(fromHtml(dataDialog.learnMore))
    }
    val connectedAccountNotice = remember(dataDialog.connectedAccountNotice) {
        dataDialog.connectedAccountNotice?.let { TextResource.Text(fromHtml(it)) }
    }
    val bullets = remember(dataDialog.body.bullets) {
        dataDialog.body.bullets.map { body ->
            Triple(
                first = body.icon,
                second = body.title?.let { TextResource.Text(fromHtml(body.title)) },
                third = TextResource.Text(fromHtml(body.content))
            )
        }
    }
    val scrollState = rememberScrollState()
    Column {
        Column(
            Modifier
                .verticalScroll(scrollState)
                .padding(24.dp)
        ) {
            AnnotatedText(
                text = title,
                defaultStyle = typography.heading.copy(
                    color = colors.textPrimary
                ),
                annotationStyles = emptyMap(),
                onClickableTextClick = onClickableTextClick
            )
            bullets.forEach { (iconUrl, title, description) ->
                Spacer(modifier = Modifier.size(24.dp))
                ConsentBottomSheetBullet(
                    iconUrl = iconUrl.default,
                    title = title,
                    description = description
                )
            }
        }
        Column(
            Modifier.padding(
                bottom = 24.dp,
                start = 24.dp,
                end = 24.dp
            )
        ) {
            if (connectedAccountNotice != null) {
                AnnotatedText(
                    text = connectedAccountNotice,
                    onClickableTextClick = onClickableTextClick,
                    defaultStyle = typography.caption.copy(
                        color = colors.textSecondary
                    ),
                    annotationStyles = mapOf(
                        StringAnnotation.CLICKABLE to typography.captionEmphasized
                            .toSpanStyle()
                            .copy(color = colors.textBrand),
                        StringAnnotation.BOLD to typography.captionEmphasized
                            .toSpanStyle()
                            .copy(color = colors.textSecondary),
                    )
                )
                Spacer(modifier = Modifier.size(12.dp))
            }
            AnnotatedText(
                text = learnMore,
                onClickableTextClick = onClickableTextClick,
                defaultStyle = typography.caption.copy(
                    color = colors.textSecondary
                ),
                annotationStyles = mapOf(
                    StringAnnotation.CLICKABLE to typography.captionEmphasized
                        .toSpanStyle()
                        .copy(color = colors.textBrand),
                    StringAnnotation.BOLD to typography.captionEmphasized
                        .toSpanStyle()
                        .copy(color = colors.textSecondary),
                )
            )
            Spacer(modifier = Modifier.size(16.dp))
            FinancialConnectionsButton(
                onClick = { onConfirmModalClick() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = dataDialog.cta)
            }
        }
    }
}

@Composable
private fun ConsentBottomSheetBullet(
    title: TextResource?,
    description: TextResource?,
    iconUrl: String
) {
    Row {
        val modifier = Modifier
            .size(16.dp)
            .offset(y = 2.dp)
        StripeImage(
            url = iconUrl,
            colorFilter = ColorFilter.tint(colors.textSuccess),
            errorContent = { InstitutionPlaceholder(modifier) },
            imageLoader = LocalImageLoader.current,
            contentDescription = null,
            modifier = modifier
        )
        Spacer(modifier = Modifier.size(8.dp))
        Column {
            title?.let {
                Text(
                    text = it.toText().toString(),
                    style = typography.bodyEmphasized.copy(
                        color = colors.textPrimary
                    )
                )
            }
            description?.let {
                Text(
                    text = description.toText().toString(),
                    style = typography.caption.copy(
                        color = colors.textSecondary
                    )
                )
            }
        }
    }
}

@Composable
private fun ConsentBullet(
    iconUrl: String,
    text: TextResource,
    onClickableTextClick: ((String) -> Unit)? = null
) {
    Row {
        val modifier = Modifier
            .size(16.dp)
            .offset(y = 2.dp)
        StripeImage(
            url = iconUrl,
            colorFilter = ColorFilter.tint(colors.textSecondary),
            imageLoader = LocalImageLoader.current,
            contentDescription = null,
            errorContent = { InstitutionPlaceholder(modifier) },
            modifier = modifier
        )
        Spacer(modifier = Modifier.size(8.dp))
        AnnotatedText(
            text,
            onClickableTextClick = { onClickableTextClick?.invoke(it) },
            defaultStyle = typography.body.copy(
                color = colors.textSecondary
            ),
            annotationStyles = mapOf(
                StringAnnotation.CLICKABLE to typography.bodyEmphasized
                    .toSpanStyle()
                    .copy(color = colors.textBrand),
                StringAnnotation.BOLD to typography.bodyEmphasized
                    .toSpanStyle()
                    .copy(color = colors.textSecondary)
            )
        )
    }
}

@SuppressWarnings("deprecation")
private fun fromHtml(source: String): Spanned {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY)
    } else {
        Html.fromHtml(source)
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
            ConsentPermissionsBottomSheetContent(
                dataDialog = ConsentStates.sampleConsent().dataAccessNotice,
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
