package com.stripe.android.common.nfcscan.scanner

import android.nfc.Tag
import java.io.IOException
import javax.inject.Inject
import kotlin.jvm.Throws

internal interface NfcTagTransceiver {
    @Throws(IOException::class)
    fun open()

    @Throws(IOException::class)
    fun transceive(data: ByteArray): ByteArray

    @Throws(IOException::class)
    fun close()

    interface Factory {
        fun create(tag: Tag): NfcTagTransceiver?
    }
}

internal class IsoNfcTagTransceiver : NfcTagTransceiver {
    override fun open() {
        // Do nothing
    }

    override fun transceive(data: ByteArray): ByteArray {
        return ByteArray(0x00)
    }

    override fun close() {
        // Do nothing
    }

    class Factory @Inject constructor() : NfcTagTransceiver.Factory {
        override fun create(tag: Tag): NfcTagTransceiver? {
            return null
        }
    }
}
