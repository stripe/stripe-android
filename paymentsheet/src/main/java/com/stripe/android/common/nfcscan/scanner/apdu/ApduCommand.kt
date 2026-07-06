package com.stripe.android.common.nfcscan.scanner.apdu

import com.stripe.android.common.nfcscan.scanner.NfcTagTransceiver

/**
 * An abstraction for defining an [APDU](https://en.wikipedia.org/wiki/Smart_card_application_protocol_data_unit)
 * command that is for communication with an [ISO 7816-4 NFC card](https://en.wikipedia.org/wiki/ISO/IEC_7816).
 */
internal abstract class ApduCommand<TResponseData> {
    /**
     * Instruction class - indicates the type of command
     */
    protected abstract val classByte: Byte

    /**
     * Instruction code - indicates the specific command, e.g., "select", "write data"
     */
    protected abstract val instructionByte: Byte

    /**
     * First instruction parameter for the command, e.g., offset into file at which to write the data
     */
    protected abstract val firstParameterByte: Byte

    /**
     * Second instruction parameter for the command
     */
    protected abstract val secondParameterByte: Byte

    /**
     * Data to send alongside the command for the NFC tag to process when returning a response. Commands can also have
     * no data.
     */
    protected abstract val dataArray: ByteArray?

    /**
     * Defined length fo the data being sent alongside the command
     */
    protected val dataLengthByte: Byte?
        get() = dataArray?.run { size.toByte() }

    /**
     * Converts TLV data into readable response for another command to process or to use when building the final
     * response.
     *
     * @param tlv Tag-Length-Value encoded data separated by string keys which return parsable byte array data.
     */
    protected abstract fun responseData(tlv: Map<String, ByteArray>): TResponseData?

    /**
     * Sends a command to the NFC tag then parses out the response returned from the NFC tag.
     */
    fun transceiveWith(transceiver: NfcTagTransceiver): Result<TResponseData> {
        val rawData = transceiver.transceive(request())
        return response(rawData)
    }

    private fun request(): ByteArray {
        // Builds the byte array request in the proper format
        return byteArrayOf(
            classByte,
            instructionByte,
            firstParameterByte,
            secondParameterByte,
        ) + buildData(dataLengthByte, dataArray) + byteArrayOf(0x00)
    }

    private fun response(rawResponse: ByteArray): Result<TResponseData> {
        // Ensure there is enough response data to parse
        if (rawResponse.size < 2) {
            return Result.failure(ApduResponseError.TooShort())
        }

        // Check if there were any errors that occurred during command execution
        val sw1 = rawResponse[rawResponse.size - 2]
        val sw2 = rawResponse[rawResponse.size - 1]

        if (sw1 != 0x90.toByte() || sw2 != 0x00.toByte()) {
            return Result.failure(ApduResponseError.Command(sw1, sw2))
        }

        val rawData = rawResponse.copyOfRange(0, rawResponse.size - 2)

        return TlvParser.parse(rawData).fold(
            onSuccess = { tlv ->
                responseData(tlv)?.let { responseData ->
                    Result.success(responseData)
                } ?: Result.failure(ApduResponseError.Invalid(rawData))
            },
            onFailure = { cause ->
                Result.failure(ApduResponseError.Parsing(rawData, cause))
            }
        )
    }

    private fun buildData(dataLengthByte: Byte?, dataArray: ByteArray?): ByteArray {
        return if (dataLengthByte != null && dataArray != null) {
            byteArrayOf(dataLengthByte) + dataArray
        } else {
            byteArrayOf()
        }
    }
}
