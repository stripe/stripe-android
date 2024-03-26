package com.stripe.android.financialconnections.features.error

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.stripe.android.financialconnections.core.Async
import com.stripe.android.financialconnections.core.Async.Fail
import com.stripe.android.financialconnections.core.Async.Loading
import com.stripe.android.financialconnections.core.Async.Success
import com.stripe.android.financialconnections.core.Async.Uninitialized
import com.stripe.android.financialconnections.core.paneViewModel
import com.stripe.android.financialconnections.exception.InstitutionPlannedDowntimeError
import com.stripe.android.financialconnections.exception.InstitutionUnplannedDowntimeError
import com.stripe.android.financialconnections.exception.PartnerAuthError
import com.stripe.android.financialconnections.features.common.FullScreenGenericLoading
import com.stripe.android.financialconnections.features.common.InstitutionPlannedDowntimeErrorContent
import com.stripe.android.financialconnections.features.common.InstitutionUnknownErrorContent
import com.stripe.android.financialconnections.features.common.InstitutionUnplannedDowntimeErrorContent
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar

@Composable
internal fun ErrorScreen() {
    val viewModel: ErrorViewModel = paneViewModel(ErrorViewModel.Companion::factory)
    val parentViewModel = parentViewModel()
    BackHandler(true) { }
    val state by viewModel.stateFlow.collectAsState()
    ErrorContent(
        payload = state.payload,
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
        is Loading -> FullScreenError(
            showBack = false,
            onCloseClick = { },
            content = { FullScreenGenericLoading() }
        )

        // Render error successfully retrieved from a previous pane
        is Success -> ErrorContent(
            payload().error,
            allowManualEntry = payload().allowManualEntry,
            onSelectAnotherBank = onSelectBankClick,
            onEnterDetailsManually = onManualEntryClick,
            onCloseFromErrorClick = onCloseFromErrorClick
        )

        // Something wrong happened while trying to retrieve the error, render the unclassified error
        is Fail -> ErrorContent(
            payload.error,
            allowManualEntry = false,
            onSelectAnotherBank = onSelectBankClick,
            onEnterDetailsManually = onManualEntryClick,
            onCloseFromErrorClick = onCloseFromErrorClick
        )
    }
}

@Composable
private fun ErrorContent(
    error: Throwable,
    allowManualEntry: Boolean,
    onSelectAnotherBank: () -> Unit,
    onEnterDetailsManually: () -> Unit,
    onCloseFromErrorClick: (Throwable) -> Unit,
) {
    when (error) {
        is InstitutionPlannedDowntimeError -> FullScreenError(
            showBack = false,
            onCloseClick = { onCloseFromErrorClick(error) },
            content = {
                InstitutionPlannedDowntimeErrorContent(
                    exception = error,
                    onSelectAnotherBank = onSelectAnotherBank,
                    onEnterDetailsManually = onEnterDetailsManually
                )
            }
        )

        is InstitutionUnplannedDowntimeError -> FullScreenError(
            showBack = false,
            onCloseClick = { onCloseFromErrorClick(error) },
            content = {
                InstitutionUnplannedDowntimeErrorContent(
                    exception = error,
                    onSelectAnotherBank = onSelectAnotherBank,
                    onEnterDetailsManually = onEnterDetailsManually
                )
            }
        )

        is PartnerAuthError -> FullScreenError(
            showBack = false,
            onCloseClick = { onCloseFromErrorClick(error) },
            content = {
                InstitutionUnknownErrorContent(
                    onSelectAnotherBank = onSelectAnotherBank,
                )
            }
        )

        else -> FullScreenError(
            showBack = false,
            onCloseClick = { onCloseFromErrorClick(error) },
            content = {
                UnclassifiedErrorContent(
                    allowManualEntry = allowManualEntry,
                ) {
                    if (allowManualEntry) {
                        onEnterDetailsManually()
                    } else {
                        onCloseFromErrorClick(error)
                    }
                }
            }
        )
    }
}

@Composable
private fun FullScreenError(
    showBack: Boolean,
    onCloseClick: () -> Unit,
    content: @Composable () -> Unit
) {
    FinancialConnectionsScaffold(
        topBar = {
            FinancialConnectionsTopAppBar(
                allowBackNavigation = showBack,
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
            onCloseFromErrorClick = {}
        )
    }
}
