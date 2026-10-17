package com.stripe.android.common.nfcscan.scanner.apdu.pdol

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import org.junit.Test

internal class EnhancedContactlessReaderCapabilitiesProducerTest {
    @Test
    fun `produce returns enhanced contactless reader capabilities for online-only mobile reader`() {
        val result = EnhancedContactlessReaderCapabilitiesProducer.produce(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        )

        assertThat(result.tag).isEqualTo("9F6E")
        assertThat(result.value).isEqualTo(
            byteArrayOf(0x18, 0x80.toByte(), 0x00, 0x03),
        )
    }
}
