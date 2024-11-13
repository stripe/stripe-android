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
            canRemovePaymentMethods = true,
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
            currentSelection = PaymentSelection.Link,
            nameProvider = { it!!.resolvableString },
            isCbcEligible = false,
            canRemovePaymentMethods = true,
        )

        assertThat(state.selectedItem).isNull()
    }

    @Test
    fun `'isModifiable' is true when multiple networks are available & is CBC eligible`() {
        val paymentMethods = PaymentMethodFixtures.createCards(3).toMutableList()

        val lastPaymentMethodWithNetworks = paymentMethods.removeLast().let { paymentMethod ->
            paymentMethod.copy(
                card = paymentMethod.card?.copy(
                    networks = PaymentMethod.Card.Networks(
                        available = setOf("visa", "cartes_bancaires")
                    )
                )
            )
        }

        paymentMethods.add(lastPaymentMethodWithNetworks)

        val state = PaymentOptionsStateFactory.create(
            paymentMethods = paymentMethods,
            showGooglePay = false,
            showLink = false,
            currentSelection = PaymentSelection.Link,
            nameProvider = { it!!.resolvableString },
            isCbcEligible = true,
            canRemovePaymentMethods = true,
        )

        assertThat(
            (state.items.last() as PaymentOptionsItem.SavedPaymentMethod).isModifiable
        ).isTrue()
    }

    @Test
    fun `isEnabledDuringEditing is true for all saved payment methods when remove is enabled`() {
        testIsEnabledDuringEditingTrueForAllSavedPmsWhenRemoveIsEnabled(useUpdatePaymentMethodScreen = false)
    }

    @Test
    fun `isEnabledDuringEditing is true for all saved payment methods when remove is enabled, using updatePM screen`() {
        testIsEnabledDuringEditingTrueForAllSavedPmsWhenRemoveIsEnabled(useUpdatePaymentMethodScreen = true)
    }

    private fun testIsEnabledDuringEditingTrueForAllSavedPmsWhenRemoveIsEnabled(useUpdatePaymentMethodScreen: Boolean) {
        val state = createPaymentOptionsState(
            paymentMethods = PaymentMethodFixtures.createCards(3),
            canRemovePaymentMethods = true,
        )

        val options = state.items.filterIsInstance<PaymentOptionsItem.SavedPaymentMethod>()

        assertThat(options[1].isEnabledDuringEditing(useUpdatePaymentMethodScreen)).isTrue()
        assertThat(options[2].isEnabledDuringEditing(useUpdatePaymentMethodScreen)).isTrue()
        assertThat(options[2].isEnabledDuringEditing(useUpdatePaymentMethodScreen)).isTrue()
    }

    @Test
    fun `canRemovePaymentMethods is false for all saved payment methods when not using updatePM screen`() {
        val state = createPaymentOptionsState(
            paymentMethods = PaymentMethodFixtures.createCards(3),
            canRemovePaymentMethods = false,
        )

        val options = state.items.filterIsInstance<PaymentOptionsItem.SavedPaymentMethod>()

        assertThat(options[0].isEnabledDuringEditing(useUpdatePaymentMethodScreen = false)).isFalse()
        assertThat(options[1].isEnabledDuringEditing(useUpdatePaymentMethodScreen = false)).isFalse()
        assertThat(options[2].isEnabledDuringEditing(useUpdatePaymentMethodScreen = false)).isFalse()
    }

    @Test
    fun `canRemovePaymentMethods is false for all saved payment methods unless can modify`() {
        val paymentMethods = PaymentMethodFixtures.createCards(3).run {
            val mutablePaymentMethods = toMutableList()
            val updatedPaymentMethod = mutablePaymentMethods[2].run {
                copy(
                    card = card?.copy(
                        networks = PaymentMethod.Card.Networks(
                            available = setOf("visa", "cartes_bancaires")
                        )
                    )
                )
            }

            mutablePaymentMethods[2] = updatedPaymentMethod

            mutablePaymentMethods
        }

        val state = createPaymentOptionsState(
            paymentMethods = paymentMethods,
            canRemovePaymentMethods = false,
        )

        val options = state.items.filterIsInstance<PaymentOptionsItem.SavedPaymentMethod>()

        assertThat(options[0].isEnabledDuringEditing(useUpdatePaymentMethodScreen = false)).isFalse()
        assertThat(options[1].isEnabledDuringEditing(useUpdatePaymentMethodScreen = false)).isFalse()
        assertThat(options[2].isEnabledDuringEditing(useUpdatePaymentMethodScreen = false)).isTrue()
    }

    @Test
    fun `saved payment methods are enabled even when cannot remove when using updatePM screen`() {
        val state = createPaymentOptionsState(
            paymentMethods = PaymentMethodFixtures.createCards(3),
            canRemovePaymentMethods = false,
        )

        val options = state.items.filterIsInstance<PaymentOptionsItem.SavedPaymentMethod>()

        assertThat(options[0].isEnabledDuringEditing(useUpdatePaymentMethodScreen = true)).isTrue()
        assertThat(options[1].isEnabledDuringEditing(useUpdatePaymentMethodScreen = true)).isTrue()
        assertThat(options[2].isEnabledDuringEditing(useUpdatePaymentMethodScreen = true)).isTrue()
    }

    private fun createPaymentOptionsState(
        paymentMethods: List<PaymentMethod>,
        canRemovePaymentMethods: Boolean,
    ): PaymentOptionsState {
        return PaymentOptionsStateFactory.create(
            paymentMethods = paymentMethods,
            showGooglePay = false,
            showLink = false,
            currentSelection = PaymentSelection.Link,
            nameProvider = { it!!.resolvableString },
            isCbcEligible = true,
            canRemovePaymentMethods = canRemovePaymentMethods,
        )
    }
}
