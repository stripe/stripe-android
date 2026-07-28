package com.stripe.android.common.nfcscan.scanner.apdu

/**
 * An APDU command for retrieving processing options from a payment application.
 */
internal class GetProcessingOptionsCommand(
    val pdolData: ByteArray,
) : ApduCommand<ProcessingOptionsInfo>() {
    override val classByte = 0x80.toByte()
    override val instructionByte = 0xA8.toByte()
    override val firstParameterByte = 0x00.toByte()
    override val secondParameterByte = 0x00.toByte()
    override val dataArray = if (pdolData.isEmpty()) {
        byteArrayOf(0x83.toByte(), 0x00)
    } else {
        pdolData
    }

    override fun responseData(tlv: Map<String, ByteArray>): ProcessingOptionsInfo? {
        if (tlv.isEmpty()) {
            return null
        }

        return ProcessingOptionsInfo(
            aflEntries = extractAfl(tlv)?.let(::parseAflEntries).orEmpty(),
            records = tlv,
        )
    }

    private fun extractAfl(tlv: Map<String, ByteArray>): ByteArray? {
        return tlv[TAG_AFL] ?: tlv[TAG_RESPONSE_TEMPLATE_FORMAT_1]?.let { format1Value ->
            format1Value.takeIf { it.size > AIP_LENGTH }
                ?.copyOfRange(AIP_LENGTH, format1Value.size)
        }
    }

    private fun parseAflEntries(data: ByteArray): List<ProcessingOptionsInfo.AflEntry> {
        return data.toList()
            .chunked(AFL_ENTRY_LENGTH)
            .map { entry ->
                ProcessingOptionsInfo.AflEntry(
                    shortFileIdentifier = (entry[0].toInt() shr SFI_SHIFT) and SFI_MASK,
                    firstRecord = entry[1].toInt() and BYTE_MASK,
                    lastRecord = entry[2].toInt() and BYTE_MASK,
                )
            }
    }

    private companion object {
        const val TAG_RESPONSE_TEMPLATE_FORMAT_1 = "80"
        const val TAG_AFL = "94"
        const val AIP_LENGTH = 2

        const val AFL_ENTRY_LENGTH = 4
        const val SFI_SHIFT = 3
        const val SFI_MASK = 0x1F
        const val BYTE_MASK = 0xFF
    }
}
