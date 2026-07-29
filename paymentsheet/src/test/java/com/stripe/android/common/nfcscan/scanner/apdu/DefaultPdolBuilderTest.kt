package com.stripe.android.common.nfcscan.scanner.apdu

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.nfcscan.scanner.apdu.pdol.DefaultPdolBuilder
import com.stripe.android.common.nfcscan.scanner.apdu.pdol.FakeTagValueProducer
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.testing.PaymentIntentFactory
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

        assertThat(result).isEqualTo(
            commandTemplateResponse(TERMINAL_TRANSACTION_QUALIFIERS),
        )

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

        assertThat(result).isEqualTo(
            commandTemplateResponse(
                byteArrayOf(0x01, 0x02),
            ),
        )

        assertThat(tagValueProducers.single().produceCalls.awaitItem())
            .isEqualTo(paymentMethodMetadata)
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

        assertThat(result).isEqualTo(
            commandTemplateResponse(
                byteArrayOf(0x00, 0x00, 0x00, 0x00),
            ),
        )

        assertThat(tagValueProducers.single().produceCalls.awaitItem())
            .isEqualTo(paymentMethodMetadata)
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

        assertThat(result).isEqualTo(
            commandTemplateResponse(
                TERMINAL_TRANSACTION_QUALIFIERS + UNPREDICTABLE_NUMBER,
            ),
        )

        tagValueProducers.forEach { producer ->
            assertThat(producer.produceCalls.awaitItem()).isEqualTo(paymentMethodMetadata)
        }
    }

    private fun runScenario(
        tagValueProducers: Set<FakeTagValueProducer>,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFactory.create(amount = 5502)
        )
        val builder = DefaultPdolBuilder(
            tagValueProducers = tagValueProducers,
        )

        Scenario(
            builder = builder,
            paymentMethodMetadata = paymentMethodMetadata,
            tagValueProducers = tagValueProducers,
        ).apply { block() }

        tagValueProducers.forEach {
            it.ensureAllEventsConsumed()
        }
    }

    private class Scenario(
        val builder: DefaultPdolBuilder,
        val paymentMethodMetadata: PaymentMethodMetadata,
        val tagValueProducers: Set<FakeTagValueProducer>,
    )

    private fun commandTemplateResponse(data: ByteArray): ByteArray {
        return byteArrayOf(COMMAND_TEMPLATE_TAG, data.size.toByte()) + data
    }

    private companion object {
        const val COMMAND_TEMPLATE_TAG = 0x83.toByte()

        val TERMINAL_TRANSACTION_QUALIFIERS = byteArrayOf(0x26, 0x00, 0x00, 0x00)
        val UNPREDICTABLE_NUMBER = byteArrayOf(0x01, 0x02, 0x03, 0x04)
    }
}
