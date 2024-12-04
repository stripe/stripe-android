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
                    selectionMandatory = true,
                    preferred = "network1"
                )
            )
    }

    @Test
    fun parse_withCardWithDisplayBrand_shouldCreateExpectedObject() {
        val actualDisplayBrand =
            PaymentMethodJsonParser().parse(PaymentMethodFixtures.CARD_WITH_DISPLAY_BRAND_JSON)
                .card?.displayBrand

        assertThat(actualDisplayBrand)
            .isEqualTo("cartes_bancaires")
    }

    @Test
    fun parse_withIdeal_shouldCreateExpectedObject() {
        val expectedPaymentMethod = PaymentMethod(
            id = "pm_123456789",
            created = 1550757934255L,
            liveMode = true,
            type = PaymentMethod.Type.Ideal,
            billingDetails = PaymentMethodFixtures.BILLING_DETAILS,
            customerId = "cus_AQsHpvKfKwJDrF",
            ideal = PaymentMethod.Ideal(
                bank = "my bank",
                bankIdentifierCode = "bank id"
            ),
            code = "ideal"
        )

        assertThat(PaymentMethodJsonParser().parse(PaymentMethodFixtures.IDEAL_JSON))
            .isEqualTo(expectedPaymentMethod)
    }

    @Test
    fun parse_withAllowRedisplayUnspecified_shouldCreateExpectedObject() {
        val parsedPaymentMethod = PaymentMethodJsonParser()
            .parse(PaymentMethodFixtures.ALLOW_REDISPLAY_UNSPECIFIED_JSON)

        assertThat(parsedPaymentMethod.allowRedisplay)
            .isEqualTo(PaymentMethod.AllowRedisplay.UNSPECIFIED)
    }

    @Test
    fun parse_withAllowRedisplayLimited_shouldCreateExpectedObject() {
        val parsedPaymentMethod = PaymentMethodJsonParser()
            .parse(PaymentMethodFixtures.ALLOW_REDISPLAY_LIMITED_JSON)

        assertThat(parsedPaymentMethod.allowRedisplay)
            .isEqualTo(PaymentMethod.AllowRedisplay.LIMITED)
    }

    @Test
    fun parse_withAllowRedisplayAlways_shouldCreateExpectedObject() {
        val parsedPaymentMethod = PaymentMethodJsonParser()
            .parse(PaymentMethodFixtures.ALLOW_REDISPLAY_ALWAYS_JSON)

        assertThat(parsedPaymentMethod.allowRedisplay)
            .isEqualTo(PaymentMethod.AllowRedisplay.ALWAYS)
    }

    @Test
    fun parse_withAllowRedisplayFoobar_shouldCreateExpectedObject() {
        val parsedPaymentMethod = PaymentMethodJsonParser()
            .parse(PaymentMethodFixtures.ALLOW_REDISPLAY_FOOBAR_JSON)

        assertThat(parsedPaymentMethod.allowRedisplay).isNull()
    }

    @Test
    fun parse_withFpx_shouldCreateExpectedObject() {
        assertThat(PaymentMethodJsonParser().parse(PaymentMethodFixtures.FPX_JSON))
            .isEqualTo(PaymentMethodFixtures.FPX_PAYMENT_METHOD)
    }

    @Test
    fun parse_withAuBecsDebit_shouldCreateExpectedObject() {
        assertThat(PaymentMethodJsonParser().parse(PaymentMethodFixtures.AU_BECS_DEBIT_JSON))
            .isEqualTo(PaymentMethodFixtures.AU_BECS_DEBIT_PAYMENT_METHOD)
    }

    @Test
    fun parse_withBacsDebit_shouldCreateExpectedObject() {
        assertThat(PaymentMethodJsonParser().parse(PaymentMethodFixtures.BACS_DEBIT_JSON))
            .isEqualTo(PaymentMethodFixtures.BACS_DEBIT_PAYMENT_METHOD)
    }

    @Test
    fun parse_withSepaDebit_shouldCreateExpectedObject() {
        assertThat(PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD)
            .isEqualTo(
                PaymentMethod(
                    id = "pm_1FSQaJCR",
                    created = 1570809799L,
                    liveMode = false,
                    type = PaymentMethod.Type.SepaDebit,
                    billingDetails = PaymentMethod.BillingDetails(
                        name = "Jenny Rosen",
                        email = "jenny.rosen@example.com",
                        address = Address()
                    ),
                    sepaDebit = PaymentMethod.SepaDebit(
                        "3704",
                        null,
                        "DE",
                        "vIZc7Ywn0",
                        "3000"
                    ),
                    code = "sepa_debit"
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

    @Test
    fun parse_withUsBankAccount_returnsExpectedObject() {
        val usBankAccount = PaymentMethodJsonParser().parse(
            PaymentMethodFixtures.US_BANK_ACCOUNT_WITH_FCA
        )

        assertThat(usBankAccount.type)
            .isEqualTo(PaymentMethod.Type.USBankAccount)
        assertThat(usBankAccount.usBankAccount?.financialConnectionsAccount)
            .isEqualTo("fca_111")
    }
}
