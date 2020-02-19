package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.utils.ParcelUtils
import kotlin.test.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentMethodTest {
    @Test
    fun equals_withEqualPaymentMethods_shouldReturnTrue() {
        assertThat(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
            .isEqualTo(
                PaymentMethod(
                    id = "pm_123456789",
                    created = 1550757934255L,
                    liveMode = true,
                    type = PaymentMethod.Type.Card,
                    customerId = "cus_AQsHpvKfKwJDrF",
                    billingDetails = PaymentMethodFixtures.BILLING_DETAILS,
                    card = PaymentMethodFixtures.CARD,
                    metadata = mapOf("order_id" to "123456789")
                )
            )
    }

    @Test
    fun billingDetails_toParamMap_removesNullValues() {
        val billingDetails = PaymentMethod.BillingDetails(name = "name")
            .toParamMap()
        assertThat(billingDetails.keys)
            .isEqualTo(setOf(PaymentMethod.BillingDetails.PARAM_NAME))
    }

    @Test
    fun testParcelable() {
        val paymentMethod = PaymentMethod(
            billingDetails = PaymentMethodFixtures.BILLING_DETAILS,
            created = 1550757934255L,
            customerId = "cus_AQsHpvKfKwJDrF",
            id = "pm_123456789",
            type = PaymentMethod.Type.Card,
            liveMode = true,
            metadata = mapOf(
                "meta" to "data",
                "meta2" to "data2"
            ),
            card = PaymentMethodFixtures.CARD,
            cardPresent = PaymentMethod.CardPresent.EMPTY,
            fpx = PaymentMethodFixtures.FPX_PAYMENT_METHOD.fpx,
            ideal = PaymentMethod.Ideal("my bank", "bank id"),
            sepaDebit = PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD.sepaDebit
        )
        ParcelUtils.verifyParcelRoundtrip(paymentMethod)
    }

    @Test
    fun testBillingDetailsToBuilder() {
        assertThat(
            PaymentMethodFixtures.BILLING_DETAILS.toBuilder()
                .build()
        ).isEqualTo(PaymentMethodFixtures.BILLING_DETAILS)
    }
}
