package com.stripe.android.financialconnections.features.accountupdate

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.features.common.InstitutionIcon
import com.stripe.android.financialconnections.presentation.paneViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.financialconnections.ui.theme.Layout
import com.stripe.android.uicore.utils.collectAsState

@Composable
internal fun AccountUpdateRequiredModal(
    backStackEntry: NavBackStackEntry,
    modifier: Modifier = Modifier
) {
    val viewModel: AccountUpdateRequiredViewModel = paneViewModel {
        AccountUpdateRequiredViewModel.factory(it, backStackEntry.arguments)
    }

    val state by viewModel.stateFlow.collectAsState()

    AccountUpdateRequiredModalContent(
        payload = state.payload(),
        onContinue = viewModel::handleContinue,
        onCancel = viewModel::handleCancel,
        modifier = modifier,
    )
}

@Composable
private fun AccountUpdateRequiredModalContent(
    payload: AccountUpdateRequiredState.Payload?,
    onContinue: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Layout(
        modifier = modifier,
        inModal = true,
        footer = {
            Column {
                Spacer(modifier = Modifier.size(16.dp))
                FinancialConnectionsButton(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.stripe_prepane_continue))
                }
                Spacer(modifier = Modifier.size(8.dp))
                FinancialConnectionsButton(
                    onClick = onCancel,
                    type = FinancialConnectionsButton.Type.Secondary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.stripe_prepane_cancel_cta))
                }
            }
        },
        body = {
            InstitutionIcon(institutionIcon = payload?.iconUrl)

            Spacer(modifier = Modifier.height(16.dp))

            AnnotatedText(
                text = TextResource.StringId(R.string.stripe_update_required_title),
                defaultStyle = FinancialConnectionsTheme.typography.headingMedium.copy(
                    color = FinancialConnectionsTheme.colors.textDefault
                ),
                onClickableTextClick = {},
            )

            Spacer(modifier = Modifier.height(16.dp))

            AnnotatedText(
                text = TextResource.StringId(R.string.stripe_update_required_desc),
                onClickableTextClick = {},
                defaultStyle = FinancialConnectionsTheme.typography.bodyMedium,
            )
        },
    )
}

@Preview(
    group = "Account Update Required Modal",
)
@Composable
internal fun AccountUpdateRequiredModalPreview() {
    FinancialConnectionsPreview {
        AccountUpdateRequiredModalContent(
            payload = AccountUpdateRequiredState.Payload(
                iconUrl = null,
                type = AccountUpdateRequiredState.Type.PartnerAuth(
                    institution = null,
                ),
            ),
            onContinue = {},
            onCancel = {},
        )
    }
}
