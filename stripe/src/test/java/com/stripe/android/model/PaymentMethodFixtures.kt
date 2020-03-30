package com.stripe.android.model

import com.stripe.android.model.parsers.PaymentMethodJsonParser
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom
import org.json.JSONObject

internal object PaymentMethodFixtures {
    val CARD = PaymentMethod.Card(
        brand = PaymentMethod.Card.Brand.VISA,
        checks = PaymentMethod.Card.Checks(
            addressLine1Check = "unchecked",
            addressPostalCodeCheck = null,
            cvcCheck = "unchecked"
        ),
        country = "US",
        expiryMonth = 8,
        expiryYear = 2022,
        funding = "credit",
        last4 = "4242",
        threeDSecureUsage = PaymentMethod.Card.ThreeDSecureUsage(
            isSupported = true
        ),
        wallet = null
    )

    val BILLING_DETAILS = PaymentMethod.BillingDetails(
        address = Address(
            line1 = "510 Townsend St",
            city = "San Francisco",
            state = "CA",
            postalCode = "94103",
            country = "US"
        ),
        email = "patrick@example.com",
        name = "Patrick",
        phone = "123-456-7890"
    )

    val CARD_PAYMENT_METHOD = PaymentMethod(
        id = "pm_123456789",
        created = 1550757934255L,
        liveMode = true,
        type = PaymentMethod.Type.Card,
        customerId = "cus_AQsHpvKfKwJDrF",
        billingDetails = BILLING_DETAILS,
        card = CARD,
        metadata = mapOf("order_id" to "123456789")
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
        )
    )

    val AU_BECS_DEBIT_PAYMENT_METHOD = PaymentMethod(
        id = "pm_1GJ4cUABjb",
        created = 1583356750L,
        liveMode = false,
        type = PaymentMethod.Type.AuBecsDebit,
        billingDetails = PaymentMethod.BillingDetails(
            name = "Jenny Rosen",
            email = "jrosen@example.com",
            address = Address()
        ),
        auBecsDebit = PaymentMethod.AuBecsDebit(
            bsbNumber = "000000",
            fingerprint = "lm7qI5V7PUkWUM7E",
            last4 = "3456"
        ),
        metadata = emptyMap()
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
            "email": "jrosen@example.com",
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
                "metadata": {
                    "order_id": "123456789"
                },
                "type": "card",
                "billing_details": {
                    "address": {
                        "city": "San Francisco",
                        "country": "US",
                        "line1": "510 Townsend St",
                        "postal_code": "94103",
                        "state": "CA"
                    },
                    "email": "patrick@example.com",
                    "name": "Patrick",
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
                        "line1": "510 Townsend St",
                        "postal_code": "94103",
                        "state": "CA"
                    },
                    "email": "patrick@example.com",
                    "name": "Patrick",
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
                        "line1": "510 Townsend St",
                        "line2": null,
                        "postal_code": "94103",
                        "state": "CA"
                    },
                    "email": "patrick@example.com",
                    "name": "Patrick",
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
                "email": "jrosen@example.com",
                "name": "Jenny Rosen",
                "phone": null
            },
            "created": 1583356750,
            "customer": null,
            "livemode": false,
            "metadata": {},
            "type": "au_becs_debit"
        }
        """.trimIndent()
    )

    val CARD_PAYMENT_METHODS = listOf(
        PaymentMethod(
            type = PaymentMethod.Type.Card,
            liveMode = false,
            created = 1000L,
            id = "pm_1000",
            card = PaymentMethod.Card(
                brand = "visa",
                last4 = "4242"
            )
        ),
        PaymentMethod(
            type = PaymentMethod.Type.Card,
            liveMode = false,
            created = 2000L,
            id = "pm_2000",
            card = PaymentMethod.Card(
                brand = "visa",
                last4 = "3063"
            )
        ),
        PaymentMethod(
            type = PaymentMethod.Type.Card,
            liveMode = false,
            created = 3000L,
            id = "pm_3000",
            card = PaymentMethod.Card(
                brand = "visa",
                last4 = "3220"
            )
        )
    )

    @JvmOverloads
    fun createCard(createdOrigin: Long? = null): PaymentMethod {
        val id = "pm_" + UUID.randomUUID().toString()
            .replace("-", "")
        return PaymentMethod(
            type = PaymentMethod.Type.Card,
            liveMode = false,
            created = ThreadLocalRandom.current().nextLong(
                createdOrigin ?: 1L,
                10000000
            ),
            id = id,
            card = PaymentMethod.Card(
                brand = "visa",
                last4 = createLast4()
            )
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
}
