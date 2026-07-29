package com.stripe.android.common.nfcscan.scanner.apdu.pdol

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.SetupIntentFixtures
import org.junit.Test

internal class TransactionCurrencyCodeProducerTest {
    @Test
    fun `produce returns transaction currency tag & bcd encoded numeric currency code`() {
        val result = TransactionCurrencyCodeProducer.produce(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        )

        assertThat(result.tag).isEqualTo("5F2A")
        assertThat(result.value).isEqualTo(
            byteArrayOf(0x08, 0x40),
        )
    }

    @Test
    fun `produce returns zero currency code when payment intent currency is unavailable`() {
        val result = TransactionCurrencyCodeProducer.produce(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
            ),
        )

        assertThat(result.tag).isEqualTo("5F2A")
        assertThat(result.value).isEqualTo(
            byteArrayOf(0x00, 0x00),
        )
    }
}
