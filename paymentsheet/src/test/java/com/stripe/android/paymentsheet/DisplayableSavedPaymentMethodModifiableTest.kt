package com.stripe.android.paymentsheet

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
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

        assertThat(
            paymentMethod.isModifiable(
                canUpdateCardExpiryAndBillingDetails = params.canUpdatePaymentMethod,
                canChangeCbc = params.canChangeCbc,
            )
        ).isEqualTo(params.expectedResult)
    }

    @Test
    fun `isModifiable returns false for non-card payment methods`() {
        listOf(
            PaymentMethodFixtures.LINK_PAYMENT_METHOD,
            PaymentMethodFixtures.US_BANK_ACCOUNT,
        ).forEach { paymentMethod ->
            assertThat(
                paymentMethod.isModifiable(
                    canUpdateCardExpiryAndBillingDetails = true,
                    canChangeCbc = true,
                )
            ).isFalse()
        }
    }

    @Test
    fun `isExpired returns false when expiry date is missing`() {
        listOf(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD.card?.copy(
                expiryMonth = null,
                expiryYear = 2099,
            ),
            PaymentMethodFixtures.CARD_PAYMENT_METHOD.card?.copy(
                expiryMonth = 12,
                expiryYear = null,
            ),
            PaymentMethodFixtures.CARD_PAYMENT_METHOD.card?.copy(
                expiryMonth = null,
                expiryYear = null,
            )
        ).forEach { card ->
            assertThat(card?.isExpired()).isFalse()
        }
    }

    @Test
    fun `isModifiable allows cbc updates when expiry date is missing`() {
        listOf(
            PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD.card?.copy(
                expiryMonth = null,
                expiryYear = 2099,
            ),
            PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD.card?.copy(
                expiryMonth = 12,
                expiryYear = null,
            )
        ).forEach { card ->
            val paymentMethod = PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD.copy(
                card = card,
            )

            assertThat(
                paymentMethod.isModifiable(
                    canUpdateCardExpiryAndBillingDetails = false,
                    canChangeCbc = true,
                )
            ).isTrue()
        }
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
