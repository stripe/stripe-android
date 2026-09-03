package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.elements.PaymentElement
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet
import kotlin.test.Test

@OptIn(CheckoutSessionPreview::class)
internal class PaymentMethodLayoutMapperTest {
    @Test
    fun `prefer form maps to vertical`() {
        val layout = PaymentElement.Configuration.PaymentMethodLayout.PreferForm.asPaymentSheet()

        assertThat(layout).isEqualTo(PaymentSheet.PaymentMethodLayout.Vertical)
    }
}
