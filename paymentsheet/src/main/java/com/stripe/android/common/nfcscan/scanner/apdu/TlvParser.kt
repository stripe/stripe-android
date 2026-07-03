package com.stripe.android.common.nfcscan.scanner.apdu

/**
 * Converts unparsed Tag-Length-Value encoded data into a readable map keyed by the key.
 */
internal object TlvParser {
    fun parse(data: ByteArray): Result<Map<String, ByteArray>> {
        val nodes = mutableMapOf<String, ByteArray>()

        return runCatching {
            parseInto(data, nodes)
        }.map {
            nodes.toMap()
        }
    }

    /*
     * Recursively parses TLV-encoded data and populates nodes with tag-value pairs. Constructed tags
     * are parsed recursively to extract nested TLV data.
     */
    private fun parseInto(data: ByteArray, nodes: MutableMap<String, ByteArray>) {
        var offset = 0

        while (offset < data.size) {
            /*
             * Get the tag for the data
             */
            val tagByte = data[offset]
            val tagInt = tagByte.asInt()
            var tag = formatAsMinimumTwoCharacters(tagInt)
            val isConstructed = (tagInt and SIXTH_BIT_MASK) != 0

            if ((tagInt and TAG_NUMBER_MASK) == 0x1F) {
                val nextTagByte = data[++offset]
                tag += formatAsMinimumTwoCharacters(nextTagByte.asInt())
            }
            offset++

            /*
             * Get the length of the data
             */
            val lengthByte = data[offset++].asInt()
            var valueLength = lengthByte

            /*
             * Some data might be long enough that its length cannot be represented in one byte. In that case, parse
             * out the multibyte length.
             */
            if (lengthByte > MULTI_BYTE_LENGTH_INDICATOR) {
                val numberOfLengthBytes = lengthByte and NUMBER_OF_LENGTH_BYTES_MASK
                valueLength = 0
                for (i in 0 until numberOfLengthBytes) {
                    valueLength = (valueLength shl VALUE_LENGTH_SHIFT) or (data[offset++].asInt())
                }
            }

            /*
             * Get the value data for this tag using the length retrieved above
             */
            val value = data.copyOfRange(offset, offset + valueLength)
            nodes[tag] = value

            if (isConstructed) {
                parseInto(value, nodes)
            }

            offset += valueLength
        }
    }

    private fun formatAsMinimumTwoCharacters(value: Int): String {
        return String.format(TWO_CHARACTER_FORMAT, value)
    }

    private fun Byte.asInt(): Int {
        return toInt() and ISOLATE_BYTE_MASK
    }

    private const val VALUE_LENGTH_SHIFT = 8
    private const val NUMBER_OF_LENGTH_BYTES_MASK = 0x7F
    private const val MULTI_BYTE_LENGTH_INDICATOR = 0x80
    private const val TAG_NUMBER_MASK = 0x1F
    private const val SIXTH_BIT_MASK = 0x20
    private const val ISOLATE_BYTE_MASK = 0xFF
    private const val TWO_CHARACTER_FORMAT = "%02X"
}
