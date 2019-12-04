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

    val BILLING_DETAILS = PaymentMethod.BillingDetails.Builder()
        .setAddress(Address.Builder()
            .setLine1("510 Townsend St")
            .setCity("San Francisco")
            .setState("CA")
            .setPostalCode("94103")
            .setCountry("USA")
            .build())
        .setEmail("patrick@example.com")
        .setName("Patrick")
        .setPhone("123-456-7890")
        .build()

    val CARD_PAYMENT_METHOD = PaymentMethod.Builder()
        .setId("pm_123456789")
        .setCreated(1550757934255L)
        .setLiveMode(true)
        .setType("card")
        .setCustomerId("cus_AQsHpvKfKwJDrF")
        .setBillingDetails(BILLING_DETAILS)
        .setCard(CARD)
        .setMetadata(mapOf("order_id" to "123456789"))
        .build()

    val FPX_PAYMENT_METHOD = PaymentMethod.Builder()
        .setId("pm_1F5GlnH8dsfnfKo3gtixzcq0")
        .setCreated(1565290527L)
        .setLiveMode(true)
        .setType("fpx")
        .setBillingDetails(BILLING_DETAILS)
        .setFpx(PaymentMethod.Fpx(
            "hsbc",
            "individual"
        ))
        .build()

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
        PaymentMethod.Builder()
            .setType("card")
            .setCreated(1000L)
            .setId("pm_1000")
            .setCard(PaymentMethod.Card(
                brand = "visa",
                last4 = "4242"
            ))
            .build(),
        PaymentMethod.Builder()
            .setType("card")
            .setCreated(2000L)
            .setId("pm_2000")
            .setCard(PaymentMethod.Card(
                brand = "visa",
                last4 = "3063"
            ))
            .build(),
        PaymentMethod.Builder()
            .setType("card")
            .setCreated(3000L)
            .setId("pm_3000")
            .setCard(PaymentMethod.Card(
                brand = "visa",
                last4 = "3220"
            ))
            .build()
    )

    @JvmOverloads
    fun createCard(createdOrigin: Long? = null): PaymentMethod {
        val id = "pm_" + UUID.randomUUID().toString()
            .replace("-", "")
        return PaymentMethod.Builder()
            .setType("card")
            .setCreated(
                ThreadLocalRandom.current().nextLong(
                    createdOrigin ?: 1L,
                    10000000
                )
            )
            .setId(id)
            .setCard(PaymentMethod.Card(
                brand = "visa",
                last4 = createLast4()
            ))
            .build()
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
