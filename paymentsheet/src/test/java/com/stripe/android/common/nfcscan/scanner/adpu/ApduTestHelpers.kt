package com.stripe.android.common.nfcscan.scanner.adpu

internal fun tlv(tag: Byte, value: ByteArray): ByteArray {
    return byteArrayOf(tag) + encodeLength(value.size) + value
}

internal fun tlv(tag: Byte, tagContinuation: Byte, value: ByteArray): ByteArray {
    return byteArrayOf(tag, tagContinuation) + encodeLength(value.size) + value
}

internal fun apduSuccessResponse(data: ByteArray): ByteArray {
    return data + byteArrayOf(0x90.toByte(), 0x00)
}

private fun encodeLength(length: Int): ByteArray {
    return when {
        length < 0x80 -> byteArrayOf(length.toByte())
        length <= 0xFF -> byteArrayOf(0x81.toByte(), length.toByte())
        else -> byteArrayOf(
            0x82.toByte(),
            (length shr 8).toByte(),
            (length and 0xFF).toByte(),
        )
    }
}
