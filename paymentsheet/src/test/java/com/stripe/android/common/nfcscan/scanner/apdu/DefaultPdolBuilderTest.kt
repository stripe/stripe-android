package com.stripe.android.common.nfcscan.scanner.apdu

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.nfcscan.scanner.apdu.pdol.DefaultPdolBuilder
import com.stripe.android.common.nfcscan.scanner.apdu.pdol.FakeTagValueProducer
import com.stripe.android.common.nfcscan.scanner.apdu.pdol.TagValueProducer
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class DefaultPdolBuilderTest {
    @Test
    fun `fromTemplate returns empty payload when template is empty`() = runScenario(
        tagValueProducers = emptySet(),
    ) {
        val result = builder.fromTemplate(
            paymentMethodMetadata = paymentMethodMetadata,
            template = byteArrayOf(),
        )

        assertThat(result).isEmpty()
    }

    @Test
    fun `fromTemplate encodes producer value for matching template tag`() = runScenario(
        tagValueProducers = setOf(
            FakeTagValueProducer(
                tag = "9F66",
                value = TERMINAL_TRANSACTION_QUALIFIERS,
            ),
        ),
    ) {
        val template = byteArrayOf(
            0x9F.toByte(), 0x66, 0x04,
        )

        val result = builder.fromTemplate(
            paymentMethodMetadata = paymentMethodMetadata,
            template = template,
        )

        val pdolResponse = extractCommandTemplateValue(result)
        assertThat(pdolResponse).isEqualTo(TERMINAL_TRANSACTION_QUALIFIERS)

        val producer = tagValueProducers.single() as FakeTagValueProducer
        assertThat(producer.produceCalls.awaitItem()).isEqualTo(paymentMethodMetadata)
    }

    @Test
    fun `fromTemplate truncates producer value to template length`() = runScenario(
        tagValueProducers = setOf(
            FakeTagValueProducer(
                tag = "9F37",
                value = UNPREDICTABLE_NUMBER,
            ),
        ),
    ) {
        val template = byteArrayOf(
            0x9F.toByte(), 0x37, 0x02,
        )

        val result = builder.fromTemplate(
            paymentMethodMetadata = paymentMethodMetadata,
            template = template,
        )

        val pdolResponse = extractCommandTemplateValue(result)
        assertThat(pdolResponse).isEqualTo(
            byteArrayOf(0x01, 0x02),
        )

        val producer = tagValueProducers.single() as FakeTagValueProducer
        assertThat(producer.produceCalls.awaitItem()).isEqualTo(paymentMethodMetadata)
    }

    @Test
    fun `fromTemplate zero pads unknown template tags`() = runScenario(
        tagValueProducers = setOf(
            FakeTagValueProducer(
                tag = "9F66",
                value = TERMINAL_TRANSACTION_QUALIFIERS,
            ),
        ),
    ) {
        val template = byteArrayOf(0x9F.toByte(), 0x15, 0x04)

        val result = builder.fromTemplate(
            paymentMethodMetadata = paymentMethodMetadata,
            template = template,
        )

        val pdolResponse = extractCommandTemplateValue(result)
        assertThat(pdolResponse).isEqualTo(byteArrayOf(0x00, 0x00, 0x00, 0x00))

        val producer = tagValueProducers.single() as FakeTagValueProducer
        assertThat(producer.produceCalls.awaitItem()).isEqualTo(paymentMethodMetadata)
    }

    private fun runScenario(
        tagValueProducers: Set<TagValueProducer>,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val paymentMethodMetadata = defaultPaymentMethodMetadata()
        val builder = DefaultPdolBuilder(
            tagValueProducers = tagValueProducers,
        )

        Scenario(
            builder = builder,
            paymentMethodMetadata = paymentMethodMetadata,
            tagValueProducers = tagValueProducers,
        ).apply { block() }

        tagValueProducers.filterIsInstance<FakeTagValueProducer>().forEach {
            it.ensureAllEventsConsumed()
        }
    }

    private class Scenario(
        val builder: DefaultPdolBuilder,
        val paymentMethodMetadata: PaymentMethodMetadata,
        val tagValueProducers: Set<TagValueProducer>,
    )

    private fun extractCommandTemplateValue(data: ByteArray): ByteArray {
        val length = when {
            data[1].toInt() and 0x80 == 0 -> data[1].toInt() and 0xFF
            data[1] == 0x81.toByte() -> data[2].toInt() and 0xFF
            else -> ((data[2].toInt() and 0xFF) shl 8) or (data[3].toInt() and 0xFF)
        }
        val valueStartIndex = when {
            data[1].toInt() and 0x80 == 0 -> 2
            data[1] == 0x81.toByte() -> 3
            else -> 4
        }
        return data.copyOfRange(valueStartIndex, valueStartIndex + length)
    }

    private companion object {
        val TERMINAL_TRANSACTION_QUALIFIERS = byteArrayOf(0x26, 0x00, 0x00, 0x00)
        val UNPREDICTABLE_NUMBER = byteArrayOf(0x01, 0x02, 0x03, 0x04)
    }
}
