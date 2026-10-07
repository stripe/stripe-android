package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.elements.ExpressCheckoutElement
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet
import org.junit.Test

@OptIn(CheckoutSessionPreview::class)
internal class ExpressCheckoutElementLinkConfigurationMapperTest {
    @Test
    fun `asPaymentSheet maps Link configuration`() {
        val configuration = ExpressCheckoutElement.Configuration.LinkConfiguration()
            .display(ExpressCheckoutElement.Configuration.LinkConfiguration.Display.WalletButtonHidden)
            .build()

        val mapped = configuration.asPaymentSheet()

        assertThat(mapped.display).isEqualTo(PaymentSheet.LinkConfiguration.Display.WalletButtonHidden)
    }
}
