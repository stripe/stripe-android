package com.stripe.android.paymentsheet

import com.google.common.truth.Truth.assertThat
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

        assertEquals(
            "Expected isModifiable to be ${params.expectedResult} for parameters: " +
                "canUpdatePaymentMethod=${params.canUpdatePaymentMethod}, " +
                "canChangeCbc=${params.canChangeCbc}, " +
                "availableNetworks=${params.availableNetworks}, " +
                "cardExpired=${params.cardExpired}",
            params.expectedResult,
            paymentMethod.isModifiable(
                canUpdateCardExpiryAndBillingDetails = params.canUpdatePaymentMethod,
                canChangeCbc = params.canChangeCbc,
            )
        )

        assertThat(
            paymentMethod.isModifiable(
                canUpdateCardExpiryAndBillingDetails = params.canUpdatePaymentMethod,
                canChangeCbc = params.canChangeCbc,
            )
        ).isEqualTo(params.expectedResult)
    }

    @Test
    fun `isModifiable returns false for non-card payment methods`() {
        assertThat(
            PaymentMethodFixtures.LINK_PAYMENT_METHOD.isModifiable(
                canUpdateCardExpiryAndBillingDetails = true,
                canChangeCbc = true,
            )
        ).isFalse()
    }

    data class IsModifiableParams(
        val canUpdatePaymentMethod: Boolean = false,
        val canChangeCbc: Boolean,
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
                canChangeCbc = true,
                expectedResult = true
            ),
            IsModifiableParams(
                canUpdatePaymentMethod = true,
                canChangeCbc = true,
                cardExpired = true,
                expectedResult = true
            ),
            IsModifiableParams(
                canUpdatePaymentMethod = true,
                canChangeCbc = true,
                availableNetworks = setOf("visa", "mastercard"),
                cardExpired = false,
                expectedResult = true
            ),
            IsModifiableParams(
                canChangeCbc = true,
                availableNetworks = setOf("visa", "cartes_bancaires"),
                expectedResult = true
            ),
            IsModifiableParams(
                canChangeCbc = false,
                availableNetworks = setOf("visa", "cartes_bancaires"),
                expectedResult = false
            ),
            IsModifiableParams(
                canChangeCbc = true,
                expectedResult = false
            ),
            IsModifiableParams(
                canChangeCbc = true,
                availableNetworks = setOf("visa", "cartes_bancaires"),
                cardExpired = true,
                expectedResult = false
            ),
            IsModifiableParams(
                canChangeCbc = true,
                cardExpired = true,
                expectedResult = false
            )
        )
    }
}
