package com.stripe.android.paymentsheet.paymentdatacollection.ach.model

import com.stripe.android.financialconnections.model.BankAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount

internal object FinancialConnectionsAccountFixtures {
    val CHECKING_ACCOUNT = FinancialConnectionsAccount(
        id = "la_1KMGIuClCIKljWvsLzbigpVh",
        displayName = "My Checking",
        institutionName = "My Bank",
        last4 = "3456",
        category = FinancialConnectionsAccount.Category.CASH,
        created = 1643221992,
        livemode = true,
        permissions = listOf(FinancialConnectionsAccount.Permissions.PAYMENT_METHOD),
        status = FinancialConnectionsAccount.Status.ACTIVE,
        subcategory = FinancialConnectionsAccount.Subcategory.CHECKING,
        supportedPaymentMethodTypes = listOf(
            FinancialConnectionsAccount.SupportedPaymentMethodTypes.US_BANK_ACCOUNT,
            FinancialConnectionsAccount.SupportedPaymentMethodTypes.LINK
        )
    )

    val BANK_ACCOUNT = BankAccount(
        id = "1234",
        last4 = "3456",
        bankName = "My Bank",
        routingNumber = "123456789",
    )
}
