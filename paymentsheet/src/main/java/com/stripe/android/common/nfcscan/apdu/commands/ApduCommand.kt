package com.stripe.android.common.nfcscan.apdu.commands

import com.stripe.android.common.nfcscan.apdu.IsoNfcTagTransceiver

internal abstract class ApduCommand<TResponseData> {
    protected abstract val classByte: Byte
    protected abstract val instructionByte: Byte
    protected abstract val firstParameterByte: Byte
    protected abstract val secondParameterByte: Byte
    protected abstract val dataArray: ByteArray?

    protected val dataLengthByte: Byte?
        get() = dataArray?.run { size.toByte() }

    protected abstract fun responseData(tlv: Map<String, ByteArray>): TResponseData?

    fun transceiveWith(transceiver: IsoNfcTagTransceiver): Result<TResponseData> {
        val rawData = transceiver.transceive(request())
        return response(rawData)
    }

    private fun request(): ByteArray {
        return byteArrayOf(
            classByte,
            instructionByte,
            firstParameterByte,
            secondParameterByte,
        ) + buildData(dataLengthByte, dataArray) + byteArrayOf(0x00)
    }

    private fun response(rawResponse: ByteArray): Result<TResponseData> {
        if (rawResponse.size < 2) {
            return Result.failure(AdpuResponseError.TooShort())
        }

        val sw1 = rawResponse[rawResponse.size - 2]
        val sw2 = rawResponse[rawResponse.size - 1]

        if (sw1 != 0x90.toByte() || sw2 != 0x00.toByte()) {
            return Result.failure(AdpuResponseError.Command(sw1, sw2))
        }

        val rawData = rawResponse.copyOfRange(0, rawResponse.size - 2)

        val responseData = responseData(TlvParser.parse(rawData))
            ?: return Result.failure(AdpuResponseError.Parsing(rawData))

        return Result.success(responseData)
    }

    private fun buildData(dataLengthByte: Byte?, dataArray: ByteArray?): ByteArray {
        val dataLengthByte = dataLengthByte
        val dataArray = dataArray

        return if (dataLengthByte != null && dataArray != null) {
            byteArrayOf(dataLengthByte) + dataArray
        } else {
            byteArrayOf()
        }
    }
}
