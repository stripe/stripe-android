package com.stripe.android.financialconnections.features.success

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.model.PartnerAccountsList
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

@Composable
internal fun SuccessScreen() {
    val viewModel: SuccessViewModel = mavericksViewModel()
    val state = viewModel.collectAsState { it.partnerAccountsList }
    SuccessContent(accounts = state.value)
}

@Composable
private fun SuccessContent(
    accounts: PartnerAccountsList,
) {
    FinancialConnectionsScaffold {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth(),
                text = stringResource(R.string.stripe_success_title),
                style = FinancialConnectionsTheme.typography.subtitle
            )
            accounts.data.forEach { account ->
                Text(text = "${account.name}${account.displayableAccountNumbers?.let { " ****$it" }}")
            }
        }
    }
}
