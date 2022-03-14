package com.stripe.android.connections.model

internal object LinkedAccountFixtures {

    val CREDIT_CARD_WITH_BALANCES_JSON = """
        {
            "id": "la_1KORBcClCIKljWvswF82ymIp",
            "object": "linked_account",
            "balance": {
              "as_of": 1643713200,
              "credit": {
                "used": null
              },
              "current": {
                "usd": -31415
              },
              "type": "credit"
            },
            "balance_refresh": {
              "last_attempted_at": 1643740480,
              "status": "succeeded"
            },
            "category": "credit",
            "created": 1643740480,
            "display_name": "CREDIT CARD",
            "institution_name": "My Bank",
            "last4": "3456",
            "livemode": true,
            "permissions": [
              "balances",
              "payment_method"
            ],
            "status": "active",
            "subcategory": "credit_card",
            "supported_payment_method_types": [
    
            ]
          }
    """.trimIndent()

    val CHECKING_ACCOUNT_JSON = """
        {
            "id": "la_1KMGIuClCIKljWvsLzbigpVh",
            "object": "linked_account",
            "balance": null,
            "balance_refresh": null,
            "category": "cash",
            "created": 1643221992,
            "display_name": "My Checking",
            "institution_name": "My Bank",
            "last4": "3456",
            "livemode": true,
            "permissions": [
              "payment_method"
            ],
            "status": "active",
            "subcategory": "checking",
            "supported_payment_method_types": [
              "us_bank_account",
              "link"
            ],
            "account_holder" : {
              "type": "customer",
              "customer": "Stripe Customer"
            }
          }          
    """.trimIndent()

    val CHECKING_ACCOUNT_WITH_BALANCE_JSON = """
         {
            "id": "la_1KOTatClCIKljWvs3zV3s6TW",
            "object": "linked_account",
            "balance": {
              "as_of": 1643749734,
              "cash": {
                "available": {
                  "usd": 314159265
                }
              },
              "current": {
                "usd": 314159265
              },
              "type": "cash"
            },
            "balance_refresh": {
              "last_attempted_at": 1643749735,
              "status": "succeeded"
            },
            "category": "cash",
            "created": 1643749735,
            "display_name": "My Checking",
            "institution_name": "My Bank",
            "last4": "8787",
            "livemode": true,
            "permissions": [
              "balances",
              "payment_method"
            ],
            "status": "active",
            "subcategory": "checking",
            "supported_payment_method_types": [
              "us_bank_account",
              "link"
            ]
          }
    """.trimIndent()

    val SAVINGS_ACCOUNT_JSON = """
        {
            "id": "la_1KMGIuClCIKljWvsN3k8bH3B",
            "object": "linked_account",
            "balance": null,
            "balance_refresh": null,
            "category": "cash",
            "created": 1643221992,
            "display_name": "My Savings",
            "institution_name": "My Bank",
            "last4": "6789",
            "livemode": true,
            "permissions": [
              "payment_method"
            ],
            "status": "active",
            "subcategory": "savings",
            "supported_payment_method_types": [
              "us_bank_account"
            ]
          }
    """.trimIndent()

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
