package com.stripe.android.financialconnections.features.error

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.exception.AccountLoadError
import com.stripe.android.financialconnections.exception.AccountNoneEligibleForPaymentMethodError
import com.stripe.android.financialconnections.exception.InstitutionPlannedDowntimeError
import com.stripe.android.financialconnections.exception.InstitutionUnplannedDowntimeError
import com.stripe.android.financialconnections.exception.PartnerAuthError
import com.stripe.android.financialconnections.features.common.FullScreenGenericLoading
import com.stripe.android.financialconnections.features.common.InstitutionPlannedDowntimeErrorContent
import com.stripe.android.financialconnections.features.common.InstitutionUnknownErrorContent
import com.stripe.android.financialconnections.features.common.InstitutionUnplannedDowntimeErrorContent
import com.stripe.android.financialconnections.features.common.NoAccountsAvailableErrorContent
import com.stripe.android.financialconnections.features.common.NoSupportedPaymentMethodTypeAccountsErrorContent
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar

@Composable
internal fun ErrorScreen() {
    val viewModel: ErrorViewModel = mavericksViewModel()
    val parentViewModel = parentViewModel()
    BackHandler(true) { }
    val payload = viewModel.collectAsState { it.payload }
    ErrorContent(
        payload = payload.value,
        onManualEntryClick = viewModel::onManualEntryClick,
        onSelectBankClick = viewModel::onSelectAnotherBank,
        onTryAgain = viewModel::onTryAgain,
        onCloseFromErrorClick = parentViewModel::onCloseFromErrorClick
    )
}

@Composable
private fun ErrorContent(
    payload: Async<ErrorState.Payload>,
    onSelectBankClick: () -> Unit,
    onManualEntryClick: () -> Unit,
    onTryAgain: () -> Unit,
    onCloseFromErrorClick: (Throwable) -> Unit
) {
    when (payload) {
        Uninitialized,
        is Loading -> FullScreenError(
            onCloseClick = { },
            content = { FullScreenGenericLoading() }
        )

        // Render error successfully retrieved from a previous pane
        is Success -> ErrorContent(
            error = payload().error,
            allowManualEntry = payload().allowManualEntry,
            allowRetry = payload().allowRetry,
            onSelectAnotherBank = onSelectBankClick,
            onEnterDetailsManually = onManualEntryClick,
            onTryAgain = onTryAgain,
            onCloseFromErrorClick = onCloseFromErrorClick,
        )

        // Something wrong happened while trying to retrieve the error, render the unclassified error
        is Fail -> ErrorContent(
            error = payload.error,
            allowManualEntry = false,
            allowRetry = false,
            onSelectAnotherBank = onSelectBankClick,
            onEnterDetailsManually = onManualEntryClick,
            onTryAgain = onTryAgain,
            onCloseFromErrorClick = onCloseFromErrorClick,
        )
    }
}

@Composable
private fun ErrorContent(
    error: Throwable,
    allowManualEntry: Boolean,
    allowRetry: Boolean,
    onSelectAnotherBank: () -> Unit,
    onEnterDetailsManually: () -> Unit,
    onTryAgain: () -> Unit,
    onCloseFromErrorClick: (Throwable) -> Unit,
) {
    FullScreenError(
        onCloseClick = { onCloseFromErrorClick(error) },
    ) {
        when (error) {
            is InstitutionPlannedDowntimeError ->
                InstitutionPlannedDowntimeErrorContent(
                    exception = error,
                    onSelectAnotherBank = onSelectAnotherBank,
                    onEnterDetailsManually = onEnterDetailsManually
                )

            is InstitutionUnplannedDowntimeError ->
                InstitutionUnplannedDowntimeErrorContent(
                    exception = error,
                    onSelectAnotherBank = onSelectAnotherBank,
                    onEnterDetailsManually = onEnterDetailsManually
                )

            is PartnerAuthError -> InstitutionUnknownErrorContent(
                onSelectAnotherBank = onSelectAnotherBank,
            )

            is AccountNoneEligibleForPaymentMethodError ->
                NoSupportedPaymentMethodTypeAccountsErrorContent(
                    exception = error,
                    onSelectAnotherBank = onSelectAnotherBank,
                )

            is AccountLoadError -> NoAccountsAvailableErrorContent(
                exception = error,
                onEnterDetailsManually = onEnterDetailsManually,
                onTryAgain = onTryAgain,
                onSelectAnotherBank = onSelectAnotherBank,
            )

            else -> UnclassifiedErrorContent(
                allowManualEntry = allowManualEntry,
                onCtaClick = {
                    if (allowManualEntry) {
                        onEnterDetailsManually()
                    } else {
                        onCloseFromErrorClick(error)
                    }
                }
            )
        }
    }
}

@Composable
private fun FullScreenError(
    onCloseClick: () -> Unit,
    content: @Composable () -> Unit
) {
    FinancialConnectionsScaffold(
        topBar = {
            FinancialConnectionsTopAppBar(
                allowBackNavigation = false,
                onCloseClick = onCloseClick
            )
        }
    ) {
        content()
    }
}

@Preview
@Composable
internal fun ErrorScreenPreview(
    @PreviewParameter(ErrorPreviewParameterProvider::class) state: ErrorState
) {
    FinancialConnectionsPreview {
        ErrorContent(
            payload = state.payload,
            onSelectBankClick = {},
            onManualEntryClick = {},
            onTryAgain = {},
            onCloseFromErrorClick = {},
        )
    }
}
