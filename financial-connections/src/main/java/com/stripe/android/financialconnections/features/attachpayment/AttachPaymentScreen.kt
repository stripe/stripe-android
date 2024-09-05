package com.stripe.android.financialconnections.features.attachpayment

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.stripe.android.financialconnections.exception.AccountNumberRetrievalError
import com.stripe.android.financialconnections.features.common.AccountNumberRetrievalErrorContent
import com.stripe.android.financialconnections.features.common.FullScreenGenericLoading
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.model.LinkAccountSessionPaymentAccount
import com.stripe.android.financialconnections.presentation.Async
import com.stripe.android.financialconnections.presentation.Async.Fail
import com.stripe.android.financialconnections.presentation.Async.Loading
import com.stripe.android.financialconnections.presentation.Async.Success
import com.stripe.android.financialconnections.presentation.Async.Uninitialized
import com.stripe.android.financialconnections.presentation.paneViewModel
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.uicore.utils.collectAsState

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
        onCloseFromErrorClick = parentViewModel::onCloseFromErrorClick
    )
}

@Composable
private fun AttachPaymentContent(
    attachPayment: Async<LinkAccountSessionPaymentAccount>,
    onSelectAnotherBank: () -> Unit,
    onEnterDetailsManually: () -> Unit,
    onCloseFromErrorClick: (Throwable) -> Unit
) {
    Box {
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
            onCloseFromErrorClick = {},
        )
    }
}
