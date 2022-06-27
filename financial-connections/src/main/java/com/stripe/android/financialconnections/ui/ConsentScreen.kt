package com.stripe.android.financialconnections.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewModel
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

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
    ConsentContent(
        state = state.value,
        onContinueClick = viewModel::onContinueClick,
        onClickableTextClick = viewModel::onClickableTextClick
    )
}

@Composable
private fun ConsentContent(
    state: ConsentState,
    onContinueClick: () -> Unit,
    onClickableTextClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Subtitle(state)
            Spacer(modifier = Modifier.size(24.dp))
            state.bullets.forEach { (icon, text) ->
                ConsentBodyWithIcon(icon, text) { onClickableTextClick(it) }
                Spacer(modifier = Modifier.size(16.dp))
            }
        }
        Column(
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            AnnotatedText(
                resource = TextResource.StringId(R.string.consent_pane_tc),
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
                Text(text = stringResource(R.string.consent_pane_agree))
            }
        }
    }
}

@Composable
fun clickableTextSpanStyle() = FinancialConnectionsTheme.typography.bodyEmphasized
    .toSpanStyle()
    .copy(color = FinancialConnectionsTheme.colors.textBrand)

@Composable
private fun Subtitle(state: ConsentState) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        text = state.title,
        textAlign = TextAlign.Center,
        color = FinancialConnectionsTheme.colors.textPrimary,
        style = FinancialConnectionsTheme.typography.subtitle
    )
}

@Composable
private fun ConsentBodyWithIcon(
    icon: Int,
    text: TextResource,
    onClickableTextClick: (String) -> Unit
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
            onClickableTextClick = { onClickableTextClick(it) },
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
                state = ConsentState(
                    title = "Random title",
                    bullets = listOf(
                        R.drawable.stripe_ic_lock to TextResource.StringId(R.string.consent_pane_body2)
                    )
                ),
                onContinueClick = {}
            ) {}
        }
    }
}
