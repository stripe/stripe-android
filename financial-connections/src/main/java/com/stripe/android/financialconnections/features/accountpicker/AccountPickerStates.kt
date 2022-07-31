package com.stripe.android.financialconnections.features.accountpicker

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.airbnb.mvrx.Success
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.PartnerAccount
import com.stripe.android.financialconnections.model.PartnerAccountsList

internal class AccountPickerStates : PreviewParameterProvider<AccountPickerState> {
    override val values = sequenceOf(
        multiSelect(),
        singleSelect(),
        dropdown(),
    )

    override val count: Int
        get() = super.count

    // TODO@carlosmuvi migrate to PreviewParameterProvider when showkase adds support.
    companion object {
        fun multiSelect() = AccountPickerState(
            selectedIds = setOf("id1"),
            selectionMode = AccountPickerState.SelectionMode.CHECKBOXES,
            accounts = Success(
                PartnerAccountsList(
                    data = partnerAccountList(),
                    hasMore = false,
                    nextPane = FinancialConnectionsSessionManifest.NextPane.ACCOUNT_PICKER,
                    url = ""
                )
            )
        )

        fun singleSelect() = AccountPickerState(
            selectedIds = setOf("id1"),
            selectionMode = AccountPickerState.SelectionMode.RADIO,
            accounts = Success(
                PartnerAccountsList(
                    data = partnerAccountList(),
                    hasMore = false,
                    nextPane = FinancialConnectionsSessionManifest.NextPane.ACCOUNT_PICKER,
                    url = ""
                )
            )
        )

        fun dropdown() = AccountPickerState(
            selectedIds = setOf("id1"),
            selectionMode = AccountPickerState.SelectionMode.DROPDOWN,
            accounts = Success(
                PartnerAccountsList(
                    data = partnerAccountList(),
                    hasMore = false,
                    nextPane = FinancialConnectionsSessionManifest.NextPane.ACCOUNT_PICKER,
                    url = ""
                )
            )
        )

        private fun partnerAccountList() = listOf(
            PartnerAccount(
                authorization = "Authorization",
                category = FinancialConnectionsAccount.Category.CASH,
                id = "id1",
                name = "Account 1",
                balanceAmount = 1000,
                displayableAccountNumbers = "1234",
                currency = "$",
                subcategory = FinancialConnectionsAccount.Subcategory.CHECKING,
                supportedPaymentMethodTypes = emptyList(),
            ),
            PartnerAccount(
                authorization = "Authorization",
                category = FinancialConnectionsAccount.Category.CASH,
                id = "id2",
                name = "Account 2",
                subcategory = FinancialConnectionsAccount.Subcategory.CHECKING,
                supportedPaymentMethodTypes = emptyList()
            )
        )
    }
}
