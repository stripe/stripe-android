package com.stripe.android.financialconnections.features.accountpicker

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.stripe.android.core.exception.APIException
import com.stripe.android.financialconnections.exception.AccountNoneEligibleForPaymentMethodError
import com.stripe.android.financialconnections.features.accountpicker.AccountPickerState.SelectionMode
import com.stripe.android.financialconnections.model.Bullet
import com.stripe.android.financialconnections.model.ConnectedAccessNotice
import com.stripe.android.financialconnections.model.DataAccessNotice
import com.stripe.android.financialconnections.model.DataAccessNoticeBody
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.Image
import com.stripe.android.financialconnections.model.PartnerAccount
import com.stripe.android.financialconnections.presentation.Async.Fail
import com.stripe.android.financialconnections.presentation.Async.Loading
import com.stripe.android.financialconnections.presentation.Async.Success

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
        institution = Success(
            FinancialConnectionsInstitution(
                id = "1",
                name = "Institution 1",
                url = "Institution 1 url",
                featured = false,
                featuredOrder = null,
                icon = Image(default = ""),
                logo = null,
                mobileHandoffCapable = false
            )
        ),
        payload = Success(
            AccountPickerState.Payload(
                skipAccountSelection = false,
                accounts = partnerAccountList(),
                dataAccessNotice = dataAccessNotice(),
                selectionMode = SelectionMode.Multiple,
                dataAccessDisclaimer = "My business can access account details, balances, " +
                    "account ownership details and transactions. <a href=\"https://stripe.com\">Learn more</a>",
                singleAccount = false,
                stripeDirect = false,
                businessName = "Random business",
                userSelectedSingleAccountInInstitution = false,
            )
        ),
        selectedIds = setOf("id1", "id3"),
    )

    private fun singleSelect() = AccountPickerState(
        institution = Success(
            FinancialConnectionsInstitution(
                id = "1",
                name = "Institution 1",
                url = "Institution 1 url",
                featured = false,
                featuredOrder = null,
                icon = Image(default = ""),
                logo = null,
                mobileHandoffCapable = false
            )
        ),
        payload = Success(
            AccountPickerState.Payload(
                skipAccountSelection = false,
                accounts = partnerAccountList(),
                dataAccessNotice = dataAccessNotice(),
                selectionMode = SelectionMode.Single,
                dataAccessDisclaimer = null,
                singleAccount = true,
                stripeDirect = false,
                businessName = "Random business",
                userSelectedSingleAccountInInstitution = false,
            )
        ),
        selectedIds = setOf("id1"),
    )

    private fun dataAccessNotice() = DataAccessNotice(
        icon = Image("https://www.cdn.stripe.com/12321312321.png"),
        title = "Goldilocks uses Stripe to link your accounts",
        subtitle = "Goldilocks will use your account and routing number, balances and transactions:",
        body = DataAccessNoticeBody(
            bullets = bullets()
        ),
        disclaimer = "Learn more about data access",
        connectedAccountNotice = ConnectedAccessNotice(
            subtitle = "Connected account placeholder",
            body = DataAccessNoticeBody(
                bullets = bullets()
            )
        ),
        cta = "OK"
    )

    private fun bullets() = listOf(
        Bullet(
            icon = Image("https://www.cdn.stripe.com/12321312321.png"),
            title = "Account details",
            content = "Account number, routing number, account type, account nickname."
        ),
        Bullet(
            icon = Image("https://www.cdn.stripe.com/12321312321.png"),
            title = "Account details",
            content = "Account number, routing number, account type, account nickname."
        ),
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
}
