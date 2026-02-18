package com.stripe.android.common.taptoadd.nfcdirect

/**
 * BER-TLV parser for EMV contactless card data.
 * Parses Tag-Length-Value structures as defined in EMV specifications.
 */
internal object TlvParser {

    /**
     * Parsed TLV data element.
     */
    data class TlvElement(
        val tag: String,
        val value: ByteArray,
        val isConstructed: Boolean = false,
        val children: List<TlvElement> = emptyList()
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is TlvElement) return false
            return tag == other.tag && value.contentEquals(other.value)
        }

        override fun hashCode(): Int {
            return 31 * tag.hashCode() + value.contentHashCode()
        }
    }

    /**
     * Parse BER-TLV encoded data into a flat map of tag -> value.
     * Recursively parses constructed tags.
     */
    fun parse(data: ByteArray): Map<String, ByteArray> {
        val result = mutableMapOf<String, ByteArray>()
        parseRecursive(data, 0, data.size, result)
        return result
    }

    /**
     * Parse BER-TLV data and return structured elements.
     */
    fun parseToElements(data: ByteArray): List<TlvElement> {
        return parseElements(data, 0, data.size)
    }

    private fun parseRecursive(
        data: ByteArray,
        startOffset: Int,
        endOffset: Int,
        result: MutableMap<String, ByteArray>
    ) {
        var offset = startOffset

        while (offset < endOffset) {
            // Parse tag
            val tagResult = parseTag(data, offset)
            if (tagResult == null) break
            val (tag, tagLength) = tagResult
            offset += tagLength

            if (offset >= endOffset) break

            // Parse length
            val lengthResult = parseLength(data, offset)
            if (lengthResult == null) break
            val (length, lengthBytes) = lengthResult
            offset += lengthBytes

            if (offset + length > endOffset) break

            // Extract value
            val value = data.copyOfRange(offset, offset + length)
            result[tag] = value

            // If constructed tag, parse children
            if (isConstructedTag(data[offset - tagLength - lengthBytes].toInt() and 0xFF)) {
                parseRecursive(value, 0, value.size, result)
            }

            offset += length
        }
    }

    private fun parseElements(
        data: ByteArray,
        startOffset: Int,
        endOffset: Int
    ): List<TlvElement> {
        val elements = mutableListOf<TlvElement>()
        var offset = startOffset

        while (offset < endOffset) {
            val tagResult = parseTag(data, offset) ?: break
            val (tag, tagLength) = tagResult
            val tagFirstByte = data[offset].toInt() and 0xFF
            offset += tagLength

            if (offset >= endOffset) break

            val lengthResult = parseLength(data, offset) ?: break
            val (length, lengthBytes) = lengthResult
            offset += lengthBytes

            if (offset + length > endOffset) break

            val value = data.copyOfRange(offset, offset + length)
            val isConstructed = isConstructedTag(tagFirstByte)

            val element = if (isConstructed) {
                TlvElement(
                    tag = tag,
                    value = value,
                    isConstructed = true,
                    children = parseElements(value, 0, value.size)
                )
            } else {
                TlvElement(tag = tag, value = value)
            }

            elements.add(element)
            offset += length
        }

        return elements
    }

    private fun parseTag(data: ByteArray, offset: Int): Pair<String, Int>? {
        if (offset >= data.size) return null

        val firstByte = data[offset].toInt() and 0xFF

        // Skip padding bytes (00 or FF)
        if (firstByte == 0x00 || firstByte == 0xFF) {
            return null
        }

        // Check if multi-byte tag (bits 1-5 all set)
        return if ((firstByte and 0x1F) == 0x1F) {
            // Multi-byte tag
            var tagLength = 1
            while (offset + tagLength < data.size) {
                val nextByte = data[offset + tagLength].toInt() and 0xFF
                tagLength++
                // If bit 8 is not set, this is the last byte
                if ((nextByte and 0x80) == 0) break
            }
            val tagBytes = data.copyOfRange(offset, offset + tagLength)
            tagBytes.toHexString() to tagLength
        } else {
            // Single byte tag
            String.format("%02X", firstByte) to 1
        }
    }

    private fun parseLength(data: ByteArray, offset: Int): Pair<Int, Int>? {
        if (offset >= data.size) return null

        val firstByte = data[offset].toInt() and 0xFF

        return if ((firstByte and 0x80) == 0) {
            // Short form: length is in bits 1-7
            firstByte to 1
        } else {
            // Long form: bits 1-7 indicate number of subsequent length bytes
            val numBytes = firstByte and 0x7F
            if (numBytes == 0 || offset + 1 + numBytes > data.size) {
                return null
            }
            var length = 0
            for (i in 1..numBytes) {
                length = (length shl 8) or (data[offset + i].toInt() and 0xFF)
            }
            length to (1 + numBytes)
        }
    }

    private fun isConstructedTag(firstByte: Int): Boolean {
        // Bit 6 indicates constructed (1) vs primitive (0)
        return (firstByte and 0x20) != 0
    }

    /**
     * Find a specific tag in parsed TLV data.
     */
    fun findTag(data: ByteArray, targetTag: String): ByteArray? {
        return parse(data)[targetTag.uppercase()]
    }

    /**
     * Convert ByteArray to hex string.
     */
    fun ByteArray.toHexString(): String {
        return joinToString("") { String.format("%02X", it) }
    }

    /**
     * Convert hex string to ByteArray.
     */
    fun String.hexToByteArray(): ByteArray {
        val cleanHex = this.replace(" ", "").uppercase()
        return ByteArray(cleanHex.length / 2) { i ->
            cleanHex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}
