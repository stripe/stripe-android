package com.stripe.android.common.nfcscan.scanner.apdu

/**
 * An APDU command for selecting the payment application on the credit card chip
 */
internal data class SelectApplicationCommand(
    val aid: ApplicationIdentifier,
) : ApduCommand<SelectApplicationCommand.Response>() {
    override val commandName: String = "selectApplication(aid=$aid)"

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
     * Application identifier value
     */
    override val dataArray = aid.value.hexToByteArray()

    override fun responseData(tlv: Map<String, ByteArray>): Response {
        return Response(
            processingOptionsDataObjectList = tlv[TAG_PDOL]
        )
    }

    internal data class Response(
        val processingOptionsDataObjectList: ByteArray?,
    )

    private companion object {
        const val TAG_PDOL = "9F38"
    }
}
