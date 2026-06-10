package com.stripe.android.common.nfcscan.apdu.commands

internal class PdolData(private val data: ByteArray) {
    fun toCommandTemplate(): ByteArray {
        return byteArrayOf(0x83.toByte(), data.size.toByte()) + data
    }
}
