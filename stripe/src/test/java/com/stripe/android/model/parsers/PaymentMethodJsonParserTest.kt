package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import kotlin.test.Test

class PaymentMethodJsonParserTest {

    @Test
    fun parse_withCardWithNetworks_shouldCreateExpectedObject() {
        val actualNetworks =
            PaymentMethodJsonParser().parse(PaymentMethodFixtures.CARD_WITH_NETWORKS_JSON)
                .card?.networks

        assertThat(actualNetworks)
            .isEqualTo(
                PaymentMethod.Card.Networks(
                    available = setOf("network1", "network2"),
                    preferred = "network1"
                )
            )
    }

    @Test
    fun parse_withIdeal_shouldCreateExpectedObject() {
        val expectedPaymentMethod = PaymentMethod(
            id = "pm_123456789",
            created = 1550757934255L,
            liveMode = true,
            type = PaymentMethod.Type.Ideal,
            customerId = "cus_AQsHpvKfKwJDrF",
            billingDetails = PaymentMethodFixtures.BILLING_DETAILS,
            ideal = PaymentMethod.Ideal(
                bank = "my bank",
                bankIdentifierCode = "bank id"
            )
        )

        assertThat(PaymentMethodJsonParser().parse(PaymentMethodFixtures.IDEAL_JSON))
            .isEqualTo(expectedPaymentMethod)
    }

    @Test
    fun parse_withFpx_shouldCreateExpectedObject() {
        assertThat(PaymentMethodJsonParser().parse(PaymentMethodFixtures.FPX_JSON))
            .isEqualTo(PaymentMethodFixtures.FPX_PAYMENT_METHOD)
    }

    @Test
    fun parse_withSepaDebit_shouldCreateExpectedObject() {
        assertThat(PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD)
            .isEqualTo(
                PaymentMethod(
                    type = PaymentMethod.Type.SepaDebit,
                    id = "pm_1FSQaJCR",
                    liveMode = false,
                    created = 1570809799L,
                    sepaDebit = PaymentMethod.SepaDebit(
                        "3704",
                        null,
                        "DE",
                        "vIZc7Ywn0",
                        "3000"
                    ),
                    billingDetails = PaymentMethod.BillingDetails(
                        name = "Jenny Rosen",
                        email = "jrosen@example.com",
                        address = Address()
                    )
                )
            )
    }

    @Test
    fun parse_withCard_shouldReturnExpectedPaymentMethod() {
        assertThat(PaymentMethodJsonParser().parse(PaymentMethodFixtures.CARD_JSON))
            .isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
    }

    @Test
    fun parse_withIdeal_returnsExpectedObject() {
        assertThat(PaymentMethodJsonParser().parse(PaymentMethodFixtures.IDEAL_JSON).type)
            .isEqualTo(PaymentMethod.Type.Ideal)
    }
}
