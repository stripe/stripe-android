package com.stripe.android.common.nfcscan.scanner.apdu.pdol

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import org.junit.Test

internal class TerminalTransactionQualifiersProducerTest {
    @Test
    fun `produce returns terminal transaction qualifiers tag & contactless read only qualifiers`() {
        val result = TerminalTransactionQualifiersProducer.produce(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        )

        assertThat(result.tag).isEqualTo("9F66")
        assertThat(result.value).isEqualTo(
            byteArrayOf(0x26, 0x00, 0x00, 0x00),
        )
    }
}
