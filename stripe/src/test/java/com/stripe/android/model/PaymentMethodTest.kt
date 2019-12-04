package com.stripe.android.model

import com.stripe.android.model.parsers.PaymentMethodJsonParser
import com.stripe.android.utils.ParcelUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.json.JSONException
import org.json.JSONObject
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentMethodTest {

    @Test
    @Throws(JSONException::class)
    fun toJson_withIdeal_shouldCreateExpectedObject() {
        val paymentMethod = PaymentMethod.Builder()
            .setId("pm_123456789")
            .setCreated(1550757934255L)
            .setLiveMode(true)
            .setType("ideal")
            .setCustomerId("cus_AQsHpvKfKwJDrF")
            .setBillingDetails(PaymentMethodFixtures.BILLING_DETAILS)
            .setIdeal(PaymentMethod.Ideal("my bank", "bank id"))
            .build()

        assertEquals(paymentMethod, PaymentMethodJsonParser().parse(PM_IDEAL_JSON))
    }

    @Test
    @Throws(JSONException::class)
    fun toJson_withFpx_shouldCreateExpectedObject() {
        assertEquals(PaymentMethodFixtures.FPX_PAYMENT_METHOD,
            PaymentMethodJsonParser().parse(PM_FPX_JSON))
    }

    @Test
    fun toJson_withSepaDebit_shouldCreateExpectedObject() {
        assertEquals(PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD,
            PaymentMethod.Builder()
                .setType(PaymentMethod.Type.SepaDebit.code)
                .setId("pm_1FSQaJCR")
                .setLiveMode(false)
                .setCreated(1570809799L)
                .setSepaDebit(
                    PaymentMethod.SepaDebit(
                        "3704",
                        null,
                        "DE",
                        "vIZc7Ywn0",
                        "3000"
                    )
                )
                .setBillingDetails(
                    PaymentMethod.BillingDetails.Builder()
                        .setName("Jenny Rosen")
                        .setEmail("jrosen@example.com")
                        .setAddress(Address.Builder().build())
                        .build()
                )
                .build()
        )
    }

    @Test
    fun equals_withEqualPaymentMethods_shouldReturnTrue() {
        assertEquals(PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            PaymentMethod.Builder()
                .setId("pm_123456789")
                .setCreated(1550757934255L)
                .setLiveMode(true)
                .setType("card")
                .setCustomerId("cus_AQsHpvKfKwJDrF")
                .setBillingDetails(PaymentMethodFixtures.BILLING_DETAILS)
                .setCard(PaymentMethodFixtures.CARD)
                .setMetadata(mapOf("order_id" to "123456789"))
                .build())
    }

    @Test
    @Throws(JSONException::class)
    fun fromString_shouldReturnExpectedPaymentMethod() {
        assertEquals(PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            PaymentMethodJsonParser().parse(PM_CARD_JSON))
    }

    @Test
    @Throws(JSONException::class)
    fun fromString_withIdeal_returnsExpectedObject() {
        val paymentMethod = PaymentMethodJsonParser().parse(PM_IDEAL_JSON)
        assertEquals("ideal", paymentMethod?.type)
    }

    @Test
    fun billingDetails_toParamMap_removesNullValues() {
        val billingDetails = PaymentMethod.BillingDetails.Builder()
            .setName("name")
            .build()
            .toParamMap()
        assertEquals(1, billingDetails.size)
        assertFalse(billingDetails.containsKey(PaymentMethod.BillingDetails.PARAM_ADDRESS))
        assertTrue(billingDetails.containsKey(PaymentMethod.BillingDetails.PARAM_NAME))
    }

    @Test
    fun testParcelable_shouldBeEqualAfterParcel() {
        val metadata = mapOf(
            "meta" to "data",
            "meta2" to "data2"
        )
        val paymentMethod = PaymentMethod.Builder()
            .setBillingDetails(PaymentMethodFixtures.BILLING_DETAILS)
            .setCreated(1550757934255L)
            .setCustomerId("cus_AQsHpvKfKwJDrF")
            .setId("pm_123456789")
            .setType("card")
            .setLiveMode(true)
            .setMetadata(metadata)
            .setCard(PaymentMethodFixtures.CARD)
            .setCardPresent(PaymentMethod.CardPresent.EMPTY)
            .setFpx(PaymentMethodFixtures.FPX_PAYMENT_METHOD.fpx)
            .setIdeal(PaymentMethod.Ideal("my bank", "bank id"))
            .setSepaDebit(PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD.sepaDebit)
            .build()

        assertEquals(paymentMethod, ParcelUtils.create(paymentMethod))
    }

    @Test
    fun testBillingDetailsToBuilder() {
        assertEquals(
            PaymentMethodFixtures.BILLING_DETAILS,
            PaymentMethodFixtures.BILLING_DETAILS.toBuilder()
                .build()
        )
    }

    internal companion object {
        internal val PM_CARD_JSON: JSONObject = JSONObject(
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
                        "country": "USA",
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

        private val PM_IDEAL_JSON = JSONObject(
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
                        "country": "USA",
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

        private val PM_FPX_JSON = JSONObject(
            """
            {
                "id": "pm_1F5GlnH8dsfnfKo3gtixzcq0",
                "object": "payment_method",
                "billing_details": {
                    "address": {
                        "city": "San Francisco",
                        "country": "USA",
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
    }
}
