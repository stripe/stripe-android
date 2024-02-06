package com.stripe.android.financialconnections.features.error

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.exception.InstitutionPlannedDowntimeError
import com.stripe.android.financialconnections.exception.InstitutionUnplannedDowntimeError
import com.stripe.android.financialconnections.exception.PartnerAuthError
import com.stripe.android.financialconnections.features.common.FullScreenGenericLoading
import com.stripe.android.financialconnections.features.common.InstitutionPlannedDowntimeErrorContent
import com.stripe.android.financialconnections.features.common.InstitutionUnknownErrorContent
import com.stripe.android.financialconnections.features.common.InstitutionUnplannedDowntimeErrorContent
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar

@Composable
internal fun ErrorScreen() {
    val viewModel: ErrorViewModel = mavericksViewModel()
    val parentViewModel = parentViewModel()
    val payload = viewModel.collectAsState { it.payload }
    BackHandler(enabled = true) {}
    ErrorContent(
        payload = payload.value,
        onManualEntryClick = viewModel::onManualEntryClick,
        onSelectBankClick = viewModel::onSelectBankClick,
        onCloseClick = { parentViewModel.onCloseWithConfirmationClick(Pane.UNEXPECTED_ERROR) },
        onCloseFromErrorClick = parentViewModel::onCloseFromErrorClick
    )
}

@Composable
private fun ErrorContent(
    payload: Async<ErrorState.Payload>,
    onSelectBankClick: () -> Unit,
    onManualEntryClick: () -> Unit,
    onCloseClick: () -> Unit,
    onCloseFromErrorClick: (Throwable) -> Unit
) {
    FinancialConnectionsScaffold(
        topBar = {
            FinancialConnectionsTopAppBar(
                showBack = false,
                onCloseClick = onCloseClick
            )
        }
    ) {
        when (payload) {
            Uninitialized,
            is Loading -> FullScreenGenericLoading()
            is Success -> ErrorContent(
                payload().error,
                onSelectAnotherBank = onSelectBankClick,
                onEnterDetailsManually = onManualEntryClick,
                onCloseFromErrorClick = onCloseFromErrorClick
            )
            is Fail -> UnclassifiedErrorContent(
                error = payload.error,
                onCloseFromErrorClick = onCloseFromErrorClick
            )
        }
    }
}

@Composable
private fun ErrorContent(
    error: Throwable,
    onSelectAnotherBank: () -> Unit,
    onEnterDetailsManually: () -> Unit,
    onCloseFromErrorClick: (Throwable) -> Unit
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

        is PartnerAuthError -> InstitutionUnknownErrorContent(
            onSelectAnotherBank = onSelectAnotherBank,
        )
        else -> UnclassifiedErrorContent(error, onCloseFromErrorClick)
    }
}

@Preview(
    group = "Error",
    name = "Default"
)
@Composable
internal fun ErrorScreenPreview() {
    FinancialConnectionsPreview {
        ErrorContent(
            payload = Uninitialized,
            onSelectBankClick = {},
            onManualEntryClick = {},
            onCloseClick = {}
        ) {}
    }
}
