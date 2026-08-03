package com.stripe.android.common.nfcscan.scanner.apdu.pdol

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Locale

internal class TerminalCountryCodeProducerTest {
    private lateinit var originalLocale: Locale

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `produce returns terminal country tag for device locale`() {
        Locale.setDefault(Locale.CANADA)

        val result = TerminalCountryCodeProducer.produce(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        )

        assertThat(result.tag).isEqualTo("9F1A")
        assertThat(result.value).isEqualTo(
            byteArrayOf(0x01, 0x24),
        )
    }

    @Test
    fun `produce returns US terminal country tag for US device locale`() {
        Locale.setDefault(Locale.US)

        val result = TerminalCountryCodeProducer.produce(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        )

        assertThat(result.tag).isEqualTo("9F1A")
        assertThat(result.value).isEqualTo(
            byteArrayOf(0x08, 0x40),
        )
    }

    @Test
    fun `produce returns US terminal country tag when device locale country is unmapped`() {
        Locale.setDefault(Locale.Builder().setLanguage("en").setRegion("JO").build())

        val result = TerminalCountryCodeProducer.produce(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        )

        assertThat(result.tag).isEqualTo("9F1A")
        assertThat(result.value).isEqualTo(
            byteArrayOf(0x08, 0x40),
        )
    }
}
