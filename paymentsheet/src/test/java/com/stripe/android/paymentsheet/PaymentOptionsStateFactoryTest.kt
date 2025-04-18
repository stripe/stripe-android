package com.stripe.android.paymentsheet

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import org.junit.Test

class PaymentOptionsStateFactoryTest {

    @Test
    fun `Returns current selection if available`() {
        val paymentMethods = PaymentMethodFixtures.createCards(3)
        val paymentMethod = paymentMethods.random()

        val state = PaymentOptionsStateFactory.create(
            paymentMethods = paymentMethods,
            showGooglePay = true,
            showLink = true,
            currentSelection = PaymentSelection.Saved(paymentMethod),
            nameProvider = { it!!.resolvableString },
            isCbcEligible = false,
            defaultPaymentMethodId = null,
        )

        val selectedPaymentMethod = state.selectedItem as? PaymentOptionsItem.SavedPaymentMethod
        assertThat(selectedPaymentMethod?.paymentMethod).isEqualTo(paymentMethod)
    }

    @Test
    fun `Returns no payment selection if the current selection is no longer available`() {
        val paymentMethods = PaymentMethodFixtures.createCards(3)

        val state = PaymentOptionsStateFactory.create(
            paymentMethods = paymentMethods,
            showGooglePay = false,
            showLink = false,
            currentSelection = PaymentSelection.Link(),
            nameProvider = { it!!.resolvableString },
            isCbcEligible = false,
            defaultPaymentMethodId = null,
        )

        assertThat(state.selectedItem).isNull()
    }

    @Test
    fun `'isModifiable' is true when canUpdatePaymentMethod is false and cbc can change`() {
        helperIsModifiable(
            canUpdatePaymentMethod = false,
            isCbcEligible = true,
            availableNetworks = setOf("visa", "cartes_bancaires"),
            expectedResult = true
        )
    }

    @Test
    fun `'isModifiable' is true when canUpdatePaymentMethod is true and cbc cannot change`() {
        helperIsModifiable(
            canUpdatePaymentMethod = true,
            isCbcEligible = false,
            availableNetworks = setOf("visa"),
            expectedResult = true
        )
    }

    @Test
    fun `'isModifiable' is false when canUpdatePaymentMethod is false and cbc cannot change`() {
        helperIsModifiable(
            canUpdatePaymentMethod = false,
            isCbcEligible = false,
            availableNetworks = setOf("visa"),
            expectedResult = false
        )
    }

    private fun helperIsModifiable(
        canUpdatePaymentMethod: Boolean = false,
        isCbcEligible: Boolean = false,
        availableNetworks: Set<String> = emptySet(),
        expectedResult: Boolean
    ) {
        val paymentMethods = PaymentMethodFixtures.createCards(3).toMutableList()

        val lastPaymentMethodWithNetworks = paymentMethods.removeAt(paymentMethods.lastIndex).let { paymentMethod ->
            paymentMethod.copy(
                card = paymentMethod.card?.copy(
                    networks = PaymentMethod.Card.Networks(
                        available = availableNetworks
                    )
                )
            )
        }

        paymentMethods.add(lastPaymentMethodWithNetworks)

        val state = PaymentOptionsStateFactory.create(
            paymentMethods = paymentMethods,
            showGooglePay = false,
            showLink = false,
            currentSelection = PaymentSelection.Link(),
            nameProvider = { it!!.resolvableString },
            isCbcEligible = isCbcEligible,
            defaultPaymentMethodId = null,
        )

        assertThat(
            (state.items.last() as PaymentOptionsItem.SavedPaymentMethod).isModifiable(canUpdatePaymentMethod)
        ).isEqualTo(expectedResult)
    }

    @Test
    fun `isEnabledDuringEditing is true for all saved payment methods when remove is enabled, using updatePM screen`() {
        val state = createPaymentOptionsState(
            paymentMethods = PaymentMethodFixtures.createCards(3),
        )

        val options = state.items.filterIsInstance<PaymentOptionsItem.SavedPaymentMethod>()

        assertThat(options[1].isEnabledDuringEditing).isTrue()
        assertThat(options[2].isEnabledDuringEditing).isTrue()
        assertThat(options[2].isEnabledDuringEditing).isTrue()
    }

    @Test
    fun `saved payment methods are enabled even when cannot remove when using updatePM screen`() {
        val state = createPaymentOptionsState(
            paymentMethods = PaymentMethodFixtures.createCards(3),
        )

        val options = state.items.filterIsInstance<PaymentOptionsItem.SavedPaymentMethod>()

        assertThat(options[0].isEnabledDuringEditing).isTrue()
        assertThat(options[1].isEnabledDuringEditing).isTrue()
        assertThat(options[2].isEnabledDuringEditing).isTrue()
    }

    @Test
    fun `State is correct when given non null defaultPaymentMethodId`() {
        val paymentMethods = PaymentMethodFixtures.createCards(3)

        val defaultPaymentMethod = paymentMethods[0]
        val defaultPaymentMethodId = defaultPaymentMethod.id

        assertThat(defaultPaymentMethodId).isNotNull()

        val state = PaymentOptionsStateFactory.create(
            paymentMethods = paymentMethods,
            showGooglePay = true,
            showLink = true,
            currentSelection = PaymentSelection.Saved(defaultPaymentMethod),
            nameProvider = { it!!.resolvableString },
            isCbcEligible = false,
            defaultPaymentMethodId = defaultPaymentMethodId
        )

        val options = state.items.filterIsInstance<PaymentOptionsItem.SavedPaymentMethod>()

        assertThat(options[0].paymentMethod.id).isEqualTo(defaultPaymentMethodId)
        assertThat(options[0].displayableSavedPaymentMethod.shouldShowDefaultBadge).isTrue()
        assertThat(options[1].displayableSavedPaymentMethod.shouldShowDefaultBadge).isFalse()
        assertThat(options[2].displayableSavedPaymentMethod.shouldShowDefaultBadge).isFalse()
    }

    private fun createPaymentOptionsState(
        paymentMethods: List<PaymentMethod>,
    ): PaymentOptionsState {
        return PaymentOptionsStateFactory.create(
            paymentMethods = paymentMethods,
            showGooglePay = false,
            showLink = false,
            currentSelection = PaymentSelection.Link(),
            nameProvider = { it!!.resolvableString },
            isCbcEligible = true,
            defaultPaymentMethodId = null,
        )
    }
}
