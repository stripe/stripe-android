package com.stripe.android.common.nfcscan.scanner.apdu

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.nfcscan.scanner.apdu.pdol.DefaultPdolBuilder
import com.stripe.android.common.nfcscan.scanner.apdu.pdol.FakeTagValueProducer
import com.stripe.android.common.nfcscan.scanner.apdu.pdol.PdolParsingException
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.testing.PaymentIntentFactory
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class DefaultPdolBuilderTest {
    @Test
    fun `fromTemplate throws PdolParsingException for malformed template`() = runScenario(
        tagValueProducers = emptySet(),
    ) {
        val malformedTemplate = byteArrayOf(0x9F.toByte())

        val exception = runCatching {
            builder.fromTemplate(
                paymentMethodMetadata = paymentMethodMetadata,
                template = malformedTemplate,
            )
        }.exceptionOrNull()

        assertThat(exception).isInstanceOf(PdolParsingException::class.java)
    }

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
    fun `fromTemplate encodes producer value when value length matches template`() = runScenario(
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

        assertThat(result).isEqualTo(byteArrayOf(0x83.toByte(), 0x04, 0x20, 0x00, 0x40, 0x00))
    }

    @Test
    fun `fromTemplate zero pads when producer value length does not match template`() = runScenario(
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

        assertThat(result).isEqualTo(byteArrayOf(0x83.toByte(), 0x02, 0x00, 0x00))
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

        assertThat(result).isEqualTo(byteArrayOf(0x83.toByte(), 0x04, 0x00, 0x00, 0x00, 0x00))
    }

    @Test
    fun `fromTemplate concatenates values from multiple producers in template order`() = runScenario(
        tagValueProducers = setOf(
            FakeTagValueProducer(
                tag = "9F66",
                value = TERMINAL_TRANSACTION_QUALIFIERS,
            ),
            FakeTagValueProducer(
                tag = "9F37",
                value = UNPREDICTABLE_NUMBER,
            ),
        ),
    ) {
        val template = byteArrayOf(
            0x9F.toByte(), 0x66, 0x04,
            0x9F.toByte(), 0x37, 0x04,
        )

        val result = builder.fromTemplate(
            paymentMethodMetadata = paymentMethodMetadata,
            template = template,
        )

        assertThat(result)
            .isEqualTo(byteArrayOf(0x83.toByte(), 0x08, 0x20, 0x00, 0x40, 0x00, 0x01, 0x02, 0x03, 0x04))
    }

    @Test
    fun `fromTemplate uses long form length encoding for large pdol data`() = runScenario(
        tagValueProducers = emptySet(),
    ) {
        val template = byteArrayOf(
            0x9F.toByte(), 0x15, 0x80.toByte(),
        )

        val result = builder.fromTemplate(
            paymentMethodMetadata = paymentMethodMetadata,
            template = template,
        )

        assertThat(result).isEqualTo(
            byteArrayOf(0x83.toByte(), 0x81.toByte(), 0x80.toByte()) + ByteArray(0x80),
        )
    }

    private fun runScenario(
        tagValueProducers: Set<FakeTagValueProducer>,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFactory.create(amount = 5502),
        )
        val builder = DefaultPdolBuilder(
            tagValueProducers = tagValueProducers,
        )

        Scenario(
            builder = builder,
            paymentMethodMetadata = paymentMethodMetadata,
        ).apply { block() }

        tagValueProducers.forEach { producer ->
            assertThat(producer.produceCalls.awaitItem()).isEqualTo(paymentMethodMetadata)
        }

        tagValueProducers.forEach {
            it.ensureAllEventsConsumed()
        }
    }

    private class Scenario(
        val builder: DefaultPdolBuilder,
        val paymentMethodMetadata: PaymentMethodMetadata,
    )

    private companion object {
        val TERMINAL_TRANSACTION_QUALIFIERS = byteArrayOf(0x20, 0x00, 0x40, 0x00)
        val UNPREDICTABLE_NUMBER = byteArrayOf(0x01, 0x02, 0x03, 0x04)
    }
}
