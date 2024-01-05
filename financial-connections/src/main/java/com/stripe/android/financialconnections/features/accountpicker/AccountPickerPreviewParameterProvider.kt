@file:Suppress("LongMethod")

package com.stripe.android.financialconnections.features.accountpicker

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.stripe.android.core.exception.APIException
import com.stripe.android.financialconnections.exception.AccountNoneEligibleForPaymentMethodError
import com.stripe.android.financialconnections.features.common.MerchantDataAccessModel
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.PartnerAccount

internal class AccountPickerPreviewParameterProvider :
    PreviewParameterProvider<AccountPickerState> {
    override val values = sequenceOf(
        loading(),
        error(),
        multiSelect(),
        singleSelect(),
    )

    override val count: Int
        get() = super.count

    private fun loading() = AccountPickerState(
        payload = Loading(),
        selectedIds = emptySet(),
    )

    private fun error() = AccountPickerState(
        payload = Fail(
            AccountNoneEligibleForPaymentMethodError(
                accountsCount = 1,
                institution = FinancialConnectionsInstitution(
                    id = "2",
                    name = "Institution 2",
                    url = "Institution 2 url",
                    featured = false,
                    featuredOrder = null,
                    icon = null,
                    logo = null,
                    mobileHandoffCapable = false
                ),
                merchantName = "Merchant name",
                stripeException = APIException()
            )
        ),
        selectedIds = emptySet(),
    )

    private fun multiSelect() = AccountPickerState(
        payload = Success(
            AccountPickerState.Payload(
                skipAccountSelection = false,
                accounts = partnerAccountList(),
                selectionMode = AccountPickerState.SelectionMode.MULTIPLE,
                merchantDataAccess = accessibleCallout(),
                singleAccount = false,
                stripeDirect = false,
                businessName = "Random business",
                userSelectedSingleAccountInInstitution = false,
            )
        ),
        selectedIds = setOf("id1", "id3"),
    )

    private fun singleSelect() = AccountPickerState(
        payload = Success(
            AccountPickerState.Payload(
                skipAccountSelection = false,
                accounts = partnerAccountList(),
                selectionMode = AccountPickerState.SelectionMode.SINGLE,
                merchantDataAccess = accessibleCallout(),
                singleAccount = true,
                stripeDirect = false,
                businessName = "Random business",
                userSelectedSingleAccountInInstitution = false,
            )
        ),
        selectedIds = setOf("id1"),
    )

    private fun partnerAccountList() = listOf(
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
        PartnerAccount(
            authorization = "Authorization",
            category = FinancialConnectionsAccount.Category.CASH,
            id = "id2",
            name = "With balance disabled",
            balanceAmount = 1000,
            _allowSelection = false,
            allowSelectionMessage = "Cannot be selected",
            subcategory = FinancialConnectionsAccount.Subcategory.SAVINGS,
            supportedPaymentMethodTypes = emptyList()
        ),
        PartnerAccount(
            authorization = "Authorization",
            category = FinancialConnectionsAccount.Category.CASH,
            id = "id3",
            name = "No balance",
            displayableAccountNumbers = "1234",
            subcategory = FinancialConnectionsAccount.Subcategory.CREDIT_CARD,
            _allowSelection = true,
            allowSelectionMessage = "Cannot be selected",
            supportedPaymentMethodTypes = emptyList()
        ),
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
    )

    private fun accessibleCallout() = MerchantDataAccessModel(
        businessName = "My business",
        permissions = listOf(
            FinancialConnectionsAccount.Permissions.PAYMENT_METHOD,
            FinancialConnectionsAccount.Permissions.BALANCES,
            FinancialConnectionsAccount.Permissions.OWNERSHIP,
            FinancialConnectionsAccount.Permissions.TRANSACTIONS
        ),
        isStripeDirect = false,
        dataPolicyUrl = ""
    )
}
