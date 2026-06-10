package com.stripe.android.common.nfcscan.apdu.commands

internal data class ReadRecordCommand(
    val recordNumber: Int,
    val shortFileIdentifier: Int,
) : ApduCommand<Map<String, ByteArray>>() {
    override val classByte = 0x00.toByte()
    override val instructionByte = 0xB2.toByte()
    override val firstParameterByte = recordNumber.toByte()
    override val secondParameterByte = ((shortFileIdentifier shl 3) or 4).toByte()
    override val dataArray = null

    override fun responseData(tlv: Map<String, ByteArray>): Map<String, ByteArray> = tlv
}
