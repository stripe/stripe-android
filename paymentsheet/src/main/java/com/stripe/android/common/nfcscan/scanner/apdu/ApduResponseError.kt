package com.stripe.android.common.nfcscan.scanner.apdu

internal sealed class ApduResponseError(
    override val message: String?,
    override val cause: Throwable? = null,
) : Throwable() {
    class TooShort : ApduResponseError("APDU response is too short! Needs at least two bytes!")

    data class Command(
        val sw1: Byte,
        val sw2: Byte,
    ) : ApduResponseError("APDU error: SW1=$sw1, SW2=$sw2")

    class Parsing(
        val data: ByteArray,
        override val cause: Throwable?,
    ) : ApduResponseError("Failed to parse response data: ${data.toHexString()}")

    class Invalid(
        val data: ByteArray
    ) : ApduResponseError("Invalid data in response: ${data.toHexString()}")
}
