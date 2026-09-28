package com.stripe.android.common.nfcscan.scanner

import android.nfc.Tag
import android.nfc.tech.IsoDep
import java.io.IOException
import javax.inject.Inject
import kotlin.jvm.Throws

internal interface NfcTagTransceiver {
    @Throws(IOException::class, SecurityException::class)
    fun open()

    @Throws(IOException::class, SecurityException::class)
    fun transceive(data: ByteArray): ByteArray

    @Throws(IOException::class, SecurityException::class)
    fun close()

    interface Factory {
        fun create(tag: Tag): NfcTagTransceiver?
    }
}

internal class IsoNfcTagTransceiver(
    private val isoDep: IsoDep,
) : NfcTagTransceiver {
    override fun open() {
        isoDep.timeout = TIMEOUT_MS
        isoDep.connect()
    }

    override fun transceive(data: ByteArray): ByteArray {
        return isoDep.transceive(data)
    }

    override fun close() {
        isoDep.close()
    }

    class Factory @Inject constructor() : NfcTagTransceiver.Factory {
        override fun create(tag: Tag): IsoNfcTagTransceiver? {
            return IsoDep.get(tag)?.let {
                IsoNfcTagTransceiver(it)
            }
        }
    }

    private companion object {
        const val TIMEOUT_MS = 5000
    }
}
