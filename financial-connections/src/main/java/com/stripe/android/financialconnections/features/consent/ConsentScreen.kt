@file:OptIn(ExperimentalMaterialApi::class, ExperimentalMaterialApi::class)

package com.stripe.android.financialconnections.features.consent

import android.net.Uri
import android.os.Build
import android.text.Html
import android.text.Spanned
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.features.common.LoadingContent
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.features.consent.ConsentState.ViewEffect
import com.stripe.android.financialconnections.model.ConsentScreen
import com.stripe.android.financialconnections.model.DataDialog
import com.stripe.android.financialconnections.presentation.CreateBrowserIntentForUrl
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.components.StringAnnotation
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.uicore.image.StripeImage
import com.stripe.android.uicore.image.StripeImageLoader
import kotlinx.coroutines.launch

@ExperimentalMaterialApi
@Composable
internal fun ConsentScreen() {
    // update step state when manifest changes
    val viewModel: ConsentViewModel = mavericksViewModel()
    val parentViewModel = parentViewModel()
    val state = viewModel.collectAsState()

    // create bottom sheet state.
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(
        ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )

    BackHandler(bottomSheetState.isVisible) {
        scope.launch { bottomSheetState.hide() }
    }

    ViewEffect(
        viewModel = viewModel,
        bottomSheetState = bottomSheetState,
        viewEffect = state.value.viewEffect
    )

    ConsentContent(
        state = state.value,
        bottomSheetState = bottomSheetState,
        onContinueClick = viewModel::onContinueClick,
        onClickableTextClick = viewModel::onClickableTextClick,
        onConfirmModalClick = { scope.launch { bottomSheetState.hide() } },
        onCloseClick = parentViewModel::onCloseNoConfirmationClick
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun ViewEffect(
    viewModel: ConsentViewModel,
    viewEffect: ViewEffect?,
    bottomSheetState: ModalBottomSheetState
) {
    val context = LocalContext.current
    LaunchedEffect(viewEffect) {
        when (viewEffect) {
            is ViewEffect.OpenUrl -> context.startActivity(
                CreateBrowserIntentForUrl(
                    context = context,
                    uri = Uri.parse(viewEffect.url)
                )
            )

            is ViewEffect.OpenBottomSheet -> bottomSheetState.show()
            null -> Unit
        }
        viewModel.onViewEffectLaunched()
    }
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
    consent: ConsentScreen,
    acceptConsent: Async<Unit>,
    onClickableTextClick: (String) -> Unit,
    onContinueClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    val title = remember(consent.title) {
        TextResource.Text(fromHtml(consent.title))
    }
    val bullets = remember(consent.body) {
        consent.body.map { it.iconUrl to TextResource.Text(fromHtml(it.text)) }
    }
    FinancialConnectionsScaffold(
        topBar = { FinancialConnectionsTopAppBar(onCloseClick = onCloseClick) }
    ) {
        Column(
            Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(24.dp)
            ) {
                AnnotatedText(
                    text = title,
                    onClickableTextClick = { onClickableTextClick(it) },
                    defaultStyle = FinancialConnectionsTheme.typography.subtitle,
                    annotationStyles = mapOf(
                        StringAnnotation.BOLD to FinancialConnectionsTheme.typography.heading
                            .toSpanStyle()
                            .copy(color = FinancialConnectionsTheme.colors.textSecondary),
                        StringAnnotation.CLICKABLE to FinancialConnectionsTheme.typography.captionEmphasized
                            .toSpanStyle()
                            .copy(color = FinancialConnectionsTheme.colors.textBrand),
                    )
                )
                Spacer(modifier = Modifier.size(24.dp))
                bullets.forEach { (iconUrl, text) ->
                    ConsentBullet(iconUrl, text) { onClickableTextClick(it) }
                    Spacer(modifier = Modifier.size(16.dp))
                }

                Spacer(modifier = Modifier.weight(1f))
            }
            ConsentFooter(
                footerText = consent.footerText,
                ctaText = consent.buttonTitle,
                manualEntryEnabled = true, // TODO fix
                manualEntryShowBusinessDaysNotice = true, // TODO state.manualEntryShowBusinessDaysNotice
                acceptConsent = acceptConsent,
                onClickableTextClick = onClickableTextClick,
                onContinueClick = onContinueClick
            )
        }
    }
}

@Composable
private fun LoadedContent(
    consent: ConsentScreen,
    bottomSheetState: ModalBottomSheetState,
    acceptConsent: Async<Unit>,
    onContinueClick: () -> Unit,
    onCloseClick: () -> Unit,
    onClickableTextClick: (String) -> Unit,
    onConfirmModalClick: () -> Unit,
) {
    ModalBottomSheetLayout(
        sheetState = bottomSheetState,
        sheetBackgroundColor = FinancialConnectionsTheme.colors.backgroundSurface,
        sheetShape = RoundedCornerShape(8.dp),
        scrimColor = FinancialConnectionsTheme.colors.textSecondary.copy(alpha = 0.5f),
        sheetContent = {
            ConsentPermissionsBottomSheetContent(
                dataDialog = consent.dataDialog,
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
    manualEntryEnabled: Boolean,
    onClickableTextClick: (String) -> Unit,
    onContinueClick: () -> Unit,
    manualEntryShowBusinessDaysNotice: Boolean,
    footerText: String,
    ctaText: String
) {
    val formattedFooterText = remember(footerText) {
        TextResource.Text(fromHtml(footerText))
    }
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
        AnnotatedText(
            text = formattedFooterText,
            onClickableTextClick = onClickableTextClick,
            defaultStyle = FinancialConnectionsTheme.typography.body.copy(
                textAlign = TextAlign.Center,
                color = FinancialConnectionsTheme.colors.textSecondary
            )
        )
        Spacer(modifier = Modifier.size(16.dp))
        FinancialConnectionsButton(
            loading = acceptConsent is Loading,
            onClick = onContinueClick,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(text = ctaText)
        }
        if (manualEntryEnabled) {
            Spacer(modifier = Modifier.size(24.dp))
            AnnotatedText(
                modifier = Modifier.fillMaxWidth(),
                text = when (manualEntryShowBusinessDaysNotice) {
                    true -> TextResource.StringId(R.string.consent_pane_manual_entry_microdeposits)
                    false -> TextResource.StringId(R.string.consent_pane_manual_entry)
                },
                onClickableTextClick = onClickableTextClick,
                defaultStyle = FinancialConnectionsTheme.typography.body.copy(
                    textAlign = TextAlign.Center,
                    color = FinancialConnectionsTheme.colors.textSecondary
                )
            )
            Spacer(modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun ConsentPermissionsBottomSheetContent(
    dataDialog: DataDialog,
    onClickableTextClick: (String) -> Unit,
    onConfirmModalClick: () -> Unit
) {
    val title = remember(dataDialog.title) {
        TextResource.Text(fromHtml(dataDialog.title))
    }
    val footerText = remember(dataDialog.footerText) {
        TextResource.Text(fromHtml(dataDialog.footerText))
    }
    val bullets = remember(dataDialog.body) {
        dataDialog.body.map { body ->
            Triple(
                first = body.iconUrl,
                second = TextResource.Text(fromHtml(body.text)),
                third = body.subtext?.let { TextResource.Text(fromHtml(it)) }
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
                defaultStyle = FinancialConnectionsTheme.typography.heading.copy(
                    color = FinancialConnectionsTheme.colors.textPrimary
                ),
                onClickableTextClick = onClickableTextClick
            )
            bullets.forEach { (iconUrl, title, description) ->
                Spacer(modifier = Modifier.size(24.dp))
                ConsentBottomSheetBullet(
                    iconUrl = iconUrl,
                    title = title,
                    description = description
                )
            }
        }
        Column(
            Modifier.padding(
                bottom = 16.dp,
                start = 24.dp,
                end = 24.dp
            )
        ) {
            AnnotatedText(
                text = footerText,
                onClickableTextClick = onClickableTextClick,
                defaultStyle = FinancialConnectionsTheme.typography.body.copy(
                    color = FinancialConnectionsTheme.colors.textSecondary
                ),
                annotationStyles = mapOf(
                    StringAnnotation.CLICKABLE to FinancialConnectionsTheme.typography.caption
                        .toSpanStyle()
                        .copy(color = FinancialConnectionsTheme.colors.textBrand)
                )
            )
            Spacer(modifier = Modifier.size(16.dp))
            FinancialConnectionsButton(
                onClick = { onConfirmModalClick() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = dataDialog.buttonTitle)
            }
        }
    }
}

@Composable
private fun ConsentBottomSheetBullet(
    title: TextResource,
    description: TextResource?,
    iconUrl: String
) {
    Row {
        StripeImage(
            url = iconUrl,
            placeholder = painterResource(id = R.drawable.stripe_ic_brandicon_institution),
            imageLoader = StripeImageLoader(LocalContext.current),
            contentDescription = null,
            modifier = Modifier
                .size(16.dp)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Column {
            Text(
                text = title.toText().toString(),
                style = FinancialConnectionsTheme.typography.bodyEmphasized.copy(
                    color = FinancialConnectionsTheme.colors.textPrimary
                )
            )
            description?.let {
                Text(
                    text = description.toText().toString(),
                    style = FinancialConnectionsTheme.typography.caption.copy(
                        color = FinancialConnectionsTheme.colors.textSecondary
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
        StripeImage(
            url = iconUrl,
            imageLoader = StripeImageLoader(LocalContext.current), // TODO replace by local composition
            contentDescription = null,
            placeholder = painterResource(id = R.drawable.stripe_ic_brandicon_institution),
            modifier = Modifier
                .size(16.dp)
                .offset(y = 4.dp)
        )
        Spacer(modifier = Modifier.size(8.dp))
        AnnotatedText(
            text,
            onClickableTextClick = { onClickableTextClick?.invoke(it) },
            defaultStyle = FinancialConnectionsTheme.typography.body.copy(
                color = FinancialConnectionsTheme.colors.textSecondary
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
    FinancialConnectionsTheme {
        ConsentContent(
            state = state,
            bottomSheetState = rememberModalBottomSheetState(
                ModalBottomSheetValue.Hidden,
                skipHalfExpanded = true
            ),
            onContinueClick = {},
            onClickableTextClick = {},
            onConfirmModalClick = {},
            onCloseClick = {}
        )
    }
}

@Composable
@Preview(group = "Consent Pane", name = "manual entry + microdeposits")
internal fun ContentManualEntryPlusMicrodeposits(
    state: ConsentState = ConsentStates.manualEntryPlusMicrodeposits()
) {
    FinancialConnectionsTheme {
        ConsentContent(
            state = state,
            bottomSheetState = rememberModalBottomSheetState(
                ModalBottomSheetValue.Hidden,
                skipHalfExpanded = true
            ),
            onContinueClick = {},
            onClickableTextClick = {},
            onConfirmModalClick = {},
            onCloseClick = {}
        )
    }
}
