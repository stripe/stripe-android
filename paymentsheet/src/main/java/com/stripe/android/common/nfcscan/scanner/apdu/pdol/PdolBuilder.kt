package com.stripe.android.common.nfcscan.scanner.apdu.pdol

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import javax.inject.Inject

internal interface PdolBuilder {
    fun fromTemplate(
        paymentMethodMetadata: PaymentMethodMetadata,
        template: ByteArray,
    ): ByteArray
}

/**
 * Produces data from a PDOL template. See
 * [here](https://www.openscdp.org/scripts/tutorial/emv/initiateapplicationprocess.html).
 *
 * The template is a byte array of DOL entries: each entry is a tag
 * plus the length of the value the reader must supply. The card provides this template in FCI
 * tag `9F38` during application selection
 * (done in [com.stripe.android.common.nfcscan.scanner.apdu.SelectApplicationCommand]).
 *
 * The builder returns command data for [com.stripe.android.common.nfcscan.scanner.apdu.GetProcessingOptionsCommand]:
 * tag `83` followed by the concatenated terminal values in PDOL order.
 */
internal class DefaultPdolBuilder @Inject constructor(
    private val tagValueProducers: @JvmSuppressWildcards Set<TagValueProducer>,
) : PdolBuilder {
    override fun fromTemplate(
        paymentMethodMetadata: PaymentMethodMetadata,
        template: ByteArray,
    ): ByteArray {
        if (template.isEmpty()) {
            return byteArrayOf()
        }

        val terminalValues = buildTerminalValues(paymentMethodMetadata)
        val pdolResponse = buildPdolResponse(template, terminalValues)
        val wrappedData = withCommandDataTag(pdolResponse)

        return wrappedData
    }

    /*
     * Builds tag-value pairs from the configured [TagValueProducer]s using [PaymentMethodMetadata].
     */
    private fun buildTerminalValues(
        paymentMethodMetadata: PaymentMethodMetadata,
    ): Map<String, ByteArray> {
        return tagValueProducers.associate { producer ->
            val result = producer.produce(paymentMethodMetadata)

            result.tag to result.value
        }
    }

    /*
     * Concatenates producer values in the order requested by the PDOL template.
     * Unknown tags are zero-padded to the requested length.
     */
    private fun buildPdolResponse(
        pdolTemplate: ByteArray,
        terminalValues: Map<String, ByteArray>,
    ): ByteArray {
        val response = mutableListOf<Byte>()

        for ((tag, length) in parseDolEntries(pdolTemplate)) {
            val value = terminalValues[tag]

            if (value != null) {
                response.addAll(value.copyOf(length.coerceAtMost(value.size)).toList())

                if (value.size < length) {
                    repeat(length - value.size) {
                        response.add(0x00)
                    }
                }
            } else {
                repeat(length) {
                    response.add(0x00)
                }
            }
        }

        return response.toByteArray()
    }

    /*
     * Parses a Data-Object-Length entries into `(tag, valueLength)` pairs.
     */
    private fun parseDolEntries(template: ByteArray): List<Pair<String, Int>> {
        val entries = mutableListOf<Pair<String, Int>>()
        var offset = 0

        while (offset < template.size) {
            val (tag, tagLength) = readTag(template, offset)
            offset += tagLength
            val length = template[offset++].toInt() and BYTE_MASK
            entries.add(tag to length)
        }

        return entries
    }

    private fun readTag(data: ByteArray, offset: Int): Pair<String, Int> {
        val firstByte = data[offset].toInt() and BYTE_MASK
        var tag = formatAsMinimumTwoCharacters(firstByte)
        var tagLength = 1

        if ((firstByte and TAG_CONTINUATION_MASK) == TAG_CONTINUATION_MASK) {
            val secondByte = data[offset + 1].toInt() and BYTE_MASK
            tag += formatAsMinimumTwoCharacters(secondByte)
            tagLength = 2
        }

        return tag to tagLength
    }

    /*
     * Wraps PDOL data in command template tag `83`.
     */
    private fun withCommandDataTag(pdolResponse: ByteArray): ByteArray {
        return byteArrayOf(COMMAND_TEMPLATE_TAG) +
            encodeLength(pdolResponse.size) +
            pdolResponse
    }

    private fun encodeLength(length: Int): ByteArray {
        return when {
            length < MULTI_BYTE_LENGTH_THRESHOLD -> byteArrayOf(length.toByte())
            length <= SINGLE_BYTE_MAX_LENGTH -> byteArrayOf(MULTI_BYTE_LENGTH_INDICATOR_ONE, length.toByte())
            else -> {
                byteArrayOf(
                    MULTI_BYTE_LENGTH_INDICATOR_TWO,
                    (length shr BYTE_SHIFT).toByte(),
                    (length and BYTE_MASK).toByte(),
                )
            }
        }
    }

    private fun formatAsMinimumTwoCharacters(value: Int): String {
        return String.format(TWO_CHARACTER_FORMAT, value)
    }

    private companion object {
        const val COMMAND_TEMPLATE_TAG = 0x83.toByte()

        const val BYTE_MASK = 0xFF
        const val TAG_CONTINUATION_MASK = 0x1F
        const val BYTE_SHIFT = 8

        const val MULTI_BYTE_LENGTH_THRESHOLD = 0x80
        const val MULTI_BYTE_LENGTH_INDICATOR_ONE = 0x81.toByte()
        const val MULTI_BYTE_LENGTH_INDICATOR_TWO = 0x82.toByte()

        const val SINGLE_BYTE_MAX_LENGTH = 0xFF

        const val TWO_CHARACTER_FORMAT = "%02X"
    }
}
