package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.utils.ParcelUtils
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

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
                    billingDetails = PaymentMethodFixtures.BILLING_DETAILS,
                    customerId = "cus_AQsHpvKfKwJDrF",
                    card = PaymentMethodFixtures.CARD,
                    code = "card"
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
            id = "pm_123456789",
            created = 1550757934255L,
            liveMode = true,
            type = PaymentMethod.Type.Card,
            billingDetails = PaymentMethodFixtures.BILLING_DETAILS,
            customerId = "cus_AQsHpvKfKwJDrF",
            card = PaymentMethodFixtures.CARD,
            cardPresent = PaymentMethod.CardPresent.EMPTY,
            fpx = PaymentMethodFixtures.FPX_PAYMENT_METHOD.fpx,
            ideal = PaymentMethod.Ideal("my bank", "bank id"),
            sepaDebit = PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD.sepaDebit,
            code = "card"
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
