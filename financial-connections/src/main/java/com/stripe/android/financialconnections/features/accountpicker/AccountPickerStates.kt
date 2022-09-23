package com.stripe.android.financialconnections.features.accountpicker

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.airbnb.mvrx.Success
import com.stripe.android.financialconnections.features.accountpicker.AccountPickerState.PartnerAccountUI
import com.stripe.android.financialconnections.features.common.AccessibleDataCalloutModel
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.financialconnections.model.PartnerAccount

internal class AccountPickerStates : PreviewParameterProvider<AccountPickerState> {
    override val values = sequenceOf(
        multiSelect(),
        singleSelect(),
        dropdown()
    )

    override val count: Int
        get() = super.count

    // TODO@carlosmuvi migrate to PreviewParameterProvider when showkase adds support.
    companion object {
        fun multiSelect() = AccountPickerState(
            payload = Success(
                AccountPickerState.Payload(
                    selectionMode = AccountPickerState.SelectionMode.CHECKBOXES,
                    accounts = partnerAccountList(),
                    accessibleData = accessibleCallout(),
                    selectedIds = setOf("id1"),
                    skipAccountSelection = false
                )
            )
        )

        fun singleSelect() = AccountPickerState(
            payload = Success(
                AccountPickerState.Payload(
                    selectionMode = AccountPickerState.SelectionMode.RADIO,
                    accounts = partnerAccountList(),
                    accessibleData = accessibleCallout(),
                    selectedIds = setOf("id1"),
                    skipAccountSelection = false
                )
            )
        )

        fun dropdown() = AccountPickerState(
            payload = Success(
                AccountPickerState.Payload(
                    selectionMode = AccountPickerState.SelectionMode.DROPDOWN,
                    accounts = partnerAccountList(),
                    accessibleData = accessibleCallout(),
                    selectedIds = setOf("id1"),
                    skipAccountSelection = false
                )
            )
        )

        private fun partnerAccountList() = listOf(
            PartnerAccountUI(
                PartnerAccount(
                    authorization = "Authorization",
                    category = FinancialConnectionsAccount.Category.CASH,
                    id = "id1",
                    name = "Account 1",
                    balanceAmount = 1000,
                    displayableAccountNumbers = "1234",
                    currency = "$",
                    subcategory = FinancialConnectionsAccount.Subcategory.CHECKING,
                    supportedPaymentMethodTypes = emptyList()
                ),
                enabled = true
            ),
            PartnerAccountUI(
                PartnerAccount(
                    authorization = "Authorization",
                    category = FinancialConnectionsAccount.Category.CASH,
                    id = "id2",
                    name = "Account 2 - no acct numbers",
                    subcategory = FinancialConnectionsAccount.Subcategory.SAVINGS,
                    supportedPaymentMethodTypes = emptyList()
                ),
                enabled = true
            ),
            PartnerAccountUI(
                PartnerAccount(
                    authorization = "Authorization",
                    category = FinancialConnectionsAccount.Category.CASH,
                    id = "id3",
                    name = "Account 3",
                    displayableAccountNumbers = "1234",
                    subcategory = FinancialConnectionsAccount.Subcategory.CREDIT_CARD,
                    supportedPaymentMethodTypes = emptyList()
                ),
                enabled = false
            ),
            PartnerAccountUI(
                PartnerAccount(
                    authorization = "Authorization",
                    category = FinancialConnectionsAccount.Category.CASH,
                    id = "id4",
                    name = "Account 4",
                    displayableAccountNumbers = "1234",
                    subcategory = FinancialConnectionsAccount.Subcategory.CHECKING,
                    supportedPaymentMethodTypes = emptyList()
                ),
                enabled = false
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
            isStripeDirect = true,
            dataPolicyUrl = ""
        )
    }
}
