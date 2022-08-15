package com.stripe.android.financialconnections.features.success

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.features.common.AccessibleDataCalloutModel
import com.stripe.android.financialconnections.features.common.AccessibleDataCalloutWithAccounts
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.financialconnections.model.PartnerAccount
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

@Composable
internal fun SuccessScreen() {
    val viewModel: SuccessViewModel = mavericksViewModel()
    val state = viewModel.collectAsState { it.partnerAccountInfo }
    state.value()?.let { (accessibleDataModel, accounts) ->
        SuccessContent(
            accessibleDataModel = accessibleDataModel,
            accounts = accounts.data
        )
    }
}

@Composable
private fun SuccessContent(
    accessibleDataModel: AccessibleDataCalloutModel,
    accounts: List<PartnerAccount>,
) {
    FinancialConnectionsScaffold {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Icon(
                modifier = Modifier.size(40.dp),
                painter = painterResource(R.drawable.stripe_ic_check),
                contentDescription = null,
                tint = FinancialConnectionsTheme.colors.textSuccess
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth(),
                text = stringResource(R.string.stripe_success_title),
                style = FinancialConnectionsTheme.typography.subtitle
            )
            AccessibleDataCalloutWithAccounts(
                model = accessibleDataModel,
                accounts = accounts
            )
        }
    }
}

@Composable
@Preview
internal fun SuccessScreenPreview() {
    FinancialConnectionsTheme {
        SuccessContent(
            accounts = listOf(
                PartnerAccount(
                    authorization = "Authorization",
                    category = FinancialConnectionsAccount.Category.CASH,
                    id = "id2",
                    name = "Account 2 - no acct numbers",
                    subcategory = FinancialConnectionsAccount.Subcategory.SAVINGS,
                    supportedPaymentMethodTypes = emptyList()
                ),
                PartnerAccount(
                    authorization = "Authorization",
                    category = FinancialConnectionsAccount.Category.CASH,
                    id = "id3",
                    name = "Account 3",
                    displayableAccountNumbers = "1234",
                    subcategory = FinancialConnectionsAccount.Subcategory.CREDIT_CARD,
                    supportedPaymentMethodTypes = emptyList()
                ),
                PartnerAccount(
                    authorization = "Authorization",
                    category = FinancialConnectionsAccount.Category.CASH,
                    id = "id4",
                    name = "Account 4",
                    displayableAccountNumbers = "1234",
                    subcategory = FinancialConnectionsAccount.Subcategory.CHECKING,
                    supportedPaymentMethodTypes = emptyList()
                ),
            ), accessibleDataModel = AccessibleDataCalloutModel(
                businessName = "My business",
                permissions = listOf(
                    FinancialConnectionsAccount.Permissions.PAYMENT_METHOD,
                    FinancialConnectionsAccount.Permissions.BALANCES,
                    FinancialConnectionsAccount.Permissions.OWNERSHIP,
                    FinancialConnectionsAccount.Permissions.TRANSACTIONS
                ),
                isStripeDirect = true,
                dataPolicyUrl = ""
            )
        )
    }
}