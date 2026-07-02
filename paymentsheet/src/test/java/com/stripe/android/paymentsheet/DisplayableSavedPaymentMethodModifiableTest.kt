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
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
            card = PaymentMethodFixtures.CARD_PAYMENT_METHOD.card?.copy(
                networks = PaymentMethod.Card.Networks(params.availableNetworks),
                expiryYear = if (params.cardExpired) 2005 else 2099
            )
        )
        val displayableSavedPaymentMethod = DisplayableSavedPaymentMethod.create(
            displayName = "unused".resolvableString,
            paymentMethod = paymentMethod,
            isCbcEligible = params.isCbcEligible,
        )

        assertEquals(
            "Expected isModifiable to be ${params.expectedResult} for parameters: " +
                "canUpdatePaymentMethod=${params.canUpdatePaymentMethod}, " +
                "isCbcEligible=${params.isCbcEligible}, " +
                "availableNetworks=${params.availableNetworks}, " +
                "cardExpired=${params.cardExpired}",
            params.expectedResult,
            displayableSavedPaymentMethod.isModifiable(
                canUpdateCardExpiryAndBillingDetails = params.canUpdatePaymentMethod,
                canUpdateCardBrandChoice = params.canUpdateCardBrandChoice,
            )
        )

        assertThat(
            displayableSavedPaymentMethod.isModifiable(
                canUpdateCardExpiryAndBillingDetails = params.canUpdatePaymentMethod,
                canUpdateCardBrandChoice = params.canUpdateCardBrandChoice,
            )
        )
            .isEqualTo(params.expectedResult)

        assertThat(
            paymentMethod.isModifiable(
                canUpdateCardExpiryAndBillingDetails = params.canUpdatePaymentMethod,
                canUpdateCardBrandChoice = params.canUpdateCardBrandChoice,
                isCbcEligible = params.isCbcEligible,
            )
        ).isEqualTo(params.expectedResult)
    }

    @Test
    fun `isModifiable returns false for non-card payment methods`() {
        assertThat(
            PaymentMethodFixtures.LINK_PAYMENT_METHOD.isModifiable(
                canUpdateCardExpiryAndBillingDetails = true,
                canUpdateCardBrandChoice = true,
                isCbcEligible = true,
            )
        ).isFalse()
    }

    data class IsModifiableParams(
        val canUpdatePaymentMethod: Boolean = false,
        val canUpdateCardBrandChoice: Boolean,
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
                canUpdateCardBrandChoice = true,
                expectedResult = true
            ),
            IsModifiableParams(
                canUpdatePaymentMethod = true,
                canUpdateCardBrandChoice = true,
                cardExpired = true,
                expectedResult = true
            ),
            IsModifiableParams(
                canUpdatePaymentMethod = true,
                canUpdateCardBrandChoice = true,
                isCbcEligible = true,
                availableNetworks = setOf("visa", "mastercard"),
                cardExpired = false,
                expectedResult = true
            ),
            IsModifiableParams(
                canUpdateCardBrandChoice = true,
                isCbcEligible = true,
                availableNetworks = setOf("visa", "cartes_bancaires"),
                expectedResult = true
            ),
            IsModifiableParams(
                canUpdateCardBrandChoice = false,
                isCbcEligible = true,
                availableNetworks = setOf("visa", "cartes_bancaires"),
                expectedResult = false
            ),
            IsModifiableParams(
                canUpdateCardBrandChoice = true,
                availableNetworks = setOf("visa", "cartes_bancaires"),
                expectedResult = false
            ),
            IsModifiableParams(
                canUpdateCardBrandChoice = true,
                isCbcEligible = true,
                expectedResult = false
            ),
            IsModifiableParams(
                canUpdateCardBrandChoice = true,
                isCbcEligible = true,
                availableNetworks = setOf("visa", "cartes_bancaires"),
                cardExpired = true,
                expectedResult = false
            ),
            IsModifiableParams(
                canUpdateCardBrandChoice = true,
                cardExpired = true,
                expectedResult = false
            )
        )
    }
}
