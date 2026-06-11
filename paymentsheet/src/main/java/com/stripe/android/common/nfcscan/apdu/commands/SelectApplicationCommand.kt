package com.stripe.android.common.nfcscan.apdu.commands

internal data class SelectApplicationCommand(
    val aid: ApplicationIdentifier,
) : ApduCommand<Unit>() {
    override val classByte = 0x00.toByte()
    override val instructionByte = 0xA4.toByte()
    override val firstParameterByte = 0x04.toByte()
    override val secondParameterByte = 0x00.toByte()
    override val dataArray = aid.value

    override fun responseData(tlv: Map<String, ByteArray>) {
        // No-op
    }
}