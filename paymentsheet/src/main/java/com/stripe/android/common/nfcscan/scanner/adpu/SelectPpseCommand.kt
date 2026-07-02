package com.stripe.android.common.nfcscan.scanner.adpu

import com.stripe.android.model.CardBrand

internal data object SelectPpseCommand : ApduCommand<ApplicationIdentifier>() {
    override val classByte = 0x00.toByte()
    override val instructionByte = 0xA4.toByte()
    override val firstParameterByte = 0x04.toByte()
    override val secondParameterByte = 0x00.toByte()
    override val dataArray = "2PAY.SYS.DDF01".toByteArray(Charsets.US_ASCII)

    override fun responseData(tlv: Map<String, ByteArray>): ApplicationIdentifier? {
        val rawAid = getKnownAid(tlv)
            ?: findRawAid(tlv)
            ?: return null

        return ApplicationIdentifier(rawAid)
    }

    private fun getKnownAid(tlv: Map<String, ByteArray>): String? {
        val aid = tlv[TAG_AID] ?: tlv[TAG_DF_NAME]

        return aid?.toHexString()
    }

    private fun findRawAid(tlv: Map<String, ByteArray>): String? {
        for ((_, value) in tlv) {
            val hexValue = value.toHexString()
            val valueUppercased = hexValue.uppercase()

            if (CardBrand.isSupportedCardPresentApplication(valueUppercased)) {
                return valueUppercased
            }
        }

        return null
    }

    private const val TAG_AID = "4F"
    private const val TAG_DF_NAME = "84"
}
