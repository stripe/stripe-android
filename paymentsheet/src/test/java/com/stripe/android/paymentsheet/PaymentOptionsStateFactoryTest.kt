package com.stripe.android.paymentsheet

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import org.junit.Test

class PaymentOptionsStateFactoryTest {

    @Test
    fun `Defaults to Google Pay if Google Pay and Link are available`() {
        val paymentMethods = PaymentMethodFixtures.createCards(3)
        val state = PaymentOptionsStateFactory.create(
            paymentMethods = paymentMethods,
            showGooglePay = true,
            showLink = true,
            initialSelection = SavedSelection.None,
            currentSelection = null,
            nameProvider = { it!! },
        )
        assertThat(state.selectedItem).isEqualTo(PaymentOptionsItem.GooglePay)
    }

    @Test
    fun `Defaults to Link if Google Pay is not available`() {
        val paymentMethods = PaymentMethodFixtures.createCards(3)
        val state = PaymentOptionsStateFactory.create(
            paymentMethods = paymentMethods,
            showGooglePay = false,
            showLink = true,
            initialSelection = SavedSelection.None,
            currentSelection = null,
            nameProvider = { it!! },
        )
        assertThat(state.selectedItem).isEqualTo(PaymentOptionsItem.Link)
    }

    @Test
    fun `Defaults to first saved payment method if Google Pay and Link aren't available`() {
        val paymentMethods = PaymentMethodFixtures.createCards(3)
        val state = PaymentOptionsStateFactory.create(
            paymentMethods = paymentMethods,
            showGooglePay = false,
            showLink = false,
            initialSelection = SavedSelection.None,
            currentSelection = null,
            nameProvider = { it!! },
        )

        val expectedItem = PaymentOptionsItem.SavedPaymentMethod(
            displayName = "card",
            paymentMethod = paymentMethods.first(),
        )
        assertThat(state.selectedItem).isEqualTo(expectedItem)
    }

    @Test
    fun `Defaults to saved selection if available`() {
        val paymentMethods = PaymentMethodFixtures.createCards(3)
        val savedPaymentMethod = SavedSelection.PaymentMethod(id = paymentMethods[1].id!!)

        val state = PaymentOptionsStateFactory.create(
            paymentMethods = paymentMethods,
            showGooglePay = false,
            showLink = false,
            initialSelection = savedPaymentMethod,
            currentSelection = null,
            nameProvider = { it!! },
        )

        val expectedItem = PaymentOptionsItem.SavedPaymentMethod(
            displayName = "card",
            paymentMethod = paymentMethods[1],
        )
        assertThat(state.selectedItem).isEqualTo(expectedItem)
    }

    @Test
    fun `Uses current selection over initial selection`() {
        val paymentMethods = PaymentMethodFixtures.createCards(3)

        val state = PaymentOptionsStateFactory.create(
            paymentMethods = paymentMethods,
            showGooglePay = true,
            showLink = true,
            initialSelection = SavedSelection.GooglePay,
            currentSelection = PaymentSelection.Link,
            nameProvider = { it!! },
        )

        assertThat(state.selectedItem).isEqualTo(PaymentOptionsItem.Link)
    }
}
