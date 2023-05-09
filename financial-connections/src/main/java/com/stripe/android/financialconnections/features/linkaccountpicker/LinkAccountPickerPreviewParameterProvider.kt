@file:Suppress("LongMethod")

package com.stripe.android.financialconnections.features.linkaccountpicker

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.airbnb.mvrx.Success
import com.stripe.android.financialconnections.features.common.AccessibleDataCalloutModel
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount.Status
import com.stripe.android.financialconnections.model.PartnerAccount

internal class LinkAccountPickerPreviewParameterProvider :
    PreviewParameterProvider<LinkAccountPickerState> {
    override val values = sequenceOf(
        canonical(),
        accountSelected(),
        repairableAccountSelected(),
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
                stepUpAuthenticationRequired = false,
                partnerToCoreAuths = emptyMap(),
            )
        ),
    )

    private fun accountSelected() = LinkAccountPickerState(
        selectedAccountId = partnerAccountList().first().id,
        payload = Success(
            LinkAccountPickerState.Payload(
                accounts = partnerAccountList(),
                accessibleData = accessibleCallout(),
                businessName = "Random business",
                consumerSessionClientSecret = "secret",
                stepUpAuthenticationRequired = false,
                partnerToCoreAuths = emptyMap(),
            )
        ),
    )

    private fun repairableAccountSelected() = LinkAccountPickerState(
        selectedAccountId = partnerAccountList()
            .first { it.status != Status.ACTIVE && it.allowSelection }.id,
        payload = Success(
            LinkAccountPickerState.Payload(
                accounts = partnerAccountList(),
                accessibleData = accessibleCallout(),
                businessName = "Random business",
                consumerSessionClientSecret = "secret",
                stepUpAuthenticationRequired = false,
                partnerToCoreAuths = emptyMap(),
            )
        ),
    )

    private fun partnerAccountList() = listOf(
        PartnerAccount(
            authorization = "Authorization",
            category = FinancialConnectionsAccount.Category.CASH,
            id = "id1",
            name = "With balance",
            balanceAmount = 1000,
            status = Status.ACTIVE,
            displayableAccountNumbers = "1234",
            currency = "USD",
            _allowSelection = true,
            allowSelectionMessage = "",
            subcategory = FinancialConnectionsAccount.Subcategory.CHECKING,
            supportedPaymentMethodTypes = emptyList()
        ),
        PartnerAccount(
            authorization = "Authorization",
            category = FinancialConnectionsAccount.Category.CASH,
            id = "id2",
            name = "With balance repairable",
            status = Status.INACTIVE,
            balanceAmount = 1000,
            _allowSelection = true,
            allowSelectionMessage = "Select to repair and connect",
            subcategory = FinancialConnectionsAccount.Subcategory.SAVINGS,
            supportedPaymentMethodTypes = emptyList()
        ),
        PartnerAccount(
            authorization = "Authorization",
            category = FinancialConnectionsAccount.Category.CASH,
            id = "id3",
            name = "Repairable + disabled",
            status = Status.INACTIVE,
            balanceAmount = 1000,
            _allowSelection = false,
            allowSelectionMessage = "Select to repair and connect",
            subcategory = FinancialConnectionsAccount.Subcategory.SAVINGS,
            supportedPaymentMethodTypes = emptyList()
        ),
        PartnerAccount(
            authorization = "Authorization",
            category = FinancialConnectionsAccount.Category.CASH,
            id = "id4",
            name = "No balance",
            displayableAccountNumbers = "1234",
            status = Status.ACTIVE,
            subcategory = FinancialConnectionsAccount.Subcategory.CREDIT_CARD,
            _allowSelection = true,
            allowSelectionMessage = "",
            supportedPaymentMethodTypes = emptyList()
        ),
        PartnerAccount(
            authorization = "Authorization",
            category = FinancialConnectionsAccount.Category.CASH,
            id = "id5",
            name = "Very long account of a very long institution",
            status = Status.ACTIVE,
            displayableAccountNumbers = "1234",
            linkedAccountId = "linkedAccountId",
            _allowSelection = true,
            subcategory = FinancialConnectionsAccount.Subcategory.CHECKING,
            supportedPaymentMethodTypes = emptyList()
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
