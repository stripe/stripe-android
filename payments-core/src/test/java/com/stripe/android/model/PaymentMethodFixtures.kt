package com.stripe.android.model

import com.stripe.android.model.parsers.PaymentMethodJsonParser
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

@Suppress("LargeClass")
internal object PaymentMethodFixtures {
    val CARD = PaymentMethod.Card(
        brand = CardBrand.Visa,
        checks = PaymentMethod.Card.Checks(
            addressLine1Check = "unchecked",
            addressPostalCodeCheck = null,
            cvcCheck = "unchecked"
        ),
        country = "US",
        expiryMonth = 8,
        expiryYear = 2022,
        fingerprint = "fingerprint123",
        funding = "credit",
        last4 = "4242",
        threeDSecureUsage = PaymentMethod.Card.ThreeDSecureUsage(
            isSupported = true
        ),
        wallet = null
    )

    @JvmField
    val BILLING_DETAILS = PaymentMethod.BillingDetails(
        address = Address(
            line1 = "1234 Main Street",
            city = "San Francisco",
            state = "CA",
            postalCode = "94111",
            country = "US"
        ),
        email = "jenny.rosen@example.com",
        name = "Jenny Rosen",
        phone = "123-456-7890"
    )

    val CARD_PAYMENT_METHOD = PaymentMethod(
        id = "pm_123456789",
        created = 1550757934255L,
        liveMode = true,
        type = PaymentMethod.Type.Card,
        billingDetails = BILLING_DETAILS,
        customerId = "cus_AQsHpvKfKwJDrF",
        card = CARD,
        code = "card"
    )

    val FPX_PAYMENT_METHOD = PaymentMethod(
        id = "pm_1F5GlnH8dsfnfKo3gtixzcq0",
        created = 1565290527L,
        liveMode = true,
        type = PaymentMethod.Type.Fpx,
        billingDetails = BILLING_DETAILS,
        fpx = PaymentMethod.Fpx(
            bank = "hsbc",
            accountHolderType = "individual"
        ),
        code = "fpx"
    )

    val AU_BECS_DEBIT_PAYMENT_METHOD = PaymentMethod(
        id = "pm_1GJ4cUABjb",
        created = 1583356750L,
        liveMode = false,
        type = PaymentMethod.Type.AuBecsDebit,
        billingDetails = PaymentMethod.BillingDetails(
            name = "Jenny Rosen",
            email = "jenny.rosen@example.com",
            address = Address()
        ),
        auBecsDebit = PaymentMethod.AuBecsDebit(
            bsbNumber = "000000",
            fingerprint = "lm7qI5V7PUkWUM7E",
            last4 = "3456"
        ),
        code = "au_becs_debit"
    )

    val BACS_DEBIT_PAYMENT_METHOD = PaymentMethod(
        id = "pm_1GSREqL6pqDH",
        created = 1585588648L,
        liveMode = false,
        type = PaymentMethod.Type.BacsDebit,
        billingDetails = BILLING_DETAILS,
        bacsDebit = PaymentMethod.BacsDebit(
            fingerprint = "UkSG0Hf",
            last4 = "2345",
            sortCode = "108800"
        ),
        code = "bacs_debit"
    )

    val US_BANK_ACCOUNT_PAYMENT_METHOD = PaymentMethod(
        id = "pm_1GSREqL6pqDH",
        created = 1585588648L,
        liveMode = false,
        type = PaymentMethod.Type.USBankAccount,
        billingDetails = BILLING_DETAILS,
        usBankAccount = PaymentMethod.USBankAccount(
            accountHolderType = PaymentMethod.USBankAccount.USBankAccountHolderType.INDIVIDUAL,
            accountType = PaymentMethod.USBankAccount.USBankAccountType.CHECKING,
            bankName = "Stripe Bank",
            fingerprint = "UkSG0Hf",
            last4 = "2345",
            financialConnectionsAccount = null,
            networks = null,
            routingNumber = "110000000"
        ),
        code = "us_bank_account"
    )

    val OXXO_PAYMENT_METHOD = PaymentMethod(
        id = "pm_1IcuwoL32KlRot01",
        created = 1617638802,
        liveMode = false,
        type = PaymentMethod.Type.Oxxo,
        billingDetails = BILLING_DETAILS,
        code = "oxxo"
    )

    val ALLOW_REDISPLAY_UNSPECIFIED_JSON = JSONObject(
        """
        {
          "id": "pm_1FSQaJCR",
          "object": "payment_method",
          "billing_details": null,
          "created": 1570809799,
          "customer": null,
          "livemode": false,
          "metadata": null,
          "allow_redisplay": "unspecified",
          "link": {
            "email": "email@email.com"
          },
          "type": "link"
        }
        """.trimIndent()
    )

    val ALLOW_REDISPLAY_LIMITED_JSON = JSONObject(
        """
        {
          "id": "pm_1FSQaJCR",
          "object": "payment_method",
          "billing_details": null,
          "created": 1570809799,
          "customer": null,
          "livemode": false,
          "metadata": null,
          "allow_redisplay": "limited",
          "link": {
            "email": "email@email.com"
          },
          "type": "link"
        }
        """.trimIndent()
    )

    val ALLOW_REDISPLAY_ALWAYS_JSON = JSONObject(
        """
        {
          "id": "pm_1FSQaJCR",
          "object": "payment_method",
          "billing_details": null,
          "created": 1570809799,
          "customer": null,
          "livemode": false,
          "metadata": null,
          "allow_redisplay": "always",
          "link": {
            "email": "email@email.com"
          },
          "type": "link"
        }
        """.trimIndent()
    )

    val ALLOW_REDISPLAY_FOOBAR_JSON = JSONObject(
        """
        {
          "id": "pm_1FSQaJCR",
          "object": "payment_method",
          "billing_details": null,
          "created": 1570809799,
          "customer": null,
          "livemode": false,
          "metadata": null,
          "allow_redisplay": "foobar",
          "link": {
            "email": "email@email.com"
          },
          "type": "link"
        }
        """.trimIndent()
    )

    val SEPA_DEBIT_JSON = JSONObject(
        """
        {
          "id": "pm_1FSQaJCR",
          "object": "payment_method",
          "billing_details": {
            "address": {
              "city": null,
              "country": null,
              "line1": null,
              "line2": null,
              "postal_code": null,
              "state": null
            },
            "email": "jenny.rosen@example.com",
            "name": "Jenny Rosen",
            "phone": null
          },
          "created": 1570809799,
          "customer": null,
          "livemode": false,
          "metadata": null,
          "sepa_debit": {
            "bank_code": "3704",
            "branch_code": "",
            "country": "DE",
            "fingerprint": "vIZc7Ywn0",
            "last4": "3000"
          },
          "type": "sepa_debit"
        }
        """.trimIndent()
    )

    val SEPA_DEBIT_PAYMENT_METHOD = PaymentMethodJsonParser().parse(SEPA_DEBIT_JSON)

    internal val CARD_JSON: JSONObject = JSONObject(
        """
            {
                "id": "pm_123456789",
                "created": 1550757934255,
                "customer": "cus_AQsHpvKfKwJDrF",
                "livemode": true,
                "metadata": null,
                "type": "card",
                "billing_details": {
                    "address": {
                        "city": "San Francisco",
                        "country": "US",
                        "line1": "1234 Main Street",
                        "postal_code": "94111",
                        "state": "CA"
                    },
                    "email": "jenny.rosen@example.com",
                    "name": "Jenny Rosen",
                    "phone": "123-456-7890"
                },
                "card": {
                    "brand": "visa",
                    "checks": {
                        "address_line1_check": "unchecked",
                        "cvc_check": "unchecked"
                    },
                    "country": "US",
                    "exp_month": 8,
                    "exp_year": 2022,
                    "funding": "credit",
                    "fingerprint": "fingerprint123",
                    "last4": "4242",
                    "three_d_secure_usage": {
                        "supported": true
                    }
                }
            }
        """.trimIndent()
    )

    internal val CARD_WITH_NETWORKS_JSON = JSONObject(
        """
        {
            "id": "pm_1GDwTNAI5zDH",
            "object": "payment_method",
            "billing_details": {
                "address": {
                    "city": null,
                    "country": null,
                    "line1": null,
                    "line2": null,
                    "postal_code": null,
                    "state": null
                },
                "email": null,
                "name": null,
                "phone": null
            },
            "card": {
                "brand": "visa",
                "checks": {
                    "address_line1_check": null,
                    "address_postal_code_check": null,
                    "cvc_check": null
                },
                "country": "US",
                "exp_month": 12,
                "exp_year": 2024,
                "funding": "credit",
                "generated_from": null,
                "last4": "9999",
                "networks": {
                    "available": [
                        "network1",
                        "network2"
                    ],
                    "selection_mandatory": true,
                    "preferred": "network1"
                },
                "three_d_secure_usage": {
                    "supported": true
                },
                "wallet": null
            },
            "created": 15821393,
            "customer": null,
            "livemode": false,
            "metadata": {},
            "type": "card"
        }
        """.trimIndent()
    )

    internal val CARD_WITH_DISPLAY_BRAND_JSON = JSONObject(
        """
        {
            "id": "pm_1GDwTNAI5zDH",
            "object": "payment_method",
            "billing_details": {
                "address": {
                    "city": null,
                    "country": null,
                    "line1": null,
                    "line2": null,
                    "postal_code": null,
                    "state": null
                },
                "email": null,
                "name": null,
                "phone": null
            },
            "card": {
                "brand": "visa",
                "checks": {
                    "address_line1_check": null,
                    "address_postal_code_check": null,
                    "cvc_check": null
                },
                "country": "US",
                "exp_month": 12,
                "exp_year": 2024,
                "funding": "credit",
                "generated_from": null,
                "last4": "9999",
                "display_brand": "cartes_bancaires",
                "three_d_secure_usage": {
                    "supported": true
                },
                "wallet": null
            },
            "created": 15821393,
            "customer": null,
            "livemode": false,
            "metadata": {},
            "type": "card"
        }
        """.trimIndent()
    )

    internal val CARD_WITH_DISPLAY_BRAND = PaymentMethodJsonParser().parse(CARD_WITH_DISPLAY_BRAND_JSON)

    val IDEAL_JSON = JSONObject(
        """
            {
                "id": "pm_123456789",
                "created": 1550757934255,
                "customer": "cus_AQsHpvKfKwJDrF",
                "livemode": true,
                "type": "ideal",
                "billing_details": {
                    "address": {
                        "city": "San Francisco",
                        "country": "US",
                        "line1": "1234 Main Street",
                        "postal_code": "94111",
                        "state": "CA"
                    },
                    "email": "jenny.rosen@example.com",
                    "name": "Jenny Rosen",
                    "phone": "123-456-7890"
                },
                "ideal": {
                    "bank": "my bank",
                    "bic": "bank id"
                }
            }
        """.trimIndent()
    )

    val FPX_JSON = JSONObject(
        """
            {
                "id": "pm_1F5GlnH8dsfnfKo3gtixzcq0",
                "object": "payment_method",
                "billing_details": {
                    "address": {
                        "city": "San Francisco",
                        "country": "US",
                        "line1": "1234 Main Street",
                        "line2": null,
                        "postal_code": "94111",
                        "state": "CA"
                    },
                    "email": "jenny.rosen@example.com",
                    "name": "Jenny Rosen",
                    "phone": "123-456-7890"
                },
                "created": 1565290527,
                "customer": null,
                "fpx": {
                    "account_holder_type": "individual",
                    "bank": "hsbc"
                },
                "livemode": true,
                "metadata": null,
                "type": "fpx"
            }
        """.trimIndent()
    )

    val AU_BECS_DEBIT_JSON = JSONObject(
        """
        {
            "id": "pm_1GJ4cUABjb",
            "object": "payment_method",
            "au_becs_debit": {
                "bsb_number": "000000",
                "fingerprint": "lm7qI5V7PUkWUM7E",
                "last4": "3456"
            },
            "billing_details": {
                "address": {
                    "city": null,
                    "country": null,
                    "line1": null,
                    "line2": null,
                    "postal_code": null,
                    "state": null
                },
                "email": "jenny.rosen@example.com",
                "name": "Jenny Rosen",
                "phone": null
            },
            "created": 1583356750,
            "customer": null,
            "livemode": false,
            "metadata": null,
            "type": "au_becs_debit"
        }
        """.trimIndent()
    )

    val BACS_DEBIT_JSON = JSONObject(
        """
        {
            "id": "pm_1GSREqL6pqDH",
            "object": "payment_method",
            "bacs_debit": {
                "fingerprint": "UkSG0Hf",
                "last4": "2345",
                "sort_code": "108800"
            },
            "billing_details": {
                "address": {
                    "city": "San Francisco",
                    "country": "US",
                    "line1": "1234 Main Street",
                    "line2": null,
                    "postal_code": "94111",
                    "state": "CA"
                },
                "email": "jenny.rosen@example.com",
                "name": "Jenny Rosen",
                "phone": "123-456-7890"
            },
            "created": 1585588648,
            "customer": null,
            "livemode": false,
            "metadata": null,
            "type": "bacs_debit"
        }
        """.trimIndent()
    )

    val US_BANK_ACCOUNT_WITH_FCA = JSONObject(
        """
        {
            "id": "pm_123456",
            "object": "payment_method",
            "billing_details": {
                "address": {
                    "city": "Seattle",
                    "country": "US",
                    "line1": "123 Main St.",
                    "line2": null,
                    "postal_code": "99999",
                    "state": "AL"
                },
                "email": "jenny@example.com",
                "name": "Jenny Rosen",
                "phone": null
            },
            "created": 1706132691,
            "customer": null,
            "livemode": false,
            "type": "us_bank_account",
            "us_bank_account": {
                "account_holder_type": "individual",
                "account_type": "checking",
                "bank_name": "STRIPE TEST BANK",
                "financial_connections_account": "fca_111",
                "fingerprint": "FFDMA0xfhBjWSZLu",
                "last4": "6789",
                "networks":{ 
                    "preferred": "ach",
                    "supported": ["ach","us_domestic_wire"]},
                    "routing_number": "110000000", 
                    "status_details": null
                }
            }
        }
        """.trimIndent()
    )

    val CARD_PAYMENT_METHODS = listOf(
        PaymentMethod(
            id = "pm_1000",
            created = 1000L,
            liveMode = false,
            type = PaymentMethod.Type.Card,
            card = PaymentMethod.Card(
                brand = CardBrand.Visa,
                last4 = "4242"
            ),
            code = "card"
        ),
        PaymentMethod(
            id = "pm_2000",
            created = 2000L,
            liveMode = false,
            type = PaymentMethod.Type.Card,
            card = PaymentMethod.Card(
                brand = CardBrand.Visa,
                last4 = "3063"
            ),
            code = "card"
        ),
        PaymentMethod(
            id = "pm_3000",
            created = 3000L,
            liveMode = false,
            type = PaymentMethod.Type.Card,
            card = PaymentMethod.Card(
                brand = CardBrand.Visa,
                last4 = "3220"
            ),
            code = "card"
        )
    )

    @JvmOverloads
    fun createCard(createdOrigin: Long? = null): PaymentMethod {
        val id = "pm_" + UUID.randomUUID().toString()
            .replace("-", "")
        return PaymentMethod(
            id = id,
            created = ThreadLocalRandom.current().nextLong(
                createdOrigin ?: 1L,
                10000000
            ),
            liveMode = false,
            type = PaymentMethod.Type.Card,
            card = PaymentMethod.Card(
                brand = CardBrand.Visa,
                last4 = createLast4()
            ),
            code = "card"
        )
    }

    private fun createLast4(): String {
        return ThreadLocalRandom.current().nextInt(1000, 9999).toString()
    }

    fun createCards(size: Int): List<PaymentMethod> {
        var origin = 1L
        return (0 until size).map {
            val paymentMethod = createCard(origin)
            origin = paymentMethod.created!!
            paymentMethod
        }
    }

    fun createPaymentMethod(type: PaymentMethod.Type): PaymentMethod {
        return PaymentMethod(
            id = "pm_123",
            created = ThreadLocalRandom.current().nextLong(1L, 10000000L),
            liveMode = false,
            type = type,
            card = PaymentMethod.Card(
                brand = CardBrand.Visa
            ),
            code = "card"
        )
    }

    val BLIK_JSON = JSONObject(
        """
        {
            "payment_method": {
                "id": "pm_1Mm5TUKG6vc7r7YC41rnkvqS",
                "object": "payment_method",
                "billing_details": {
                  "address": {
                    "city": null,
                    "country": null,
                    "line1": null,
                    "line2": null,
                    "postal_code": null,
                    "state": null
                  },
                  "email": null,
                  "name": null,
                  "phone": null
                },
                "blik": {},
                "created": 1678929564,
                "customer": null,
                "livemode": false,
                "type": "blik"
            }
        }
        """.trimIndent()
    )

    val BLIK_PAYMENT_METHOD = PaymentMethodJsonParser().parse(BLIK_JSON)
}
