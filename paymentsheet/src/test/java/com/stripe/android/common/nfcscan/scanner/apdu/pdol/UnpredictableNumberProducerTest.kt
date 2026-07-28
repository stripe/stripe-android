package com.stripe.android.common.nfcscan.scanner.apdu.pdol

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.nfcscan.scanner.apdu.defaultPaymentMethodMetadata
import org.junit.Test

internal class UnpredictableNumberProducerTest {
    @Test
    fun `produce returns unpredictable number tag & 4 byte number`() {
        val result = UnpredictableNumberProducer.produce(
            paymentMethodMetadata = defaultPaymentMethodMetadata(),
        )

        assertThat(result.tag).isEqualTo("9F37")
        assertThat(result.value).hasLength(4)
    }

    @Test
    fun `produce returns different values on subsequent calls`() {
        val first = UnpredictableNumberProducer.produce(
            paymentMethodMetadata = defaultPaymentMethodMetadata(),
        )

        val second = UnpredictableNumberProducer.produce(
            paymentMethodMetadata = defaultPaymentMethodMetadata(),
        )

        assertThat(first.value.contentEquals(second.value)).isFalse()
    }
}
