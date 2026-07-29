package com.stripe.android.common.nfcscan.scanner.apdu.pdol

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.testing.PaymentIntentFactory
import org.junit.Test

internal class TerminalCountryCodeProducerTest {
    @Test
    fun `produce returns terminal country tag from intent`() {
        val result = TerminalCountryCodeProducer.produce(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(
                    countryCode = "CA"
                )
            ),
        )

        assertThat(result.tag).isEqualTo("9F1A")
        assertThat(result.value).isEqualTo(
            byteArrayOf(0x01, 0x24),
        )
    }

    @Test
    fun `produce returns default terminal country tag when none on intent`() {
        val result = TerminalCountryCodeProducer.produce(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(
                    countryCode = "US"
                )
            ),
        )

        assertThat(result.tag).isEqualTo("9F1A")
        assertThat(result.value).isEqualTo(
            byteArrayOf(0x08, 0x40),
        )
    }
}
