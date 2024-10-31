package com.stripe.android.financialconnections.features.manualentry

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.stripe.android.core.exception.StripeException
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.features.common.FullScreenGenericLoading
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.features.manualentry.ManualEntryPreviewParameterProvider.PreviewState
import com.stripe.android.financialconnections.features.manualentry.ManualEntryState.Payload
import com.stripe.android.financialconnections.model.LinkAccountSessionPaymentAccount
import com.stripe.android.financialconnections.model.ManualEntryPane
import com.stripe.android.financialconnections.presentation.Async
import com.stripe.android.financialconnections.presentation.Async.Fail
import com.stripe.android.financialconnections.presentation.Async.Loading
import com.stripe.android.financialconnections.presentation.Async.Success
import com.stripe.android.financialconnections.presentation.Async.Uninitialized
import com.stripe.android.financialconnections.presentation.paneViewModel
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsOutlinedTextField
import com.stripe.android.financialconnections.ui.components.TestModeBanner
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.financialconnections.ui.theme.Layout
import com.stripe.android.uicore.utils.collectAsState

@Composable
internal fun ManualEntryScreen() {
    val viewModel: ManualEntryViewModel = paneViewModel {
        ManualEntryViewModel.factory(it)
    }
    val parentViewModel = parentViewModel()
    val state: ManualEntryState by viewModel.stateFlow.collectAsState()
    val form by viewModel.form.collectAsState()
    ManualEntryContent(
        routing = viewModel.routing,
        routingError = form.routingError,
        account = viewModel.account,
        accountError = form.accountError,
        accountConfirm = viewModel.accountConfirm,
        accountConfirmError = form.accountConfirmError,
        isValidForm = form.isValid,
        payload = state.payload,
        linkPaymentAccountStatus = state.linkPaymentAccount,
        onRoutingEntered = viewModel::onRoutingEntered,
        onAccountEntered = viewModel::onAccountEntered,
        onAccountConfirmEntered = viewModel::onAccountConfirmEntered,
        onSubmit = viewModel::onSubmit,
        onTestFill = viewModel::onTestFill,
        onCloseFromErrorClick = parentViewModel::onCloseFromErrorClick,
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
    onCloseFromErrorClick: (Throwable) -> Unit,
    onTestFill: () -> Unit
) {
    Box {
        when (payload) {
            is Loading, Uninitialized -> FullScreenGenericLoading()
            is Fail -> UnclassifiedErrorContent { onCloseFromErrorClick(payload.error) }

            is Success -> when (payload().customManualEntry) {
                true -> FullScreenGenericLoading()
                false -> ManualEntryLoaded(
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
    val scrollState = rememberScrollState()
    Layout(
        scrollState = scrollState,
        body = {
            Spacer(modifier = Modifier.size(8.dp))
            payload.content.title?.let {
                Title(it)
            }
            Spacer(modifier = Modifier.size(16.dp))
            if (payload.content.body != null) {
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = payload.content.body,
                    color = FinancialConnectionsTheme.colors.textDefault,
                    style = FinancialConnectionsTheme.typography.bodyMedium
                )
            }
            if (payload.content.testModeBannerLabel != null) {
                Spacer(modifier = Modifier.size(8.dp))
                TestModeBanner(
                    enabled = loading.not(),
                    buttonLabel = payload.content.testModeBannerLabel,
                    onButtonClick = onTestFill
                )
            }
            Spacer(modifier = Modifier.size(24.dp))
            AccountForm(
                content = payload.content,
                enabled = loading.not(),
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
                ErrorMessage(linkPaymentAccountStatus.error)
            }
        },
        footer = {
            ManualEntryFooter(
                isValidForm = isValidForm,
                loading = loading,
                cta = payload.content.cta,
                onSubmit = onSubmit
            )
        }
    )
}

@Composable
private fun ErrorMessage(
    error: Throwable
) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        text = (error as? StripeException)?.message
            ?: stringResource(R.string.stripe_error_generic_title),
        style = FinancialConnectionsTheme.typography.bodyMedium,
        color = FinancialConnectionsTheme.colors.textCritical,
    )
}

@Composable
private fun Title(title: String) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        text = title,
        color = FinancialConnectionsTheme.colors.textDefault,
        style = FinancialConnectionsTheme.typography.headingXLarge
    )
}

@Composable
private fun AccountForm(
    content: ManualEntryPane,
    enabled: Boolean,
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        content.routingNumberLabel?.let {
            InputWithError(
                enabled = enabled,
                label = it,
                input = routing,
                error = routingError,
                testTag = "RoutingInput",
                onInputChanged = onRoutingEntered,
            )
        }
        content.accountNumberLabel?.let {
            InputWithError(
                enabled = enabled,
                label = it,
                input = account,
                error = accountError,
                testTag = "AccountInput",
                onInputChanged = onAccountEntered,
            )
        }
        content.confirmAccountNumberLabel?.let {
            InputWithError(
                enabled = enabled,
                label = it,
                input = accountConfirm,
                error = accountConfirmError,
                testTag = "ConfirmAccountInput",
                onInputChanged = onAccountConfirmEntered,
            )
        }
    }
}

@Composable
private fun ManualEntryFooter(
    isValidForm: Boolean,
    loading: Boolean,
    cta: String,
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
            Text(text = cta)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun InputWithError(
    enabled: Boolean,
    input: String,
    @StringRes error: Int?,
    label: String,
    testTag: String,
    onInputChanged: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FinancialConnectionsOutlinedTextField(
            enabled = enabled,
            value = input,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
            ),
            placeholder = {
                Text(
                    text = label,
                    style = FinancialConnectionsTheme.typography.labelLarge,
                    color = FinancialConnectionsTheme.colors.textSubdued
                )
            },
            isError = error != null,
            onValueChange = onInputChanged,
            modifier = Modifier
                .semantics { testTagsAsResourceId = true }
                .testTag(testTag)
        )
        if (error != null) {
            Text(
                text = stringResource(id = error),
                color = FinancialConnectionsTheme.colors.textCritical,
                style = FinancialConnectionsTheme.typography.labelSmall,
            )
        }
    }
}

@Preview(
    group = "Manual Entry Pane",
)
@Composable
internal fun ManualEntryPreview(
    @PreviewParameter(ManualEntryPreviewParameterProvider::class) previewState: PreviewState
) {
    FinancialConnectionsPreview {
        ManualEntryContent(
            routing = previewState.routing,
            routingError = previewState.routingError,
            account = previewState.account,
            accountError = previewState.accountError,
            accountConfirm = previewState.accountConfirm,
            accountConfirmError = previewState.accountConfirmError,
            isValidForm = true,
            payload = previewState.state.payload,
            linkPaymentAccountStatus = previewState.state.linkPaymentAccount,
            onRoutingEntered = {},
            onAccountEntered = {},
            onAccountConfirmEntered = {},
            onTestFill = {},
            onSubmit = {},
            onCloseFromErrorClick = {}
        )
    }
}
