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
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.features.institutionpicker.LoadingContent
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

@Composable
internal fun AttachPaymentScreen() {
    val viewModel: AttachPaymentViewModel = mavericksViewModel()
    val state = viewModel.collectAsState()
    BackHandler(enabled = true) {}
    AttachPaymentContent(state.value.payload)
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun AttachPaymentContent(payload: Async<AttachPaymentState.Payload>) {
    FinancialConnectionsScaffold {
        when (payload) {
            Uninitialized, is Loading -> TODO()
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
                },
            )
            is Fail -> UnclassifiedErrorContent()
        }
    }
}

@Composable
@Preview
internal fun AttachPaymentScreenPreview() {
    FinancialConnectionsTheme {
        AttachPaymentContent(Uninitialized)
    }
}
