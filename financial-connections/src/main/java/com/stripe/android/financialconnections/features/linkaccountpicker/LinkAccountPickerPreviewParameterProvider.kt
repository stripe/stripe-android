@file:Suppress("LongMethod")

package com.stripe.android.financialconnections.features.linkaccountpicker

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.airbnb.mvrx.Success
import com.stripe.android.financialconnections.features.common.AccessibleDataCalloutModel
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.financialconnections.model.PartnerAccount

internal class LinkAccountPickerPreviewParameterProvider :
    PreviewParameterProvider<LinkAccountPickerState> {
    override val values = sequenceOf(
        canonical(),
        accountSelected()
    )

    override val count: Int
        get() = super.count

    private fun canonical() = LinkAccountPickerState(
        payload = Success(
            LinkAccountPickerState.Payload(
                accounts = partnerAccountList(),
                accessibleData = accessibleCallout(),
                businessName = "Random business",
                consumerSessionClientSecret = "secret",
                repairAuthorizationEnabled = false,
                stepUpAuthenticationRequired = false,
            )
        ),
    )

    private fun accountSelected() = LinkAccountPickerState(
        selectedAccountId = partnerAccountList().first().account.id,
        payload = Success(
            LinkAccountPickerState.Payload(
                accounts = partnerAccountList(),
                accessibleData = accessibleCallout(),
                businessName = "Random business",
                consumerSessionClientSecret = "secret",
                repairAuthorizationEnabled = false,
                stepUpAuthenticationRequired = false,
            )
        ),
    )

    private fun partnerAccountList() = listOf(

        LinkAccountPickerState.NetworkedAccount(
            PartnerAccount(
                authorization = "Authorization",
                category = FinancialConnectionsAccount.Category.CASH,
                id = "id1",
                name = "With balance",
                balanceAmount = 1000,
                status = FinancialConnectionsAccount.Status.ACTIVE,
                displayableAccountNumbers = "1234",
                currency = "USD",
                _allowSelection = true,
                allowSelectionMessage = "",
                subcategory = FinancialConnectionsAccount.Subcategory.CHECKING,
                supportedPaymentMethodTypes = emptyList()
            ),
            repairable = true
        ),
        LinkAccountPickerState.NetworkedAccount(
            PartnerAccount(
                authorization = "Authorization",
                category = FinancialConnectionsAccount.Category.CASH,
                id = "id2",
                name = "With balance disabled",
                balanceAmount = 1000,
                _allowSelection = false,
                allowSelectionMessage = "Disabled",
                subcategory = FinancialConnectionsAccount.Subcategory.SAVINGS,
                supportedPaymentMethodTypes = emptyList()
            ),
            repairable = false
        ),
        LinkAccountPickerState.NetworkedAccount(
            PartnerAccount(
                authorization = "Authorization",
                category = FinancialConnectionsAccount.Category.CASH,
                id = "id2",
                name = "With balance repairable",
                balanceAmount = 1000,
                _allowSelection = true,
                allowSelectionMessage = "Select to repair and connect",
                subcategory = FinancialConnectionsAccount.Subcategory.SAVINGS,
                supportedPaymentMethodTypes = emptyList()
            ),
            repairable = true
        ),
        LinkAccountPickerState.NetworkedAccount(
            PartnerAccount(
                authorization = "Authorization",
                category = FinancialConnectionsAccount.Category.CASH,
                id = "id3",
                name = "No balance",
                displayableAccountNumbers = "1234",
                subcategory = FinancialConnectionsAccount.Subcategory.CREDIT_CARD,
                _allowSelection = true,
                allowSelectionMessage = "",
                supportedPaymentMethodTypes = emptyList()
            ),
            repairable = false
        ),
        LinkAccountPickerState.NetworkedAccount(
            PartnerAccount(
                authorization = "Authorization",
                category = FinancialConnectionsAccount.Category.CASH,
                id = "id5",
                name = "Very long institution that is already linked",
                displayableAccountNumbers = "1234",
                linkedAccountId = "linkedAccountId",
                _allowSelection = true,
                subcategory = FinancialConnectionsAccount.Subcategory.CHECKING,
                supportedPaymentMethodTypes = emptyList()
            ),
            repairable = true
        ),
    )

    private fun accessibleCallout() = AccessibleDataCalloutModel(
        businessName = "My business",
        permissions = listOf(
            FinancialConnectionsAccount.Permissions.PAYMENT_METHOD,
            FinancialConnectionsAccount.Permissions.BALANCES,
            FinancialConnectionsAccount.Permissions.OWNERSHIP,
            FinancialConnectionsAccount.Permissions.TRANSACTIONS
        ),
        isStripeDirect = true,
        isNetworking = true,
        dataPolicyUrl = ""
    )
}
