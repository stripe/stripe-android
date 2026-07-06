package com.stripe.android.paymentsheet

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class SavedPaymentMethodEditabilityTest(private val params: IsModifiableParams) {

    @Test
    fun `test isModifiable logic`() {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
            card = PaymentMethodFixtures.CARD_PAYMENT_METHOD.card?.copy(
                networks = PaymentMethod.Card.Networks(params.availableNetworks),
                expiryYear = if (params.cardExpired) 2005 else 2099
            )
        )

        assertWithMessage(
            "Expected isModifiable for parameters: " +
                "canUpdatePaymentMethod=${params.canUpdatePaymentMethod}, " +
                "isCbcEligible=${params.isCbcEligible}, " +
                "availableNetworks=${params.availableNetworks}, " +
                "cardExpired=${params.cardExpired}"
        ).that(
            paymentMethod.isModifiable(
                canUpdateFullPaymentMethodDetails = params.canUpdatePaymentMethod,
                isCbcEligible = params.isCbcEligible,
            )
        ).isEqualTo(params.expectedResult)
    }

    data class IsModifiableParams(
        val canUpdatePaymentMethod: Boolean = false,
        val isCbcEligible: Boolean = false,
        val availableNetworks: Set<String> = setOf("visa"),
        val cardExpired: Boolean = false,
        val expectedResult: Boolean,
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun testCases() = listOf(
            IsModifiableParams(
                canUpdatePaymentMethod = true,
                isCbcEligible = false,
                availableNetworks = setOf("visa"),
                cardExpired = false,
                expectedResult = true
            ),
            IsModifiableParams(
                canUpdatePaymentMethod = true,
                isCbcEligible = false,
                availableNetworks = setOf("visa"),
                cardExpired = true,
                expectedResult = true
            ),
            IsModifiableParams(
                canUpdatePaymentMethod = true,
                isCbcEligible = true,
                availableNetworks = setOf("visa", "mastercard"),
                cardExpired = false,
                expectedResult = true
            ),
            IsModifiableParams(
                canUpdatePaymentMethod = false,
                isCbcEligible = true,
                availableNetworks = setOf("visa", "cartes_bancaires"),
                cardExpired = false,
                expectedResult = true
            ),
            IsModifiableParams(
                canUpdatePaymentMethod = false,
                isCbcEligible = false,
                availableNetworks = setOf("visa", "cartes_bancaires"),
                cardExpired = false,
                expectedResult = false
            ),
            IsModifiableParams(
                canUpdatePaymentMethod = false,
                isCbcEligible = true,
                availableNetworks = setOf("visa"),
                cardExpired = false,
                expectedResult = false
            ),
            IsModifiableParams(
                canUpdatePaymentMethod = false,
                isCbcEligible = true,
                availableNetworks = setOf("visa", "cartes_bancaires"),
                cardExpired = true,
                expectedResult = false
            ),
            IsModifiableParams(
                canUpdatePaymentMethod = false,
                isCbcEligible = false,
                availableNetworks = setOf("visa"),
                cardExpired = true,
                expectedResult = false
            )
        )
    }
}

class SavedPaymentMethodEditabilityStandaloneTest {
    @Test
    fun `canChangeCbc returns false for single network card when cbc eligible`() {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
            card = PaymentMethodFixtures.CARD_PAYMENT_METHOD.card?.copy(
                networks = PaymentMethod.Card.Networks(setOf("visa"))
            )
        )

        assertThat(paymentMethod.canChangeCbc(isCbcEligible = true)).isFalse()
    }

    @Test
    fun `canChangeCbc returns false for multiple network card when cbc not eligible`() {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
            card = PaymentMethodFixtures.CARD_PAYMENT_METHOD.card?.copy(
                networks = PaymentMethod.Card.Networks(setOf("visa", "cartes_bancaires"))
            )
        )

        assertThat(paymentMethod.canChangeCbc(isCbcEligible = false)).isFalse()
    }

    @Test
    fun `canChangeCbc returns true for multiple network card when cbc eligible`() {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
            card = PaymentMethodFixtures.CARD_PAYMENT_METHOD.card?.copy(
                networks = PaymentMethod.Card.Networks(setOf("visa", "cartes_bancaires"))
            )
        )

        assertThat(paymentMethod.canChangeCbc(isCbcEligible = true)).isTrue()
    }

    @Test
    fun `canChangeCbc returns false for non-card payment methods`() {
        assertThat(PaymentMethodFixtures.US_BANK_ACCOUNT.canChangeCbc(isCbcEligible = true)).isFalse()
    }

    @Test
    fun `isModifiable returns false for non-card payment methods`() {
        listOf(
            PaymentMethodFixtures.LINK_PAYMENT_METHOD,
            PaymentMethodFixtures.US_BANK_ACCOUNT,
        ).forEach { paymentMethod ->
            assertThat(
                paymentMethod.isModifiable(
                    canUpdateFullPaymentMethodDetails = true,
                    isCbcEligible = true,
                )
            ).isFalse()
        }
    }
}
