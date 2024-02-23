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
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.focus.FocusDirection.Companion.Next
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.ImeAction
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
import com.stripe.android.financialconnections.features.manualentry.ManualEntryState.InputState
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
    routing: InputState,
    account: InputState,
    accountConfirm: InputState,
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
    routing: InputState,
    onRoutingEntered: (String) -> Unit,
    account: InputState,
    onAccountEntered: (String) -> Unit,
    accountConfirm: InputState,
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

            InputWithError(
                label = R.string.stripe_manualentry_routing,
                inputState = routing,
                testTag = "RoutingInput",
                imeAction = ImeAction.Next,
                onInputChanged = onRoutingEntered,
            )
            Spacer(modifier = Modifier.size(16.dp))
            InputWithError(
                label = R.string.stripe_manualentry_account,
                inputState = account,
                testTag = "AccountInput",
                imeAction = ImeAction.Next,
                onInputChanged = onAccountEntered,
            )
            Spacer(modifier = Modifier.size(16.dp))
            InputWithError(
                label = R.string.stripe_manualentry_accountconfirm,
                inputState = accountConfirm,
                testTag = "ConfirmAccountInput",
                imeAction = ImeAction.Done,
                onInputChanged = onAccountConfirmEntered,
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
    inputState: InputState,
    label: Int,
    testTag: String,
    imeAction: ImeAction,
    onInputChanged: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var textValue by remember { mutableStateOf(TextFieldValue()) }

    Spacer(modifier = Modifier.size(4.dp))
    FinancialConnectionsOutlinedTextField(
        value = textValue,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = imeAction,
        ),
        keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(Next) },
            onDone = { focusManager.clearFocus() },
        ),
        placeholder = {
            Text(
                text = stringResource(id = label),
                style = FinancialConnectionsTheme.typography.labelLarge,
                color = FinancialConnectionsTheme.colors.textSubdued
            )
        },
        isError = inputState.error != null,
        onValueChange = { text ->
            textValue = inputState.filter(text)
            onInputChanged(textValue.text)
        },
        modifier = Modifier
            .semantics { testTagsAsResourceId = true }
            .testTag(testTag)
    )
    if (inputState.error != null) {
        Spacer(modifier = Modifier.size(4.dp))
        Text(
            text = stringResource(id = inputState.error),
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
            account = state.account,
            accountConfirm = state.accountConfirm,
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
