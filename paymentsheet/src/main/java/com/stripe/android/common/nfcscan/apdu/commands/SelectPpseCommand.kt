package com.stripe.android.common.nfcscan.apdu.commands

import kotlin.collections.iterator

internal data object SelectPpseCommand : ApduCommand<ApplicationIdentifier>() {
    override val classByte = 0x00.toByte()
    override val instructionByte = 0xA4.toByte()
    override val firstParameterByte = 0x04.toByte()
    override val secondParameterByte = 0x00.toByte()
    override val dataArray = "2PAY.SYS.DDF01".toByteArray(Charsets.US_ASCII)

    override fun responseData(tlv: Map<String, ByteArray>): ApplicationIdentifier? {
        val rawAid = tlv[TAG_AID]
            ?: tlv[TAG_DF_NAME]
            ?: findRawAid(tlv)
            ?: return null

        return ApplicationIdentifier(rawAid)
    }

    private fun findRawAid(tlv: Map<String, ByteArray>): ByteArray? {
        for ((_, value) in tlv) {
            val hex = value.toHexString().uppercase()

            when {
                hex.startsWith("A000000003") -> return value // Visa
                hex.startsWith("A000000004") -> return value // Mastercard
                hex.startsWith("A000000025") -> return value // Amex
                hex.startsWith("A000000152") -> return value // Discover
                hex.startsWith("A000000065") -> return value // JCB
                hex.startsWith("A000000333") -> return value // Discover
            }
        }

        return null
    }

    private const val TAG_AID = "4F"
    private const val TAG_DF_NAME = "84"
}
