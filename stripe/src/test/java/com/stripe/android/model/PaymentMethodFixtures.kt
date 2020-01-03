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
            country = "USA"
        ),
        email = "patrick@example.com",
        name = "Patrick",
        phone = "123-456-7890"
    )

    val CARD_PAYMENT_METHOD = PaymentMethod(
        id = "pm_123456789",
        created = 1550757934255L,
        liveMode = true,
        type = PaymentMethod.Type.Card.code,
        customerId = "cus_AQsHpvKfKwJDrF",
        billingDetails = BILLING_DETAILS,
        card = CARD,
        metadata = mapOf("order_id" to "123456789")
    )

    val FPX_PAYMENT_METHOD = PaymentMethod(
        id = "pm_1F5GlnH8dsfnfKo3gtixzcq0",
        created = 1565290527L,
        liveMode = true,
        type = PaymentMethod.Type.Fpx.code,
        billingDetails = BILLING_DETAILS,
        fpx = PaymentMethod.Fpx(
            bank = "hsbc",
            accountHolderType = "individual"
        )
    )

    val SEPA_DEBIT_PAYMENT_METHOD = PaymentMethodJsonParser().parse(JSONObject(
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
    ))

    val CARD_PAYMENT_METHODS = listOf(
        PaymentMethod(
            type = PaymentMethod.Type.Card.code,
            liveMode = false,
            created = 1000L,
            id = "pm_1000",
            card = PaymentMethod.Card(
                brand = "visa",
                last4 = "4242"
            )
        ),
        PaymentMethod(
            type = PaymentMethod.Type.Card.code,
            liveMode = false,
            created = 2000L,
            id = "pm_2000",
            card = PaymentMethod.Card(
                brand = "visa",
                last4 = "3063"
            )
        ),
        PaymentMethod(
            type = PaymentMethod.Type.Card.code,
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
            type = PaymentMethod.Type.Card.code,
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
