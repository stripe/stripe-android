package com.stripe.android.common.nfcscan.apdu.commands

internal class PdolTemplate(private val entries: List<Entry>) {

    data class Entry(val tag: String, val length: Int)

    fun resolve(values: Map<String, ByteArray>): PdolData {
        val bytes = entries.flatMap { (tag, length) ->
            val value = values[tag] ?: ByteArray(length)
            value.copyOf(length).toList()
        }.toByteArray()
        return PdolData(bytes)
    }

    companion object {
        val empty = PdolTemplate(emptyList())

        fun parse(bytes: ByteArray): PdolTemplate {
            val entries = mutableListOf<Entry>()
            var i = 0
            while (i < bytes.size) {
                val tagByte = bytes[i].toInt() and 0xFF
                var tag = "%02X".format(tagByte)
                if ((tagByte and 0x1F) == 0x1F) {
                    tag += "%02X".format(bytes[++i].toInt() and 0xFF)
                }
                i++
                val length = bytes[i++].toInt() and 0xFF
                entries.add(Entry(tag, length))
            }
            return PdolTemplate(entries)
        }
    }
}
