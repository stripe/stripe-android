@file:Suppress("TooManyFunctions")

package com.stripe.android.financialconnections.features.manualentry

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.core.exception.StripeException
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.model.LinkAccountSessionPaymentAccount
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsOutlinedTextField
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

@Composable
internal fun ManualEntryScreen() {
    val viewModel: ManualEntryViewModel = mavericksViewModel()
    val parentViewModel = parentViewModel()
    val state: State<ManualEntryState> = viewModel.collectAsState()

    ManualEntryContent(
        routing = state.value.routing,
        account = state.value.account,
        accountConfirm = state.value.accountConfirm,
        isValidForm = state.value.isValidForm,
        verifyWithMicrodeposits = state.value.verifyWithMicrodeposits,
        linkPaymentAccountStatus = state.value.linkPaymentAccount,
        onRoutingEntered = viewModel::onRoutingEntered,
        onAccountEntered = viewModel::onAccountEntered,
        onAccountConfirmEntered = viewModel::onAccountConfirmEntered,
        onSubmit = viewModel::onSubmit,
        onCloseClick = parentViewModel::onCloseClick,
    )
}

@Suppress("LongMethod")
@Composable
private fun ManualEntryContent(
    routing: Pair<String?, Int?>,
    account: Pair<String?, Int?>,
    accountConfirm: Pair<String?, Int?>,
    isValidForm: Boolean,
    verifyWithMicrodeposits: Boolean,
    linkPaymentAccountStatus: Async<LinkAccountSessionPaymentAccount>,
    onRoutingEntered: (String) -> Unit,
    onAccountEntered: (String) -> Unit,
    onAccountConfirmEntered: (String) -> Unit,
    onSubmit: () -> Unit,
    onCloseClick: () -> Unit,
) {
    val scrollState = rememberScrollState()
    FinancialConnectionsScaffold(
        topBar = { FinancialConnectionsTopAppBar(onCloseClick = onCloseClick) }
    ) {
        Column(
            Modifier.fillMaxSize()
        ) {
            // Scrollable content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(24.dp)
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.stripe_manualentry_title),
                    color = FinancialConnectionsTheme.colors.textPrimary,
                    style = FinancialConnectionsTheme.typography.subtitle
                )
                if (verifyWithMicrodeposits) {
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = stringResource(R.string.stripe_manualentry_microdeposits_desc),
                        color = FinancialConnectionsTheme.colors.textPrimary,
                        style = FinancialConnectionsTheme.typography.body
                    )
                }
                if (linkPaymentAccountStatus is Fail) {
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = (linkPaymentAccountStatus.error as? StripeException)?.message
                            ?: stringResource(R.string.stripe_error_generic_title),
                        color = FinancialConnectionsTheme.colors.textCritical,
                        style = FinancialConnectionsTheme.typography.body
                    )
                }
                Spacer(modifier = Modifier.size(24.dp))
                InputWithError(
                    label = R.string.stripe_manualentry_routing,
                    inputWithError = routing,
                    onInputChanged = onRoutingEntered,
                )
                Spacer(modifier = Modifier.size(24.dp))
                InputWithError(
                    label = R.string.stripe_manualentry_account,
                    inputWithError = account,
                    onInputChanged = onAccountEntered,
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.stripe_manualentry_account_type_disclaimer),
                    color = FinancialConnectionsTheme.colors.textPrimary,
                    style = FinancialConnectionsTheme.typography.body
                )
                Spacer(modifier = Modifier.size(24.dp))
                InputWithError(
                    label = R.string.stripe_manualentry_accountconfirm,
                    inputWithError = accountConfirm,
                    onInputChanged = onAccountConfirmEntered,
                )
                Spacer(modifier = Modifier.weight(1f))
            }
            // Footer
            ManualEntryFooter(isValidForm, onSubmit)
        }
    }
}

@Composable
private fun ManualEntryFooter(
    isValidForm: Boolean,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier.padding(
            horizontal = 24.dp,
            vertical = 16.dp
        )
    ) {
        FinancialConnectionsButton(
            enabled = isValidForm,
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.stripe_manualentry_cta))
        }
    }
}

@Composable
private fun InputWithError(
    inputWithError: Pair<String?, Int?>,
    label: Int,
    onInputChanged: (String) -> Unit,
) {
    FinancialConnectionsOutlinedTextField(
        label = { Text(stringResource(label)) },
        value = inputWithError.first ?: "",
        isError = inputWithError.second != null,
        onValueChange = onInputChanged
    )
    if (inputWithError.second != null) {
        Text(
            text = stringResource(id = inputWithError.second!!),
            color = FinancialConnectionsTheme.colors.textCritical,
            style = FinancialConnectionsTheme.typography.captionEmphasized,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

@Preview
@Composable
internal fun ManualEntryScreenPreview() {
    FinancialConnectionsTheme {
        ManualEntryContent(
            routing = "" to null,
            account = "" to null,
            accountConfirm = "" to null,
            isValidForm = true,
            verifyWithMicrodeposits = true,
            linkPaymentAccountStatus = Uninitialized,
            onRoutingEntered = {},
            onAccountEntered = {},
            onAccountConfirmEntered = {},
            onSubmit = {},
            onCloseClick = {},
        )
    }
}
