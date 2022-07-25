@file:OptIn(ExperimentalMaterialApi::class)

package com.stripe.android.financialconnections.features.consent

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
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
import androidx.compose.material.Icon
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.features.consent.ConsentState.ViewEffect
import com.stripe.android.financialconnections.presentation.CreateBrowserIntentForUrl
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import kotlinx.coroutines.launch

@ExperimentalMaterialApi
@Composable
internal fun ConsentScreen() {
    // update step state when manifest changes
    val viewModel: ConsentViewModel = mavericksViewModel()
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
        onConfirmModalClick = { scope.launch { bottomSheetState.hide() } }
    )
}

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
    onConfirmModalClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    ModalBottomSheetLayout(
        sheetState = bottomSheetState,
        sheetBackgroundColor = FinancialConnectionsTheme.colors.backgroundSurface,
        sheetShape = RoundedCornerShape(8.dp),
        scrimColor = FinancialConnectionsTheme.colors.textSecondary.copy(alpha = 0.5f),
        sheetContent = {
            ConsentPermissionsBottomSheetContent(
                requestedDataTitle = state.requestedDataTitle,
                requestedDataBullets = state.requestedDataBullets,
                onConfirmModalClick = onConfirmModalClick,
                onClickableTextClick = onClickableTextClick
            )
        },
        content = {
            ConsentMainContent(
                scrollState = scrollState,
                state = state,
                onClickableTextClick = onClickableTextClick,
                onContinueClick = onContinueClick
            )
        }
    )
}

@Composable
private fun ConsentMainContent(
    scrollState: ScrollState,
    state: ConsentState,
    onClickableTextClick: (String) -> Unit,
    onContinueClick: () -> Unit
) {
    FinancialConnectionsScaffold {
        Column(
            Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(24.dp)
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = state.title.toText().toString(),
                    textAlign = TextAlign.Center,
                    color = FinancialConnectionsTheme.colors.textPrimary,
                    style = FinancialConnectionsTheme.typography.subtitle
                )
                Spacer(modifier = Modifier.size(24.dp))
                state.bullets.forEach { (icon, text) ->
                    ConsentBullet(icon, text) { onClickableTextClick(it) }
                    Spacer(modifier = Modifier.size(16.dp))
                }

                Spacer(modifier = Modifier.weight(1f))
            }
            ConsentFooter(
                acceptConsent = state.acceptConsent,
                onClickableTextClick = onClickableTextClick,
                onContinueClick = onContinueClick
            )
        }
    }
}

@Composable
private fun ConsentFooter(
    acceptConsent: Async<Unit>,
    onClickableTextClick: (String) -> Unit,
    onContinueClick: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
        AnnotatedText(
            text = TextResource.StringId(R.string.consent_pane_tc),
            onClickableTextClick = { onClickableTextClick(it) },
            textStyle = FinancialConnectionsTheme.typography.body.copy(
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
            Text(text = stringResource(R.string.stripe_consent_pane_agree))
        }
    }
}

@Composable
private fun ConsentPermissionsBottomSheetContent(
    requestedDataTitle: TextResource,
    requestedDataBullets: List<Pair<TextResource, TextResource>>,
    onClickableTextClick: (String) -> Unit,
    onConfirmModalClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column {
        Column(
            Modifier
                .verticalScroll(scrollState)
                .padding(24.dp)
        ) {
            Text(
                text = requestedDataTitle.toText().toString(),
                color = FinancialConnectionsTheme.colors.textPrimary,
                style = FinancialConnectionsTheme.typography.heading
            )
            requestedDataBullets.forEach { (title, description) ->
                Spacer(modifier = Modifier.size(24.dp))
                ConsentBottomSheetBullet(
                    title = TextResource.Text(title.toText().toString()),
                    description = TextResource.Text(description.toText().toString())
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
                text = TextResource.StringId(R.string.stripe_consent_requested_data_learnmore),
                onClickableTextClick = { onClickableTextClick(it) },
                textStyle = FinancialConnectionsTheme.typography.body.copy(
                    color = FinancialConnectionsTheme.colors.textSecondary
                ),
                clickableStyle = FinancialConnectionsTheme.typography.caption
                    .toSpanStyle()
                    .copy(color = FinancialConnectionsTheme.colors.textBrand)
            )
            Spacer(modifier = Modifier.size(16.dp))
            FinancialConnectionsButton(
                onClick = { onConfirmModalClick() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.stripe_ok))
            }
        }
    }
}

@Composable
private fun ConsentBottomSheetBullet(
    title: TextResource,
    description: TextResource
) {
    Row {
        Icon(
            painter = painterResource(id = R.drawable.stripe_ic_check),
            contentDescription = null,
            tint = FinancialConnectionsTheme.colors.textSuccess,
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
            Text(
                text = description.toText().toString(),
                style = FinancialConnectionsTheme.typography.caption.copy(
                    color = FinancialConnectionsTheme.colors.textSecondary
                )
            )
        }
    }
}

@Composable
private fun ConsentBullet(
    icon: Int,
    text: TextResource,
    onClickableTextClick: ((String) -> Unit)? = null
) {
    Row {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = FinancialConnectionsTheme.colors.textSecondary,
            modifier = Modifier
                .size(16.dp)
                .offset(y = 4.dp)
        )
        Spacer(modifier = Modifier.size(8.dp))
        AnnotatedText(
            text,
            onClickableTextClick = { onClickableTextClick?.invoke(it) },
            textStyle = FinancialConnectionsTheme.typography.body.copy(
                color = FinancialConnectionsTheme.colors.textSecondary
            )
        )
    }
}

@Composable
@Preview(group = "Consent Pane", name = "canonical")
internal fun ContentPreview(
    state: ConsentState = ConsentStates.canonical()
) {
    FinancialConnectionsTheme {
        ConsentContent(
            bottomSheetState = rememberModalBottomSheetState(
                ModalBottomSheetValue.Hidden,
                skipHalfExpanded = true
            ),
            state = state,
            onContinueClick = {},
            onConfirmModalClick = {},
            onClickableTextClick = {}
        )
    }
}
