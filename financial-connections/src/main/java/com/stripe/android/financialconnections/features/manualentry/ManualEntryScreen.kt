package com.stripe.android.financialconnections.features.manualentry

import androidx.annotation.StringRes
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.core.exception.StripeException
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.features.common.FullScreenGenericLoading
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.features.manualentry.ManualEntryState.Payload
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.LinkAccountSessionPaymentAccount
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsOutlinedTextField
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.components.TestModeBanner
import com.stripe.android.financialconnections.ui.components.elevation
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.financialconnections.ui.theme.Layout

@Composable
internal fun ManualEntryScreen() {
    val viewModel: ManualEntryViewModel = mavericksViewModel()
    val parentViewModel = parentViewModel()
    val state: ManualEntryState by viewModel.collectAsState()
    ManualEntryContent(
        routing = state.routing,
        routingError = state.routingError,
        account = state.account,
        accountError = state.accountError,
        accountConfirm = state.accountConfirm,
        accountConfirmError = state.accountConfirmError,
        isValidForm = state.isValidForm,
        payload = state.payload,
        linkPaymentAccountStatus = state.linkPaymentAccount,
        onRoutingEntered = viewModel::onRoutingEntered,
        onAccountEntered = viewModel::onAccountEntered,
        onAccountConfirmEntered = viewModel::onAccountConfirmEntered,
        onSubmit = viewModel::onSubmit,
        onTestFill = viewModel::onTestFill,
        onCloseClick = { parentViewModel.onCloseWithConfirmationClick(Pane.MANUAL_ENTRY) }
    )
}

@Composable
private fun ManualEntryContent(
    routing: String,
    routingError: Int?,
    account: String,
    accountError: Int?,
    accountConfirm: String,
    accountConfirmError: Int?,
    isValidForm: Boolean,
    payload: Async<Payload>,
    linkPaymentAccountStatus: Async<LinkAccountSessionPaymentAccount>,
    onRoutingEntered: (String) -> Unit,
    onAccountEntered: (String) -> Unit,
    onAccountConfirmEntered: (String) -> Unit,
    onSubmit: () -> Unit,
    onCloseClick: () -> Unit,
    onTestFill: () -> Unit
) {
    val scrollState = rememberScrollState()
    FinancialConnectionsScaffold(
        topBar = {
            FinancialConnectionsTopAppBar(
                onCloseClick = onCloseClick,
                elevation = scrollState.elevation
            )
        }
    ) {
        when (payload) {
            is Loading, Uninitialized -> FullScreenGenericLoading()
            is Fail -> UnclassifiedErrorContent(
                error = payload.error,
                onCloseFromErrorClick = {}
            )

            is Success -> when (payload().customManualEntry) {
                true -> FullScreenGenericLoading()
                false -> ManualEntryLoaded(
                    scrollState = scrollState,
                    linkPaymentAccountStatus = linkPaymentAccountStatus,
                    payload = payload(),
                    routing = routing,
                    routingError = routingError,
                    account = account,
                    accountError = accountError,
                    accountConfirm = accountConfirm,
                    accountConfirmError = accountConfirmError,
                    onRoutingEntered = onRoutingEntered,
                    onAccountEntered = onAccountEntered,
                    onAccountConfirmEntered = onAccountConfirmEntered,
                    isValidForm = isValidForm,
                    onSubmit = onSubmit,
                    onTestFill = onTestFill
                )
            }
        }
    }
}

@Composable
private fun ManualEntryLoaded(
    scrollState: ScrollState,
    payload: Payload,
    linkPaymentAccountStatus: Async<LinkAccountSessionPaymentAccount>,
    routing: String,
    routingError: Int?,
    account: String,
    accountError: Int?,
    accountConfirm: String,
    accountConfirmError: Int?,
    onRoutingEntered: (String) -> Unit,
    onAccountEntered: (String) -> Unit,
    onAccountConfirmEntered: (String) -> Unit,
    isValidForm: Boolean,
    onSubmit: () -> Unit,
    onTestFill: () -> Unit
) {
    val loading = linkPaymentAccountStatus is Loading
    Layout(
        scrollState = scrollState,
        body = {
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.stripe_manualentry_title),
                color = FinancialConnectionsTheme.colors.textDefault,
                style = FinancialConnectionsTheme.typography.headingXLarge
            )
            Spacer(modifier = Modifier.size(16.dp))
            if (payload.verifyWithMicrodeposits) {
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.stripe_manualentry_microdeposits_desc),
                    color = FinancialConnectionsTheme.colors.textDefault,
                    style = FinancialConnectionsTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.size(8.dp))

            if (payload.testMode) {
                TestModeBanner(
                    enabled = loading.not(),
                    buttonLabel = "Use test account",
                    onButtonClick = { onTestFill() }
                )
                Spacer(modifier = Modifier.size(8.dp))
            }
            AccountForm(
                loading = loading,
                routing = routing,
                routingError = routingError,
                onRoutingEntered = onRoutingEntered,
                account = account,
                accountError = accountError,
                onAccountEntered = onAccountEntered,
                accountConfirm = accountConfirm,
                accountConfirmError = accountConfirmError,
                onAccountConfirmEntered = onAccountConfirmEntered
            )
            if (linkPaymentAccountStatus is Fail) {
                Spacer(modifier = Modifier.size(16.dp))
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    text = (linkPaymentAccountStatus.error as? StripeException)?.message
                        ?: stringResource(R.string.stripe_error_generic_title),
                    style = FinancialConnectionsTheme.typography.bodyMedium,
                    color = FinancialConnectionsTheme.colors.textCritical,
                )
            }
        },
        footer = {
            ManualEntryFooter(
                isValidForm = isValidForm,
                loading = loading,
                onSubmit = onSubmit
            )
        }
    )
}

@Composable
private fun AccountForm(
    loading: Boolean,
    routing: String,
    routingError: Int?,
    onRoutingEntered: (String) -> Unit,
    account: String,
    accountError: Int?,
    onAccountEntered: (String) -> Unit,
    accountConfirm: String,
    accountConfirmError: Int?,
    onAccountConfirmEntered: (String) -> Unit
) {
    InputWithError(
        enabled = loading.not(),
        label = R.string.stripe_manualentry_routing,
        input = routing,
        error = routingError,
        testTag = "RoutingInput",
        onInputChanged = onRoutingEntered,
    )
    Spacer(modifier = Modifier.size(16.dp))
    InputWithError(
        enabled = loading.not(),
        label = R.string.stripe_manualentry_account,
        input = account,
        error = accountError,
        testTag = "AccountInput",
        onInputChanged = onAccountEntered,
    )
    Spacer(modifier = Modifier.size(16.dp))
    InputWithError(
        enabled = loading.not(),
        label = R.string.stripe_manualentry_accountconfirm,
        input = accountConfirm,
        error = accountConfirmError,
        testTag = "ConfirmAccountInput",
        onInputChanged = onAccountConfirmEntered,
    )
}

@Composable
private fun ManualEntryFooter(
    isValidForm: Boolean,
    loading: Boolean,
    onSubmit: () -> Unit
) {
    Column {
        FinancialConnectionsButton(
            loading = loading,
            enabled = isValidForm,
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.stripe_manualentry_cta))
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun InputWithError(
    enabled: Boolean,
    input: String,
    @StringRes error: Int?,
    label: Int,
    testTag: String,
    onInputChanged: (String) -> Unit
) {
    Spacer(modifier = Modifier.size(4.dp))
    FinancialConnectionsOutlinedTextField(
        enabled = enabled,
        value = input,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
        ),
        placeholder = {
            Text(
                text = stringResource(id = label),
                style = FinancialConnectionsTheme.typography.labelLarge,
                color = FinancialConnectionsTheme.colors.textSubdued
            )
        },
        isError = error != null,
        onValueChange = { newValue -> onInputChanged(newValue) },
        modifier = Modifier
            .semantics { testTagsAsResourceId = true }
            .testTag(testTag)
    )
    if (error != null) {
        Spacer(modifier = Modifier.size(4.dp))
        Text(
            text = stringResource(id = error),
            color = FinancialConnectionsTheme.colors.textCritical,
            style = FinancialConnectionsTheme.typography.labelSmall,
        )
    }
}

@Preview(
    group = "Manual Entry Pane",
)
@Composable
internal fun ManualEntryPreview(
    @PreviewParameter(ManualEntryPreviewParameterProvider::class) state: ManualEntryState
) {
    FinancialConnectionsPreview {
        ManualEntryContent(
            routing = state.routing,
            routingError = state.routingError,
            account = state.account,
            accountError = state.accountError,
            accountConfirm = state.accountConfirm,
            accountConfirmError = state.accountConfirmError,
            isValidForm = true,
            payload = state.payload,
            linkPaymentAccountStatus = state.linkPaymentAccount,
            onRoutingEntered = {},
            onAccountEntered = {},
            onAccountConfirmEntered = {},
            onTestFill = {},
            onSubmit = {},
            onCloseClick = {}
        )
    }
}
