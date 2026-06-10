package com.stripe.android.common.nfcscan.apdu

import android.nfc.Tag
import android.nfc.tech.IsoDep
import java.io.IOException
import javax.inject.Inject
import kotlin.jvm.Throws

internal interface IsoNfcTagTransceiver {
    @Throws(IOException::class)
    fun open()

    @Throws(IOException::class)
    fun transceive(data: ByteArray): ByteArray

    @Throws(IOException::class)
    fun close()

    interface Factory {
        fun create(tag: Tag): IsoNfcTagTransceiver?
    }
}

internal class DefaultIsoNfcTagTransceiver(
    private val isoDep: IsoDep,
) : IsoNfcTagTransceiver {
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

    class Factory @Inject constructor() : IsoNfcTagTransceiver.Factory {
        override fun create(tag: Tag): IsoNfcTagTransceiver? {
            return IsoDep.get(tag)?.let {
                DefaultIsoNfcTagTransceiver(it)
            }
        }
    }

    private companion object {
        const val TIMEOUT_MS = 5000
    }
}
