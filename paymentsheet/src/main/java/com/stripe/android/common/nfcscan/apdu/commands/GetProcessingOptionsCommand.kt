package com.stripe.android.common.nfcscan.apdu.commands

internal class GetProcessingOptionsCommand(pdolData: PdolData) : ApduCommand<GpoResult>() {
    override val classByte = 0x80.toByte()
    override val instructionByte = 0xA8.toByte()
    override val firstParameterByte = 0x00.toByte()
    override val secondParameterByte = 0x00.toByte()
    override val dataArray = pdolData.toCommandTemplate()

    override fun responseData(tlv: Map<String, ByteArray>): GpoResult {
        val aflBytes = tlv[TAG_AFL] ?: run {
            val fmt1 = tlv[TAG_RESPONSE_TEMPLATE_1] ?: return GpoResult(emptyList(), tlv)
            if (fmt1.size <= 2) return GpoResult(emptyList(), tlv)
            fmt1.drop(2).toByteArray()
        }
        return GpoResult(parseAflEntries(aflBytes), tlv)
    }

    private fun parseAflEntries(afl: ByteArray): List<ApplicationFileLocator> {
        val entries = mutableListOf<ApplicationFileLocator>()
        var i = 0
        while (i + 3 < afl.size) {
            val sfi = (afl[i].toInt() and 0xFF) shr 3
            val first = afl[i + 1].toInt() and 0xFF
            val last = afl[i + 2].toInt() and 0xFF
            for (rec in first..last) {
                entries.add(ApplicationFileLocator(recordNumber = rec, shortFileIdentifier = sfi))
            }
            i += 4
        }
        return entries
    }

    private companion object {
        const val TAG_AFL = "94"
        const val TAG_RESPONSE_TEMPLATE_1 = "80"
    }
}
