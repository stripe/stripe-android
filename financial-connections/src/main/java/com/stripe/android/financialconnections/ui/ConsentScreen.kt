@file:OptIn(ExperimentalMaterialApi::class)

package com.stripe.android.financialconnections.ui

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksActivityViewModel
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.presentation.ConsentState
import com.stripe.android.financialconnections.presentation.ConsentViewModel
import com.stripe.android.financialconnections.presentation.CreateBrowserIntentForUrl
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewModel
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@ExperimentalMaterialApi
@Composable
internal fun ConsentScreen() {
    // get shared configuration from activity state
    val activityViewModel: FinancialConnectionsSheetNativeViewModel = mavericksActivityViewModel()
    val manifest = activityViewModel.collectAsState { it.manifest }

    // update step state when manifest changes
    val viewModel: ConsentViewModel = mavericksViewModel()
    LaunchedEffect(manifest.value) { viewModel.onManifestChanged(manifest.value) }

    val state = viewModel.collectAsState()

    // create bottom sheet state.
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(
        ModalBottomSheetValue.Hidden,
        confirmStateChange = {
            if (it == ModalBottomSheetValue.Hidden) viewModel.onModalBottomSheetClosed()
            true
        }
    )

    ViewEffect(
        viewModel = viewModel,
        viewEffect = state.value.viewEffect
    )

    BottomSheetStateEffect(
        bottomSheetType = state.value.bottomSheetType,
        scope = scope,
        bottomSheetState = bottomSheetState
    )

    ConsentContent(
        state = state.value,
        bottomSheetState = bottomSheetState,
        onContinueClick = viewModel::onContinueClick,
        onClickableTextClick = viewModel::onClickableTextClick,
        onConfirmModalClick = viewModel::onConfirmModalClick
    )
}

@Composable
private fun ViewEffect(
    viewModel: ConsentViewModel,
    viewEffect: ConsentState.ViewEffect?
) {
    val context = LocalContext.current
    LaunchedEffect(viewEffect) {
        when (viewEffect) {
            is ConsentState.ViewEffect.OpenUrl -> {
                context.startActivity(
                    CreateBrowserIntentForUrl(
                        context = context,
                        uri = Uri.parse(viewEffect.url),
                    )
                )
            }
            null -> Unit
        }
        viewModel.onViewEffectLaunched()
    }
}

@Composable
private fun BottomSheetStateEffect(
    bottomSheetType: ConsentState.BottomSheetType,
    scope: CoroutineScope,
    bottomSheetState: ModalBottomSheetState
) {
    LaunchedEffect(bottomSheetType) {
        scope.launch {
            when (bottomSheetType) {
                ConsentState.BottomSheetType.NONE -> bottomSheetState.hide()
                else -> bottomSheetState.show()
            }
        }
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
    ModalBottomSheetLayout(
        sheetState = bottomSheetState,
        sheetBackgroundColor = FinancialConnectionsTheme.colors.backgroundSurface,
        sheetShape = RoundedCornerShape(8.dp),
        scrimColor = FinancialConnectionsTheme.colors.textSecondary.copy(alpha = 0.5f),
        sheetContent = {
            RequestedDataBottomSheetContent(
                requestedDataTitle = state.requestedDataTitle,
                requestedDataBullets = state.requestedDataBullets,
                onConfirmModalClick = onConfirmModalClick,
                onClickableTextClick = onClickableTextClick
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.TopCenter)
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
            }
            Column(
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                AnnotatedText(
                    text = TextResource.StringId(R.string.consent_pane_tc),
                    onClickableTextClick = { onClickableTextClick(it) },
                    textStyle = FinancialConnectionsTheme.typography.body.copy(
                        textAlign = TextAlign.Center,
                        color = FinancialConnectionsTheme.colors.textSecondary
                    )
                )
                Spacer(modifier = Modifier.size(24.dp))
                FinancialConnectionsButton(
                    onClick = onContinueClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.stripe_consent_pane_agree))
                }
            }
        }
    }
}

@Composable
private fun RequestedDataBottomSheetContent(
    requestedDataTitle: TextResource,
    requestedDataBullets: List<Pair<TextResource, TextResource>>,
    onClickableTextClick: (String) -> Unit,
    onConfirmModalClick: () -> Unit
) {
    Column(
        Modifier
            .padding(24.dp)
    ) {
        Text(
            text = requestedDataTitle.toText().toString(),
            color = FinancialConnectionsTheme.colors.textPrimary,
            style = FinancialConnectionsTheme.typography.heading
        )
        Spacer(modifier = Modifier.size(24.dp))
        requestedDataBullets.forEach { (title, description) ->
            ConsentBottomSheetBullet(
                title = TextResource.Text(title.toText().toString()),
                description = TextResource.Text(description.toText().toString())
            )
            Spacer(modifier = Modifier.size(24.dp))
        }

        AnnotatedText(
            text = TextResource.StringId(R.string.stripe_consent_requested_data_learnmore),
            onClickableTextClick = { onClickableTextClick(it) },
            textStyle = FinancialConnectionsTheme.typography.body.copy(
                color = FinancialConnectionsTheme.colors.textSecondary,
            ),
            clickableStyle = FinancialConnectionsTheme.typography.caption
                .toSpanStyle()
                .copy(color = FinancialConnectionsTheme.colors.textBrand)
        )
        Spacer(modifier = Modifier.size(24.dp))
        FinancialConnectionsButton(
            onClick = { onConfirmModalClick() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.stripe_ok))
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
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Column {
            Text(text = title.toText().toString(),
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
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.size(8.dp))
        AnnotatedText(
            text,
            onClickableTextClick = { onClickableTextClick?.invoke(it) },
            textStyle = FinancialConnectionsTheme.typography.body.copy(
                color = FinancialConnectionsTheme.colors.textSecondary,
            )
        )
    }
}

@Composable
@Preview(
    showBackground = true
)
private fun ContentPreview() {
    FinancialConnectionsTheme {
        FinancialConnectionsScaffold {
            ConsentContent(
                bottomSheetState = rememberModalBottomSheetState(
                    initialValue = ModalBottomSheetValue.Expanded
                ),
                state = ConsentState(
                    title = TextResource.Text("Body"),
                    bullets = listOf(
                        R.drawable.stripe_ic_check to TextResource.Text("Text")
                    ),
                    requestedDataBullets = listOf(
                        TextResource.Text("Requested data") to
                            TextResource.Text("Requested data")
                    ),
                    requestedDataTitle = TextResource.Text("Body")
                ),
                onContinueClick = {},
                onConfirmModalClick = {},
                onClickableTextClick = {}
            )
        }
    }
}
