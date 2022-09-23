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
import com.stripe.android.financialconnections.features.common.LoadingContent
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
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
        onCloseClick = parentViewModel::onCloseClick
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun AttachPaymentContent(
    payload: Async<AttachPaymentState.Payload>,
    onCloseClick: () -> Unit
) {
    FinancialConnectionsScaffold(
        topBar = {
            FinancialConnectionsTopAppBar(
                onCloseClick = onCloseClick,
                showBack = false
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
            is Fail -> UnclassifiedErrorContent()
        }
    }
}

@Composable
@Preview
internal fun AttachPaymentScreenPreview() {
    FinancialConnectionsTheme {
        AttachPaymentContent(
            payload = Uninitialized,
            onCloseClick = {}
        )
    }
}
