package com.stripe.android.financialconnections.features.genericerror

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.features.common.FullScreenGenericLoading
import com.stripe.android.financialconnections.features.common.PrepaneImage
import com.stripe.android.financialconnections.features.generic.GenericHeader
import com.stripe.android.financialconnections.model.GenericErrorPane
import com.stripe.android.financialconnections.model.GenericErrorPane.PrimaryCtaAction
import com.stripe.android.financialconnections.presentation.paneViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.colors
import com.stripe.android.financialconnections.ui.theme.Layout
import com.stripe.android.uicore.utils.collectAsState

@Composable
internal fun GenericErrorScreen() {
    val viewModel: GenericErrorViewModel = paneViewModel(GenericErrorViewModel.Companion::factory)
    // This pane replaces the one the error came from, so there's nothing to go back to.
    BackHandler(true) { }
    val state by viewModel.stateFlow.collectAsState()

    GenericErrorContent(
        pane = state.pane,
        onPrimaryCtaClick = viewModel::onPrimaryCtaClick,
        onClickableTextClick = viewModel::onClickableTextClick,
    )
}

@Composable
private fun GenericErrorContent(
    pane: GenericErrorPane?,
    onPrimaryCtaClick: () -> Unit,
    onClickableTextClick: (String) -> Unit,
) {
    if (pane == null) {
        // The view model navigates away when there's no content, so this is only ever a brief
        // placeholder.
        FullScreenGenericLoading()
        return
    }

    Layout(
        // No horizontal inset, so the image band can bleed to both edges. The header re-adds it.
        bodyPadding = PaddingValues(vertical = 16.dp),
        footer = {
            FinancialConnectionsButton(
                onClick = onPrimaryCtaClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = pane.primaryCtaLabel())
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            GenericHeader(
                payload = pane.toHeader(),
                onClickableTextClick = onClickableTextClick,
                modifier = Modifier.padding(horizontal = 24.dp),
            )

            // The server sends a screenshot of the institution's own consent screen, so it gets the
            // same phone-frame treatment as the OAuth prepane rather than being drawn plainly.
            pane.imageUrl?.let { PrepaneImage(imageUrl = it) }
        }
    }
}

@Composable
private fun GenericErrorPane.primaryCtaLabel(): String = when (primaryCtaAction) {
    PrimaryCtaAction.RestartAuthFlow -> primaryCta
    // We don't know how to perform the action the server asked for, so we fall back to letting the
    // user pick a different bank. Use our own copy so the button can't promise something we won't
    // do.
    null -> stringResource(R.string.stripe_error_cta_select_another_bank)
}

@Preview(group = "Generic Error Pane", showBackground = true)
@Composable
internal fun GenericErrorScreenPreview(
    @PreviewParameter(GenericErrorPreviewParameterProvider::class) pane: GenericErrorPane
) {
    FinancialConnectionsPreview {
        // The pane relies on the host scaffold for its background outside of previews.
        Surface(color = colors.background) {
            GenericErrorContent(
                pane = pane,
                onPrimaryCtaClick = {},
                onClickableTextClick = {},
            )
        }
    }
}
