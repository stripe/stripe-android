package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.model.PaymentSelection
import org.junit.Test

internal class PaymentSelectionTest {
    @Test
    fun testNewCardLast4_returnsLast4_whenCardInfoExists() {
        val createParams = PaymentMethodCreateParams.create(
            PaymentMethodCreateParams.Card("4242424242424242", 1, 2040),
        )
        val card = PaymentSelection.New.Card(
            paymentMethodCreateParams = createParams,
            brand = CardBrand.Visa,
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
        )
        assertThat(card.last4).isEqualTo("4242")
    }

    @Test
    fun testNewCardLast4_returnsEmpty_whenNoCardInfoExists() {
        val createParams = PaymentMethodCreateParams.createAffirm()
        val card = PaymentSelection.New.Card(
            paymentMethodCreateParams = createParams,
            brand = CardBrand.Visa,
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
        )
        assertThat(card.last4).isEqualTo("")
    }
}
