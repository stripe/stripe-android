package com.stripe.android.common.nfcscan.scanner.apdu.pdol

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.testing.PaymentIntentFactory
import org.junit.Test

internal class TransactionDateProducerTest {
    @Test
    fun `produce encodes date in UTC not local timezone`() {
        // 2020-01-01 00:30:00 UTC is still 2019-12-31 in US Pacific time.
        val result = TransactionDateProducer.produce(
            paymentMethodMetadata = paymentMethodMetadataWithCreated(CREATED_2020_01_01_0030_UTC),
        )

        assertThat(result.tag).isEqualTo("9A")
        assertThat(result.value).isEqualTo(byteArrayOf(0x20, 0x01, 0x01))
    }

    @Test
    fun `produce encodes century boundary date`() {
        val result = TransactionDateProducer.produce(
            paymentMethodMetadata = paymentMethodMetadataWithCreated(CREATED_2000_01_01_0000_UTC),
        )

        assertThat(result.tag).isEqualTo("9A")
        assertThat(result.value).isEqualTo(byteArrayOf(0x00, 0x01, 0x01))
    }

    @Test
    fun `produce encodes pre century boundary date`() {
        val result = TransactionDateProducer.produce(
            paymentMethodMetadata = paymentMethodMetadataWithCreated(CREATED_1999_12_31_2359_UTC),
        )

        assertThat(result.tag).isEqualTo("9A")
        assertThat(result.value).isEqualTo(byteArrayOf(0x99.toByte(), 0x12, 0x31))
    }

    @Test
    fun `produce encodes leap day date`() {
        val result = TransactionDateProducer.produce(
            paymentMethodMetadata = paymentMethodMetadataWithCreated(CREATED_2020_02_29_1200_UTC),
        )

        assertThat(result.tag).isEqualTo("9A")
        assertThat(result.value).isEqualTo(byteArrayOf(0x20, 0x02, 0x29))
    }

    private fun paymentMethodMetadataWithCreated(created: Long) =
        PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFactory.create().copy(created = created),
        )

    private companion object {
        // 2020-01-01 00:30:00 UTC
        const val CREATED_2020_01_01_0030_UTC = 1577838600L

        // 2000-01-01 00:00:00 UTC
        const val CREATED_2000_01_01_0000_UTC = 946684800L

        // 1999-12-31 23:59:59 UTC
        const val CREATED_1999_12_31_2359_UTC = 946684799L

        // 2020-02-29 12:00:00 UTC
        const val CREATED_2020_02_29_1200_UTC = 1582977600L
    }
}
