package com.stripe.android.financialconnections.features.attachpayment

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.tooling.preview.Preview
import com.stripe.android.financialconnections.core.Async
import com.stripe.android.financialconnections.core.Async.Fail
import com.stripe.android.financialconnections.core.Async.Loading
import com.stripe.android.financialconnections.core.Async.Success
import com.stripe.android.financialconnections.core.Async.Uninitialized
import com.stripe.android.financialconnections.core.paneViewModel
import com.stripe.android.financialconnections.exception.AccountNumberRetrievalError
import com.stripe.android.financialconnections.features.common.AccountNumberRetrievalErrorContent
import com.stripe.android.financialconnections.features.common.FullScreenGenericLoading
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane.ATTACH_LINKED_PAYMENT_ACCOUNT
import com.stripe.android.financialconnections.model.LinkAccountSessionPaymentAccount
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar

@Composable
internal fun AttachPaymentScreen() {
    val viewModel: AttachPaymentViewModel = paneViewModel { AttachPaymentViewModel.factory(it) }
    val parentViewModel = parentViewModel()
    val state = viewModel.stateFlow.collectAsState()
    BackHandler(enabled = true) {}
    AttachPaymentContent(
        attachPayment = state.value.linkPaymentAccount,
        onSelectAnotherBank = viewModel::onSelectAnotherBank,
        onEnterDetailsManually = viewModel::onEnterDetailsManually,
        onCloseClick = { parentViewModel.onCloseWithConfirmationClick(ATTACH_LINKED_PAYMENT_ACCOUNT) },
        onCloseFromErrorClick = parentViewModel::onCloseFromErrorClick
    )
}

@Composable
private fun AttachPaymentContent(
    attachPayment: Async<LinkAccountSessionPaymentAccount>,
    onSelectAnotherBank: () -> Unit,
    onEnterDetailsManually: () -> Unit,
    onCloseClick: () -> Unit,
    onCloseFromErrorClick: (Throwable) -> Unit
) {
    FinancialConnectionsScaffold(
        topBar = {
            FinancialConnectionsTopAppBar(
                allowBackNavigation = false,
                onCloseClick = onCloseClick
            )
        }
    ) {
        when (attachPayment) {
            is Loading,
            is Uninitialized,
            is Success -> FullScreenGenericLoading()

            is Fail -> ErrorContent(
                error = attachPayment.error,
                onSelectAnotherBank = onSelectAnotherBank,
                onEnterDetailsManually = onEnterDetailsManually,
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
        is AccountNumberRetrievalError -> AccountNumberRetrievalErrorContent(
            exception = error,
            onSelectAnotherBank = onSelectAnotherBank,
            onEnterDetailsManually = onEnterDetailsManually
        )

        else -> UnclassifiedErrorContent { onCloseFromErrorClick(error) }
    }
}

@Preview(
    showBackground = true,
    group = "Attach Payment Pane",
    name = "Default"
)
@Composable
internal fun AttachPaymentScreenPreview() {
    FinancialConnectionsPreview {
        AttachPaymentContent(
            attachPayment = Loading(),
            onSelectAnotherBank = {},
            onEnterDetailsManually = {},
            onCloseClick = {}
        ) {}
    }
}
