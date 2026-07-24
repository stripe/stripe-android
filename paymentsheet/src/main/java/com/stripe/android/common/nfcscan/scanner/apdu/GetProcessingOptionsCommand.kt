package com.stripe.android.common.nfcscan.scanner.apdu

/**
 * An APDU command for initiating EMV application processing and retrieving the card's AFL.
 */
internal class GetProcessingOptionsCommand(
    processingOptionsDataObjectList: ByteArray?,
) : ApduCommand<GetProcessingOptionsCommand.Response>() {
    override val commandName: String = "getProcessingOptions"

    override val classByte = 0x80.toByte()
    override val instructionByte = 0xA8.toByte()
    override val firstParameterByte = 0x00.toByte()
    override val secondParameterByte = 0x00.toByte()
    override val dataArray = buildDataArray(processingOptionsDataObjectList)

    override fun responseData(tlv: Map<String, ByteArray>): Response {
        return Response(
            records = tlv,
            recordLocators = tlv[TAG_AFL]?.toRecordLocators()
                ?: tlv[TAG_RESPONSE_MESSAGE_TEMPLATE_FORMAT_1]?.toRecordLocatorsFromFormat1()
                ?: emptyList()
        )
    }

    data class Response(
        val records: Map<String, ByteArray>,
        val recordLocators: List<RecordLocator>,
    )

    data class RecordLocator(
        val shortFileIdentifier: Int,
        val firstRecord: Int,
        val lastRecord: Int,
    )

    private fun buildDataArray(processingOptionsDataObjectList: ByteArray?): ByteArray {
        val processingOptionsData = processingOptionsDataObjectList?.toProcessingOptionsData()
            ?: byteArrayOf()

        return byteArrayOf(
            TAG_COMMAND_TEMPLATE.toByte(),
            processingOptionsData.size.toByte(),
        ) + processingOptionsData
    }

    private fun ByteArray.toProcessingOptionsData(): ByteArray {
        val result = mutableListOf<Byte>()
        var offset = 0

        while (offset < size) {
            val tagStart = offset
            var tagByte = this[offset++]

            if ((tagByte.toInt() and TAG_NUMBER_MASK) == TAG_NUMBER_MASK) {
                do {
                    tagByte = this[offset++]
                } while ((tagByte.toInt() and TAG_CONTINUATION_MASK) == TAG_CONTINUATION_MASK)
            }

            val tag = copyOfRange(tagStart, offset).toHexString().uppercase()
            val length = this[offset++].toInt() and BYTE_MASK
            val defaultValue = DEFAULT_DOL_VALUES[tag] ?: byteArrayOf()
            val value = defaultValue.copyOf(length)

            result.addAll(value.toList())
        }

        return result.toByteArray()
    }

    private fun ByteArray.toRecordLocatorsFromFormat1(): List<RecordLocator> {
        if (size <= APPLICATION_INTERCHANGE_PROFILE_LENGTH) {
            return emptyList()
        }

        return copyOfRange(APPLICATION_INTERCHANGE_PROFILE_LENGTH, size).toRecordLocators()
    }

    private fun ByteArray.toRecordLocators(): List<RecordLocator> {
        return asList()
            .chunked(AFL_ENTRY_LENGTH)
            .mapNotNull { entry ->
                if (entry.size != AFL_ENTRY_LENGTH) {
                    return@mapNotNull null
                }

                val shortFileIdentifier = (entry[0].toInt() and BYTE_MASK) shr SFI_SHIFT
                val firstRecord = entry[1].toInt() and BYTE_MASK
                val lastRecord = entry[2].toInt() and BYTE_MASK

                if (shortFileIdentifier == 0 || firstRecord == 0 || lastRecord < firstRecord) {
                    null
                } else {
                    RecordLocator(
                        shortFileIdentifier = shortFileIdentifier,
                        firstRecord = firstRecord,
                        lastRecord = lastRecord,
                    )
                }
            }
    }

    private companion object {
        const val TAG_AFL = "94"
        const val TAG_RESPONSE_MESSAGE_TEMPLATE_FORMAT_1 = "80"
        const val TAG_COMMAND_TEMPLATE = 0x83

        const val APPLICATION_INTERCHANGE_PROFILE_LENGTH = 2
        const val AFL_ENTRY_LENGTH = 4
        const val SFI_SHIFT = 3
        const val BYTE_MASK = 0xFF
        const val TAG_NUMBER_MASK = 0x1F
        const val TAG_CONTINUATION_MASK = 0x80

        val DEFAULT_DOL_VALUES = mapOf(
            // Terminal Transaction Qualifiers: contactless MSD/qVSDC supported, online-capable reader.
            "9F66" to byteArrayOf(0x36, 0x00, 0x00, 0x00),
        )
    }
}
