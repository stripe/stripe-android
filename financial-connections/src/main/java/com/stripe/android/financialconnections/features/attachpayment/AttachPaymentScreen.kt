package com.stripe.android.financialconnections.features.attachpayment

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.tooling.preview.Preview
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.exception.AccountNumberRetrievalError
import com.stripe.android.financialconnections.features.common.AccountNumberRetrievalErrorContent
import com.stripe.android.financialconnections.features.common.LoadingContent
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.NextPane.ATTACH_LINKED_PAYMENT_ACCOUNT
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

@Composable
internal fun AttachPaymentScreen() {
    val viewModel: AttachPaymentViewModel = mavericksViewModel()
    val parentViewModel = parentViewModel()
    val state = viewModel.collectAsState()
    BackHandler(enabled = true) {}
    AttachPaymentContent(
        payload = state.value.payload,
        onSelectAnotherBank = viewModel::onSelectAnotherBank,
        onEnterDetailsManually = viewModel::onEnterDetailsManually,
        onCloseClick = { parentViewModel.onCloseWithConfirmationClick(ATTACH_LINKED_PAYMENT_ACCOUNT) },
        onCloseFromErrorClick = parentViewModel::onCloseFromErrorClick
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun AttachPaymentContent(
    payload: Async<AttachPaymentState.Payload>,
    onSelectAnotherBank: () -> Unit,
    onEnterDetailsManually: () -> Unit,
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
            Uninitialized, is Loading -> LoadingContent()
            is Success -> LoadingContent(
                title = pluralStringResource(
                    id = R.plurals.stripe_attachlinkedpaymentaccount_title,
                    count = payload().accountsCount
                ),
                content = when (val businessName = payload().businessName) {
                    null -> pluralStringResource(
                        id = R.plurals.stripe_attachlinkedpaymentaccount_desc,
                        count = payload().accountsCount
                    )

                    else -> pluralStringResource(
                        id = R.plurals.stripe_attachlinkedpaymentaccount_desc,
                        count = payload().accountsCount,
                        businessName
                    )
                }
            )

            is Fail -> ErrorContent(
                error = payload.error,
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

        else -> UnclassifiedErrorContent(
            error = error,
            onCloseFromErrorClick = onCloseFromErrorClick
        )
    }
}

@Composable
@Preview
internal fun AttachPaymentScreenPreview() {
    FinancialConnectionsTheme {
        AttachPaymentContent(
            payload = Success(
                AttachPaymentState.Payload(
                    accountsCount = 10,
                    businessName = "Random Business"
                )
            ),
            onSelectAnotherBank = {},
            onEnterDetailsManually = {},
            onCloseClick = {}
        ) {}
    }
}
