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
import com.stripe.android.financialconnections.exception.FinancialConnectionsError
import com.stripe.android.financialconnections.exception.InstitutionPlannedDowntimeError
import com.stripe.android.financialconnections.exception.InstitutionUnplannedDowntimeError
import com.stripe.android.financialconnections.features.common.FullScreenGenericLoading
import com.stripe.android.financialconnections.features.common.InstitutionPlannedDowntimeErrorContent
import com.stripe.android.financialconnections.features.common.InstitutionUnplannedDowntimeErrorContent
import com.stripe.android.financialconnections.features.common.NoAccountsAvailableErrorContent
import com.stripe.android.financialconnections.features.common.NoSupportedPaymentMethodTypeAccountsErrorContent
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview

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
        onCloseFromErrorClick = parentViewModel::onCloseFromErrorClick
    )
}

@Composable
private fun ErrorContent(
    payload: Async<ErrorState.Payload>,
    onSelectBankClick: () -> Unit,
    onManualEntryClick: () -> Unit,
    onCloseFromErrorClick: (Throwable) -> Unit
) {
    when (payload) {
        Uninitialized,
        is Loading -> FullScreenGenericLoading()

        // Render error successfully retrieved from a previous pane
        is Success -> ErrorContent(
            payload().error,
            allowManualEntry = payload().allowManualEntry,
            onSelectAnotherBank = onSelectBankClick,
            onEnterDetailsManually = onManualEntryClick,
            onCloseFromErrorClick = onCloseFromErrorClick,
            onTryAgain = {},
        )

        // Something wrong happened while trying to retrieve the error, render the unclassified error
        is Fail -> ErrorContent(
            payload.error,
            allowManualEntry = false,
            onSelectAnotherBank = onSelectBankClick,
            onEnterDetailsManually = onManualEntryClick,
            onCloseFromErrorClick = onCloseFromErrorClick,
            onTryAgain = {},
        )
    }
}

@Composable
internal fun ErrorContent(
    error: Throwable,
    allowManualEntry: Boolean = (error as? FinancialConnectionsError)?.allowManualEntry ?: false,
    onSelectAnotherBank: () -> Unit,
    onEnterDetailsManually: () -> Unit,
    onTryAgain: () -> Unit,
    onCloseFromErrorClick: (Throwable) -> Unit,
) {
    when (error) {
        is InstitutionPlannedDowntimeError -> InstitutionPlannedDowntimeErrorContent(
            exception = error,
            onSelectAnotherBank = onSelectAnotherBank,
            onEnterDetailsManually = onEnterDetailsManually
        )

        is InstitutionUnplannedDowntimeError -> InstitutionUnplannedDowntimeErrorContent(
            exception = error,
            onSelectAnotherBank = onSelectAnotherBank,
            onEnterDetailsManually = onEnterDetailsManually
        )

        is AccountNoneEligibleForPaymentMethodError ->
            NoSupportedPaymentMethodTypeAccountsErrorContent(
                exception = error,
                onSelectAnotherBank = onSelectAnotherBank
            )

        is AccountLoadError -> NoAccountsAvailableErrorContent(
            exception = error,
            onEnterDetailsManually = onEnterDetailsManually,
            onTryAgain = onTryAgain,
            onSelectAnotherBank = onSelectAnotherBank
        )

        else -> UnclassifiedErrorContent(
            allowManualEntry = allowManualEntry,
        ) {
            if (allowManualEntry) {
                onEnterDetailsManually()
            } else {
                onCloseFromErrorClick(error)
            }
        }
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
            onCloseFromErrorClick = {}
        )
    }
}
