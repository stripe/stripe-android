package com.stripe.android.financialconnections.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksActivityViewModel
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.presentation.ConsentState
import com.stripe.android.financialconnections.presentation.ConsentViewModel
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewModel
import com.stripe.android.financialconnections.ui.components.SpannedBody
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.SpanSection
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

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
        onContinueClick = viewModel::onContinueClick
    )
}

@Composable
private fun ConsentContent(
    state: ConsentState,
    onContinueClick: () -> Unit
) {
    // Screen content
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Subtitle(state)
            Spacer(modifier = Modifier.size(24.dp))
            state.content.forEach {
                BodyWithIcon(it)
                Spacer(modifier = Modifier.size(16.dp))
            }
        }
        Column(
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            SpannedBody(
                listOf(
                    SpanSection.Raw("You agree to Stripe’s "),
                    SpanSection.Link("Terms", "Terms") {},
                    SpanSection.Raw(" and "),
                    SpanSection.Link("Privacy Policy", "PRIVACY") {},
                    SpanSection.Raw(". "),
                    SpanSection.Link("Learn more", "MORE") {},
                )
            )
            Spacer(modifier = Modifier.size(24.dp))
            FinancialConnectionsButton(
                onClick = onContinueClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Agree")
            }
        }
    }
}

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
private fun BodyWithIcon(text: String) {
    Row {
        Icon(
            Icons.Filled.AccountBox,
            contentDescription = null,
            tint = FinancialConnectionsTheme.colors.textSecondary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Body(text)
    }
}

@Composable
private fun Body(
    text: String,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        textAlign = textAlign,
        color = FinancialConnectionsTheme.colors.textSecondary,
        style = FinancialConnectionsTheme.typography.body
    )
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
                    content = listOf(
                        "Random very long text that takes more than one line on the screen.",
                        "Random very long text that takes more than one line on the screen.",
                        "Random very long text that takes more than one line on the screen.",
                        "Random very long text that takes more than one line on the screen.",
                    )
                ),
                onContinueClick = {}
            )
        }
    }
}
