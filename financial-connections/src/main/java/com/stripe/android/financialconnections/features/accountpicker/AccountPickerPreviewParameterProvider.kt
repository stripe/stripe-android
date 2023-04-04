@file:Suppress("LongMethod")

package com.stripe.android.financialconnections.features.accountpicker

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.airbnb.mvrx.Success
import com.stripe.android.financialconnections.features.accountpicker.AccountPickerState.PartnerAccountUI
import com.stripe.android.financialconnections.features.common.AccessibleDataCalloutModel
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.financialconnections.model.PartnerAccount

internal class AccountPickerPreviewParameterProvider : PreviewParameterProvider<AccountPickerState> {
    override val values = sequenceOf(
        multiSelect(),
        singleSelect(),
        singleSelectWithConfirm(),

    )

    override val count: Int
        get() = super.count

    private fun multiSelect() = AccountPickerState(
        payload = Success(
            AccountPickerState.Payload(
                skipAccountSelection = false,
                accounts = partnerAccountList(),
                selectionMode = AccountPickerState.SelectionMode.CHECKBOXES,
                accessibleData = accessibleCallout(),
                singleAccount = false,
                stripeDirect = false,
                businessName = "Random business",
                userSelectedSingleAccountInInstitution = false,
                requiresSingleAccountConfirmation = false
            )
        ),
        selectedIds = setOf("id1"),
    )

    private fun singleSelect() = AccountPickerState(
        payload = Success(
            AccountPickerState.Payload(
                skipAccountSelection = false,
                accounts = partnerAccountList(),
                selectionMode = AccountPickerState.SelectionMode.RADIO,
                accessibleData = accessibleCallout(),
                singleAccount = true,
                stripeDirect = false,
                businessName = "Random business",
                userSelectedSingleAccountInInstitution = false,
                requiresSingleAccountConfirmation = false
            )
        ),
        selectedIds = setOf("id1"),
    )

    private fun singleSelectWithConfirm() = AccountPickerState(
        payload = Success(
            AccountPickerState.Payload(
                skipAccountSelection = false,
                accounts = partnerAccountList(),
                selectionMode = AccountPickerState.SelectionMode.RADIO,
                accessibleData = accessibleCallout(),
                singleAccount = true,
                stripeDirect = false,
                businessName = "Random business",
                userSelectedSingleAccountInInstitution = false,
                requiresSingleAccountConfirmation = true
            )
        ),
        selectedIds = setOf("id1"),
    )

    private fun partnerAccountList() = listOf(
        PartnerAccountUI(
            PartnerAccount(
                authorization = "Authorization",
                category = FinancialConnectionsAccount.Category.CASH,
                id = "id1",
                name = "With balance",
                balanceAmount = 1000,
                displayableAccountNumbers = "1234",
                currency = "$",
                _allowSelection = true,
                allowSelectionMessage = "",
                subcategory = FinancialConnectionsAccount.Subcategory.CHECKING,
                supportedPaymentMethodTypes = emptyList()
            ),
            institutionIcon = null,
            formattedBalance = "$1,000"
        ),
        PartnerAccountUI(
            PartnerAccount(
                authorization = "Authorization",
                category = FinancialConnectionsAccount.Category.CASH,
                id = "id2",
                name = "With balance disabled",
                balanceAmount = 1000,
                _allowSelection = true,
                allowSelectionMessage = "",
                subcategory = FinancialConnectionsAccount.Subcategory.SAVINGS,
                supportedPaymentMethodTypes = emptyList()
            ),
            institutionIcon = null,
            formattedBalance = "$1,000"
        ),
        PartnerAccountUI(
            PartnerAccount(
                authorization = "Authorization",
                category = FinancialConnectionsAccount.Category.CASH,
                id = "id3",
                name = "No balance",
                displayableAccountNumbers = "1234",
                subcategory = FinancialConnectionsAccount.Subcategory.CREDIT_CARD,
                _allowSelection = false,
                allowSelectionMessage = "Cannot be selected",
                supportedPaymentMethodTypes = emptyList()
            ),
            institutionIcon = null,
            formattedBalance = null
        ),
        PartnerAccountUI(
            PartnerAccount(
                authorization = "Authorization",
                category = FinancialConnectionsAccount.Category.CASH,
                id = "id4",
                name = "No balance disabled",
                displayableAccountNumbers = "1234",
                subcategory = FinancialConnectionsAccount.Subcategory.CHECKING,
                _allowSelection = false,
                allowSelectionMessage = "Cannot be selected",
                supportedPaymentMethodTypes = emptyList()
            ),
            institutionIcon = null,
            formattedBalance = null
        ),
        PartnerAccountUI(
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
            institutionIcon = null,
            formattedBalance = null
        )
    )

    private fun accessibleCallout() = AccessibleDataCalloutModel(
        businessName = "My business",
        permissions = listOf(
            FinancialConnectionsAccount.Permissions.PAYMENT_METHOD,
            FinancialConnectionsAccount.Permissions.BALANCES,
            FinancialConnectionsAccount.Permissions.OWNERSHIP,
            FinancialConnectionsAccount.Permissions.TRANSACTIONS
        ),
        isStripeDirect = false,
        isNetworking = false,
        dataPolicyUrl = ""
    )
}
