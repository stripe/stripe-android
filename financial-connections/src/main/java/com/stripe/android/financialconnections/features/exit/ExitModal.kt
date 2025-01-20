package com.stripe.android.financialconnections.features.exit

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.features.common.ShapedIcon
import com.stripe.android.financialconnections.presentation.paneViewModel
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.colors
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.typography
import com.stripe.android.financialconnections.ui.theme.Layout
import com.stripe.android.uicore.utils.collectAsState

@Composable
internal fun ExitModal(
    backStackEntry: NavBackStackEntry
) {
    val viewModel: ExitViewModel = paneViewModel {
        ExitViewModel.factory(it, backStackEntry.arguments)
    }

    val state by viewModel.stateFlow.collectAsState()
    state.payload()?.let {
        ExitModalContent(
            description = it.description,
            loading = state.closing,
            onExit = viewModel::onCloseConfirm,
            onCancel = viewModel::onCloseDismiss
        )
    }
}

@Composable
private fun ExitModalContent(
    description: TextResource,
    loading: Boolean,
    onExit: () -> Unit,
    onCancel: () -> Unit,
) {
    Layout(
        inModal = true,
        bodyPadding = PaddingValues(
            top = 0.dp,
            start = 24.dp,
            end = 24.dp,
            bottom = 24.dp,
        ),
    ) {
        ShapedIcon(
            painter = painterResource(id = R.drawable.stripe_ic_panel_arrow_right),
            contentDescription = stringResource(R.string.stripe_exit_modal_title)
        )
        Spacer(modifier = Modifier.size(16.dp))
        Text(
            text = stringResource(R.string.stripe_exit_modal_title),
            style = typography.headingLarge,
            color = colors.textDefault,
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = description.toText().toString(),
            style = typography.bodyMedium,
            color = colors.textDefault,
        )
        Spacer(modifier = Modifier.size(24.dp))
        FinancialConnectionsButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading,
            onClick = onCancel,
        ) {
            Text(text = stringResource(id = R.string.stripe_exit_modal_cta_cancel))
        }
        Spacer(modifier = Modifier.size(8.dp))
        FinancialConnectionsButton(
            modifier = Modifier.fillMaxWidth(),
            loading = loading,
            enabled = !loading,
            type = FinancialConnectionsButton.Type.Secondary,
            onClick = onExit,
        ) {
            Text(text = stringResource(id = R.string.stripe_exit_modal_cta_accept))
        }
    }
}

@Composable
@Preview
internal fun ExitModalPreview() {
    FinancialConnectionsTheme {
        Surface(color = colors.backgroundSurface) {
            ExitModalContent(
                description = TextResource.StringId(R.string.stripe_exit_modal_desc, listOf("MerchantName")),
                loading = false,
                onExit = {},
                onCancel = {}
            )
        }
    }
}
