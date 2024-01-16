@file:Suppress("TooManyFunctions")

package com.stripe.android.financialconnections.features.manualentry

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
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
import com.stripe.android.financialconnections.ui.components.elevation
import com.stripe.android.financialconnections.ui.components.filtered
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

@Composable
internal fun ManualEntryScreen() {
    val viewModel: ManualEntryViewModel = mavericksViewModel()
    val parentViewModel = parentViewModel()
    val state: State<ManualEntryState> = viewModel.collectAsState()
    ManualEntryContent(
        routing = state.value.routing to state.value.routingError,
        account = state.value.account to state.value.accountError,
        accountConfirm = state.value.accountConfirm to state.value.accountConfirmError,
        isValidForm = state.value.isValidForm,
        payload = state.value.payload,
        linkPaymentAccountStatus = state.value.linkPaymentAccount,
        onRoutingEntered = viewModel::onRoutingEntered,
        onAccountEntered = viewModel::onAccountEntered,
        onAccountConfirmEntered = viewModel::onAccountConfirmEntered,
        onSubmit = viewModel::onSubmit,
    ) { parentViewModel.onCloseWithConfirmationClick(Pane.MANUAL_ENTRY) }
}

@Composable
private fun ManualEntryContent(
    routing: Pair<String?, Int?>,
    account: Pair<String?, Int?>,
    accountConfirm: Pair<String?, Int?>,
    isValidForm: Boolean,
    payload: Async<Payload>,
    linkPaymentAccountStatus: Async<LinkAccountSessionPaymentAccount>,
    onRoutingEntered: (String) -> Unit,
    onAccountEntered: (String) -> Unit,
    onAccountConfirmEntered: (String) -> Unit,
    onSubmit: () -> Unit,
    onCloseClick: () -> Unit
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
                    onRoutingEntered = onRoutingEntered,
                    account = account,
                    onAccountEntered = onAccountEntered,
                    accountConfirm = accountConfirm,
                    onAccountConfirmEntered = onAccountConfirmEntered,
                    isValidForm = isValidForm,
                    onSubmit = onSubmit
                )
            }
        }
    }
}

@Composable
@Suppress("LongMethod")
private fun ManualEntryLoaded(
    scrollState: ScrollState,
    payload: Payload,
    linkPaymentAccountStatus: Async<LinkAccountSessionPaymentAccount>,
    routing: Pair<String?, Int?>,
    onRoutingEntered: (String) -> Unit,
    account: Pair<String?, Int?>,
    onAccountEntered: (String) -> Unit,
    accountConfirm: Pair<String?, Int?>,
    onAccountConfirmEntered: (String) -> Unit,
    isValidForm: Boolean,
    onSubmit: () -> Unit
) {
    Column(
        Modifier.fillMaxSize()
    ) {
        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(
                    top = 16.dp,
                    start = 24.dp,
                    end = 24.dp,
                    bottom = 24.dp
                )
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.stripe_manualentry_title),
                color = FinancialConnectionsTheme.v3Colors.textDefault,
                style = FinancialConnectionsTheme.v3Typography.headingXLarge
            )
            Spacer(modifier = Modifier.size(16.dp))
            if (payload.verifyWithMicrodeposits) {
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.stripe_manualentry_microdeposits_desc),
                    color = FinancialConnectionsTheme.v3Colors.textDefault,
                    style = FinancialConnectionsTheme.v3Typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.size(8.dp))

            InputWithError(
                label = R.string.stripe_manualentry_routing,
                inputWithError = routing,
                testTag = "RoutingInput",
                onInputChanged = onRoutingEntered,
            )
            Spacer(modifier = Modifier.size(16.dp))
            InputWithError(
                label = R.string.stripe_manualentry_account,
                inputWithError = account,
                testTag = "AccountInput",
                onInputChanged = onAccountEntered,
            )
            Spacer(modifier = Modifier.size(16.dp))
            InputWithError(
                label = R.string.stripe_manualentry_accountconfirm,
                inputWithError = accountConfirm,
                testTag = "ConfirmAccountInput",
                onInputChanged = onAccountConfirmEntered,
            )
            if (linkPaymentAccountStatus is Fail) {
                Spacer(modifier = Modifier.size(16.dp))
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    text = (linkPaymentAccountStatus.error as? StripeException)?.message
                        ?: stringResource(R.string.stripe_error_generic_title),
                    style = FinancialConnectionsTheme.v3Typography.bodyMedium,
                    color = FinancialConnectionsTheme.v3Colors.textCritical,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
        }
        // Footer
        ManualEntryFooter(isValidForm, onSubmit)
    }
}

@Composable
private fun ManualEntryFooter(
    isValidForm: Boolean,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier.padding(24.dp)
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun InputWithError(
    inputWithError: Pair<String?, Int?>,
    label: Int,
    testTag: String,
    onInputChanged: (String) -> Unit
) {
    var textValue by remember { mutableStateOf(TextFieldValue()) }
    Spacer(modifier = Modifier.size(4.dp))
    FinancialConnectionsOutlinedTextField(
        value = textValue,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
        ),
        placeholder = {
            Text(
                text = stringResource(id = label),
                style = FinancialConnectionsTheme.v3Typography.labelLarge,
                color = FinancialConnectionsTheme.v3Colors.textSubdued
            )
        },
        isError = inputWithError.second != null,
        onValueChange = { text ->
            textValue = text.filtered { it.isDigit() }
            onInputChanged(textValue.text)
        },
        modifier = Modifier
            .semantics { testTagsAsResourceId = true }
            .testTag(testTag)
    )
    if (inputWithError.second != null) {
        Spacer(modifier = Modifier.size(4.dp))
        Text(
            text = stringResource(id = inputWithError.second!!),
            color = FinancialConnectionsTheme.v3Colors.textCritical,
            style = FinancialConnectionsTheme.v3Typography.labelSmall,
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
            routing = state.routing to state.routingError,
            account = state.account to state.accountError,
            accountConfirm = state.accountConfirm to state.accountConfirmError,
            isValidForm = true,
            payload = state.payload,
            linkPaymentAccountStatus = state.linkPaymentAccount,
            onRoutingEntered = {},
            onAccountEntered = {},
            onAccountConfirmEntered = {},
            onSubmit = {},
        ) {}
    }
}
