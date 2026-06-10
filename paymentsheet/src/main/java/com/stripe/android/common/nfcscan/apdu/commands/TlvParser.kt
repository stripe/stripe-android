package com.stripe.android.common.nfcscan.apdu.commands

internal object TlvParser {
    fun parse(data: ByteArray): Map<String, ByteArray> {
        val nodes = mutableMapOf<String, ByteArray>()
        parseInto(data, nodes)
        return nodes
    }

    private fun parseInto(data: ByteArray, nodes: MutableMap<String, ByteArray>) {
        var offset = 0

        while (offset < data.size) {
            val tagByte = data[offset]
            val tagInt = tagByte.toInt() and 0xFF
            var tag = String.format("%02X", tagInt)
            val isConstructed = (tagInt and 0x20) != 0

            if ((tagInt and 0x1F) == 0x1F) {
                val nextTagByte = data[++offset]
                tag += String.format("%02X", nextTagByte.toInt() and 0xFF)
            }
            offset++

            val lengthByte = data[offset++].toInt() and 0xFF
            var valueLength = lengthByte

            if (lengthByte > 0x80) {
                val numberOfLengthBytes = lengthByte and 0x7F
                valueLength = 0
                for (i in 0 until numberOfLengthBytes) {
                    valueLength = (valueLength shl 8) or (data[offset++].toInt() and 0xFF)
                }
            }

            val value = data.copyOfRange(offset, offset + valueLength)
            nodes[tag] = value

            if (isConstructed) {
                parseInto(value, nodes)
            }

            offset += valueLength
        }
    }
}
