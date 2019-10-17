package com.stripe.android.model

import java.util.UUID
import java.util.concurrent.ThreadLocalRandom
import org.json.JSONObject

internal object PaymentMethodFixtures {
    @JvmField
    val CARD = PaymentMethod.Card.Builder()
        .setBrand(PaymentMethod.Card.Brand.VISA)
        .setChecks(PaymentMethod.Card.Checks.create(
            addressLine1Check = "unchecked",
            addressPostalCodeCheck = null,
            cvcCheck = "unchecked"
        ))
        .setCountry("US")
        .setExpiryMonth(8)
        .setExpiryYear(2022)
        .setFunding("credit")
        .setLast4("4242")
        .setThreeDSecureUsage(PaymentMethod.Card.ThreeDSecureUsage.create(
            isSupported = true
        ))
        .setWallet(null)
        .build()

    @JvmField
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

    @JvmField
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

    @JvmField
    val FPX_PAYMENT_METHOD = PaymentMethod.Builder()
        .setId("pm_1F5GlnH8dsfnfKo3gtixzcq0")
        .setCreated(1565290527L)
        .setLiveMode(true)
        .setType("fpx")
        .setBillingDetails(BILLING_DETAILS)
        .setFpx(PaymentMethod.Fpx.create(
            "hsbc",
            "individual"
        ))
        .build()

    @JvmField
    val SEPA_DEBIT_PAYMENT_METHOD = PaymentMethod.fromJson(JSONObject(
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
    ))!!

    @JvmField
    val CARD_PAYMENT_METHODS = listOf(
        PaymentMethod.Builder()
            .setType("card")
            .setCreated(1000L)
            .setId("pm_1000")
            .setCard(PaymentMethod.Card.Builder()
                .setBrand("visa")
                .setLast4("4242")
                .build())
            .build(),
        PaymentMethod.Builder()
            .setType("card")
            .setCreated(2000L)
            .setId("pm_2000")
            .setCard(PaymentMethod.Card.Builder()
                .setBrand("visa")
                .setLast4("3063")
                .build())
            .build(),
        PaymentMethod.Builder()
            .setType("card")
            .setCreated(3000L)
            .setId("pm_3000")
            .setCard(PaymentMethod.Card.Builder()
                .setBrand("visa")
                .setLast4("3220")
                .build())
            .build()
    )

    @JvmStatic
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
            .setCard(PaymentMethod.Card.Builder()
                .setBrand("visa")
                .setLast4(ThreadLocalRandom.current().nextInt(1000, 9999).toString())
                .build())
            .build()
    }

    @JvmStatic
    fun createCards(size: Int): List<PaymentMethod> {
        var origin = 1L
        return (0 until size).map {
            val paymentMethod = createCard(origin)
            origin = paymentMethod.created!!
            paymentMethod
        }
    }
}
