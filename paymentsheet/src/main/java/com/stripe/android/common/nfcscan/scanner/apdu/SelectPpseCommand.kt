package com.stripe.android.common.nfcscan.scanner.apdu

import com.stripe.android.model.CardBrand

/**
 * An APDU command for selecting the first payment application on the credit card chip
 */
internal data object SelectPpseCommand : ApduCommand<ApplicationIdentifier>() {
    /*
     * Interindustry standardized command for ISO 7816-4
     */
    override val classByte = 0x00.toByte()

    /*
     * SELECT command instruction
     */
    override val instructionByte = 0xA4.toByte()

    /*
     * Select by name
     */
    override val firstParameterByte = 0x04.toByte()

    /*
     * First or only occurrence
     */
    override val secondParameterByte = 0x00.toByte()

    /*
     * PPSE (Proximity Payment System Environment) directory name
     */
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

    /*
     * If we cannot find the application identifier from known tags, we will then look through all the available tags
     * for supportable application identifiers.
     */
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

    // Application Identifier (AID) tag, should be available in most credit chips by default
    private const val TAG_AID = "4F"

    // Dedicated File (DF) Name tag, also identifies the application. Secondary option for retrieving the application
    // identifier.
    private const val TAG_DF_NAME = "84"
}
