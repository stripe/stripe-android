package com.stripe.android.connections.model

internal object LinkedAccountFixtures {

    val CREDIT_CARD = LinkedAccount(
        id = "la_1KMEuEClCIKljWvsfeLEm28K",
        displayName = "My Credit Card",
        institutionName = "My Bank",
        last4 = "3456",
        category = LinkedAccount.Category.CREDIT,
        created = 1643216618,
        livemode = true,
        permissions = listOf(LinkedAccount.Permissions.PAYMENT_METHOD),
        status = LinkedAccount.Status.ACTIVE,
        subcategory = LinkedAccount.Subcategory.CREDIT_CARD,
        supportedPaymentMethodTypes = emptyList()
    )

    val CHECKING_ACCOUNT = LinkedAccount(
        id = "la_1KMGIuClCIKljWvsLzbigpVh",
        displayName = "My Checking",
        institutionName = "My Bank",
        last4 = "3456",
        category = LinkedAccount.Category.CASH,
        created = 1643221992,
        livemode = true,
        permissions = listOf(LinkedAccount.Permissions.PAYMENT_METHOD),
        status = LinkedAccount.Status.ACTIVE,
        subcategory = LinkedAccount.Subcategory.CHECKING,
        supportedPaymentMethodTypes = listOf(
            LinkedAccount.SupportedPaymentMethodTypes.US_BANK_ACCOUNT,
            LinkedAccount.SupportedPaymentMethodTypes.LINK
        ),
    )

    val SAVINGS_ACCOUNT = LinkedAccount(
        id = "la_1KMGIuClCIKljWvsN3k8bH3B",
        displayName = "My Savings",
        institutionName = "My Bank",
        last4 = "6789",
        category = LinkedAccount.Category.CASH,
        created = 1643221992,
        livemode = true,
        permissions = listOf(LinkedAccount.Permissions.PAYMENT_METHOD),
        status = LinkedAccount.Status.ACTIVE,
        subcategory = LinkedAccount.Subcategory.SAVINGS,
        supportedPaymentMethodTypes = listOf(
            LinkedAccount.SupportedPaymentMethodTypes.US_BANK_ACCOUNT,
        ),
    )
}
