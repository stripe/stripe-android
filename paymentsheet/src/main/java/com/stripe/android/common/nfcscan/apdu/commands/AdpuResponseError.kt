package com.stripe.android.common.nfcscan.apdu.commands

internal sealed class AdpuResponseError(override val message: String?) : Throwable() {
    class TooShort : AdpuResponseError("ADPU response is too short! Needs at least two bytes!")

    data class Command(
        val sw1: Byte,
        val sw2: Byte,
    ) : AdpuResponseError("APDU error: SW1=$sw1, SW2=$sw2")

    class Parsing(
        val data: ByteArray
    ) : AdpuResponseError("Failed to parse response data: ${data.toHexString()}")
}
