package com.stripe.android.paymentsheet

import com.google.common.truth.Truth.assertThat
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
        )

        assertThat(state.selectedItem).isNull()
    }
}
