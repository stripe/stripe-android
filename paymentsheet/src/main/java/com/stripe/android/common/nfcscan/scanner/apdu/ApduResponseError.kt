package com.stripe.android.common.nfcscan.scanner.apdu

internal sealed class ApduResponseError(
    override val message: String?,
    override val cause: Throwable? = null,
) : Throwable() {
    class TooShort : ApduResponseError("APDU response is too short! Needs at least two bytes!")

    data class Command(
        val apduCommand: ApduCommand<*>,
        val sw1: Byte,
        val sw2: Byte,
    ) : ApduResponseError("APDU error: SW1=$sw1, SW2=$sw2")

    sealed class Data(
        message: String?,
        cause: Throwable? = null,
    ) : ApduResponseError(message, cause) {
        abstract val data: ByteArray

        class Parsing(
            override val data: ByteArray,
            cause: Throwable?,
        ) : Data("Failed to parse response data: ${data.toHexString()}", cause)

        class Invalid(
            override val data: ByteArray,
        ) : Data("Invalid data in response: ${data.toHexString()}")
    }
}
