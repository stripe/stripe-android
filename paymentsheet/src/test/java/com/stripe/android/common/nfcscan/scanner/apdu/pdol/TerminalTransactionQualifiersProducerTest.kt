package com.stripe.android.common.nfcscan.scanner.apdu.pdol

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import org.junit.Test

internal class TerminalTransactionQualifiersProducerTest {
    @Test
    fun `produce returns terminal transaction qualifiers for online-only mobile reader`() {
        val result = TerminalTransactionQualifiersProducer.produce(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        )

        assertThat(result.tag).isEqualTo("9F66")
        assertThat(result.value).isEqualTo(
            byteArrayOf(0x20, 0x00, 0x40, 0x00),
        )
    }
}
