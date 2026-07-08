package com.stripe.android.common.nfcscan.scanner.apdu

/**
 * An APDU command for reading records from an ISO 7816-4 card application.
 */
internal data class ReadRecordCommand(
    val recordNumber: Int,
    val shortFileIdentifier: Int,
) : ApduCommand<Map<String, ByteArray>>() {
    /*
     * Interindustry standardized command for ISO 7816-4
     */
    override val classByte = 0x00.toByte()

    /*
     * READ RECORD command instruction
     */
    override val instructionByte = 0xB2.toByte()

    /*
     * Record number to retrieve
     */
    override val firstParameterByte = recordNumber.toByte()

    /*
     * File to retrieve using short file identifier
     */
    override val secondParameterByte = ((shortFileIdentifier shl 3) or 4).toByte()
    override val dataArray = null

    override fun responseData(tlv: Map<String, ByteArray>): Map<String, ByteArray> = tlv
}
