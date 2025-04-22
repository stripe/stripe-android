package com.stripe.android.paymentsheet

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class DisplayableSavedPaymentMethodModifiableTest(private val params: IsModifiableParams) {

    @Test
    fun `test isModifiable logic`() {
        val displayableSavedPaymentMethod = DisplayableSavedPaymentMethod.create(
            displayName = "unused".resolvableString,
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
                card = PaymentMethodFixtures.CARD_PAYMENT_METHOD.card?.copy(
                    networks = PaymentMethod.Card.Networks(params.availableNetworks),
                    expiryYear = if (params.cardExpired) 2005 else 2099
                )
            ),
            isCbcEligible = params.isCbcEligible,
        )

        assertEquals(
            "Expected isModifiable to be ${params.expectedResult} for parameters: " +
                "canUpdatePaymentMethod=${params.canUpdatePaymentMethod}, " +
                "isCbcEligible=${params.isCbcEligible}, " +
                "availableNetworks=${params.availableNetworks}, " +
                "cardExpired=${params.cardExpired}",
            params.expectedResult,
            displayableSavedPaymentMethod.isModifiable(params.canUpdatePaymentMethod)
        )

        assertThat(displayableSavedPaymentMethod.isModifiable(params.canUpdatePaymentMethod))
            .isEqualTo(params.expectedResult)
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
