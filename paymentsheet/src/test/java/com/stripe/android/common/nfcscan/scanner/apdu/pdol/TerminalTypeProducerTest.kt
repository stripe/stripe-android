package com.stripe.android.common.nfcscan.scanner.apdu.pdol

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import org.junit.Test

internal class TerminalTypeProducerTest {
    @Test
    fun `produce returns terminal type for online-only cardholder-controlled device`() {
        val result = TerminalTypeProducer.produce(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        )

        assertThat(result.tag).isEqualTo("9F35")
        assertThat(result.value).isEqualTo(
            byteArrayOf(0x34),
        )
    }
}
