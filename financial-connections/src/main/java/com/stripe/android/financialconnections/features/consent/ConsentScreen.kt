@file:OptIn(ExperimentalMaterialApi::class, ExperimentalMaterialApi::class)
@file:Suppress("LongMethod", "TooManyFunctions")

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
import com.stripe.android.financialconnections.presentation.CreateBrowserIntentForUrl
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.LocalImageLoader
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.components.StringAnnotation
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
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
                        StringAnnotation.CLICKABLE to FinancialConnectionsTheme.typography.subtitle
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
        sheetBackgroundColor = FinancialConnectionsTheme.colors.backgroundSurface,
        sheetShape = RoundedCornerShape(8.dp),
        scrimColor = FinancialConnectionsTheme.colors.textSecondary.copy(alpha = 0.5f),
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
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
        AnnotatedText(
            text = aboveCta,
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
            Text(text = consent.cta)
        }
        if (belowCta != null) {
            Spacer(modifier = Modifier.size(24.dp))
            AnnotatedText(
                modifier = Modifier.fillMaxWidth(),
                text = belowCta,
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
                defaultStyle = FinancialConnectionsTheme.typography.heading.copy(
                    color = FinancialConnectionsTheme.colors.textPrimary
                ),
                annotationStyles = emptyMap(),
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
                text = learnMore,
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
                    style = FinancialConnectionsTheme.typography.bodyEmphasized.copy(
                        color = FinancialConnectionsTheme.colors.textPrimary
                    )
                )
            }
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
        val modifier = Modifier
            .size(16.dp)
            .offset(y = 2.dp)
        StripeImage(
            url = iconUrl,
            imageLoader = LocalImageLoader.current,
            contentDescription = null,
            errorContent = { InstitutionPlaceholder(modifier) },
            modifier = modifier
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
