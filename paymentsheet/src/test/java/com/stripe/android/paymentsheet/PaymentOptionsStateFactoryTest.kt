package com.stripe.android.paymentsheet

import com.google.common.truth.Truth.assertThat
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
            nameProvider = { it!! },
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
            nameProvider = { it!! },
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
            nameProvider = { it!! },
            isCbcEligible = true,
            canRemovePaymentMethods = true,
        )

        assertThat(
            (state.items.last() as PaymentOptionsItem.SavedPaymentMethod).isModifiable
        ).isTrue()
    }

    @Test
    fun `'isRemovable' is true when 'canRemovePaymentMethods' is 'true'`() {
        val paymentMethods = PaymentMethodFixtures.createCards(8)

        val state = PaymentOptionsStateFactory.create(
            paymentMethods = paymentMethods,
            showGooglePay = false,
            showLink = false,
            currentSelection = PaymentSelection.Link,
            nameProvider = { it!! },
            isCbcEligible = true,
            canRemovePaymentMethods = true,
        )

        val allAreRemovable = state.items
            .filterIsInstance<PaymentOptionsItem.SavedPaymentMethod>()
            .all { item ->
                item.isRemovable
            }

        assertThat(allAreRemovable).isTrue()
    }

    @Test
    fun `'isRemovable' is false when 'canRemovePaymentMethods' is 'false'`() {
        val paymentMethods = PaymentMethodFixtures.createCards(8)

        val state = PaymentOptionsStateFactory.create(
            paymentMethods = paymentMethods,
            showGooglePay = false,
            showLink = false,
            currentSelection = PaymentSelection.Link,
            nameProvider = { it!! },
            isCbcEligible = true,
            canRemovePaymentMethods = false,
        )

        val noneAreRemovable = state.items
            .filterIsInstance<PaymentOptionsItem.SavedPaymentMethod>()
            .none { item ->
                item.isRemovable
            }

        assertThat(noneAreRemovable).isTrue()
    }

    @Test
    fun `When 'canRemovePaymentMethods' is true, 'isEnabledDuringEditing' is true only for CBC-enabled cards`() {
        val enabledPaymentMethodId = "pm_787"
        val paymentMethods = PaymentMethodFixtures.createCards(4).toMutableList()

        paymentMethods[2] = paymentMethods[2].copy(
            id = enabledPaymentMethodId,
            card = paymentMethods[2].card?.copy(
                networks = PaymentMethod.Card.Networks(
                    available = setOf("visa", "cartes_bancaires")
                )
            )
        )

        val state = PaymentOptionsStateFactory.create(
            paymentMethods = paymentMethods,
            showGooglePay = false,
            showLink = false,
            currentSelection = PaymentSelection.Link,
            nameProvider = { it!! },
            isCbcEligible = true,
            canRemovePaymentMethods = false,
        )

        val onlyCbcPaymentMethodIsEnabled = state.items
            .filterIsInstance<PaymentOptionsItem.SavedPaymentMethod>()
            .all { item ->
                if (item.paymentMethod.id == enabledPaymentMethodId) {
                    item.isEnabledDuringEditing
                } else {
                    !item.isEnabledDuringEditing
                }
            }

        assertThat(onlyCbcPaymentMethodIsEnabled).isTrue()
    }

    @Test
    fun `'isEnabledDuringEditing' is true when 'canRemovePaymentMethods' is true && 'isCbcEligible' is false`() {
        val paymentMethods = PaymentMethodFixtures.createCards(8)

        val state = PaymentOptionsStateFactory.create(
            paymentMethods = paymentMethods,
            showGooglePay = false,
            showLink = false,
            currentSelection = PaymentSelection.Link,
            nameProvider = { it!! },
            isCbcEligible = false,
            canRemovePaymentMethods = true,
        )

        val noneAreRemovable = state.items
            .filterIsInstance<PaymentOptionsItem.SavedPaymentMethod>()
            .all { item ->
                item.isEnabledDuringEditing
            }

        assertThat(noneAreRemovable).isTrue()
    }

    @Test
    fun `'isEnabledDuringEditing' is false when 'canRemovePaymentMethods' is false && 'isCbcEligible' is false`() {
        val paymentMethods = PaymentMethodFixtures.createCards(8)

        val state = PaymentOptionsStateFactory.create(
            paymentMethods = paymentMethods,
            showGooglePay = false,
            showLink = false,
            currentSelection = PaymentSelection.Link,
            nameProvider = { it!! },
            isCbcEligible = false,
            canRemovePaymentMethods = false,
        )

        val noneAreRemovable = state.items
            .filterIsInstance<PaymentOptionsItem.SavedPaymentMethod>()
            .none { item ->
                item.isEnabledDuringEditing
            }

        assertThat(noneAreRemovable).isTrue()
    }
}
