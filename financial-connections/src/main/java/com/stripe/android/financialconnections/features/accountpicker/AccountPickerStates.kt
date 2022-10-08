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
        dropdown(emptySet())
    )

    override val count: Int
        get() = super.count

    // TODO@carlosmuvi migrate to PreviewParameterProvider when showkase adds support.
    companion object {
        fun multiSelect() = AccountPickerState(
            payload = Success(
                AccountPickerState.Payload(
                    skipAccountSelection = false,
                    accounts = partnerAccountList(),
                    selectionMode = AccountPickerState.SelectionMode.CHECKBOXES,
                    accessibleData = accessibleCallout(),
                    selectedIds = setOf("id1"),
                    singleAccount = false,
                    institutionSkipAccountSelection = false,
                    businessName = "Random business",
                    stripeDirect = false,
                )
            )
        )

        fun singleSelect() = AccountPickerState(
            payload = Success(
                AccountPickerState.Payload(
                    skipAccountSelection = false,
                    accounts = partnerAccountList(),
                    selectionMode = AccountPickerState.SelectionMode.RADIO,
                    accessibleData = accessibleCallout(),
                    selectedIds = setOf("id1"),
                    singleAccount = false,
                    institutionSkipAccountSelection = false,
                    businessName = "Random business",
                    stripeDirect = false,
                )
            )
        )

        fun dropdown(selectedIds: Set<String>) = AccountPickerState(
            payload = Success(
                AccountPickerState.Payload(
                    skipAccountSelection = false,
                    accounts = partnerAccountList(),
                    selectionMode = AccountPickerState.SelectionMode.DROPDOWN,
                    accessibleData = accessibleCallout(),
                    selectedIds = selectedIds,
                    singleAccount = true,
                    institutionSkipAccountSelection = true,
                    businessName = "Random business",
                    stripeDirect = true,
                )
            )
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
                    subcategory = FinancialConnectionsAccount.Subcategory.CHECKING,
                    supportedPaymentMethodTypes = emptyList()
                ),
                enabled = true,
                formattedBalance = "$1,000"
            ),
            PartnerAccountUI(
                PartnerAccount(
                    authorization = "Authorization",
                    category = FinancialConnectionsAccount.Category.CASH,
                    id = "id2",
                    name = "With balance disabled",
                    balanceAmount = 1000,
                    subcategory = FinancialConnectionsAccount.Subcategory.SAVINGS,
                    supportedPaymentMethodTypes = emptyList()
                ),
                enabled = false,
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
                    supportedPaymentMethodTypes = emptyList()
                ),
                enabled = true,
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
                    supportedPaymentMethodTypes = emptyList()
                ),
                enabled = false,
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
            isStripeDirect = true,
            dataPolicyUrl = ""
        )
    }
}
