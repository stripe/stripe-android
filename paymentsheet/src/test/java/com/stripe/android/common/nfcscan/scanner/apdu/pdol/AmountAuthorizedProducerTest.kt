package com.stripe.android.common.nfcscan.scanner.apdu.pdol

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.SetupIntentFixtures
import org.junit.Test

internal class AmountAuthorizedProducerTest {
    @Test
    fun `produce returns amount authorized tag & bcd encoded payment intent amount`() {
        val result = AmountAuthorizedProducer.produce(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        )

        assertThat(result.tag).isEqualTo("9F02")
        assertThat(result.value).isEqualTo(
            byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x10, 0x99.toByte()),
        )
    }

    @Test
    fun `produce returns zero amount when payment intent amount is unavailable`() {
        val result = AmountAuthorizedProducer.produce(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
            ),
        )

        assertThat(result.tag).isEqualTo("9F02")
        assertThat(result.value).isEqualTo(
            byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00),
        )
    }
}
