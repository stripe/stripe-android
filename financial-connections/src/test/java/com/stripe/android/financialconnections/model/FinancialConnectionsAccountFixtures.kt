package com.stripe.android.financialconnections.model

internal object FinancialConnectionsAccountFixtures {

    val CREDIT_CARD = FinancialConnectionsAccount(
        id = "la_1KMEuEClCIKljWvsfeLEm28K",
        displayName = "My Credit Card",
        institutionName = "My Bank",
        last4 = "3456",
        category = FinancialConnectionsAccount.Category.CREDIT,
        created = 1643216618,
        livemode = true,
        permissions = listOf(FinancialConnectionsAccount.Permissions.PAYMENT_METHOD),
        status = FinancialConnectionsAccount.Status.ACTIVE,
        subcategory = FinancialConnectionsAccount.Subcategory.CREDIT_CARD,
        supportedPaymentMethodTypes = emptyList()
    )

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

    val SAVINGS_ACCOUNT = FinancialConnectionsAccount(
        id = "la_1KMGIuClCIKljWvsN3k8bH3B",
        displayName = "My Savings",
        institutionName = "My Bank",
        last4 = "6789",
        category = FinancialConnectionsAccount.Category.CASH,
        created = 1643221992,
        livemode = true,
        permissions = listOf(FinancialConnectionsAccount.Permissions.PAYMENT_METHOD),
        status = FinancialConnectionsAccount.Status.ACTIVE,
        subcategory = FinancialConnectionsAccount.Subcategory.SAVINGS,
        supportedPaymentMethodTypes = listOf(
            FinancialConnectionsAccount.SupportedPaymentMethodTypes.US_BANK_ACCOUNT
        )
    )
}
