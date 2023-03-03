package com.stripe.android.paymentsheet

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import org.junit.Test

class PaymentOptionsStateFactoryTest {

    @Test
    fun `Defaults to first customer payment method if there's no saved selection`() {
        val paymentMethods = PaymentMethodFixtures.createCards(3)
        val state = PaymentOptionsStateFactory.create(
            paymentMethods = paymentMethods,
            showGooglePay = false,
            showLink = false,
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
        val paymentMethod = paymentMethods[1]

        val state = PaymentOptionsStateFactory.create(
            paymentMethods = paymentMethods,
            showGooglePay = false,
            showLink = false,
            currentSelection = PaymentSelection.Saved(paymentMethod = paymentMethod),
            nameProvider = { it!! },
        )

        val expectedItem = PaymentOptionsItem.SavedPaymentMethod(
            displayName = "card",
            paymentMethod = paymentMethods[1],
        )
        assertThat(state.selectedItem).isEqualTo(expectedItem)
    }
}
