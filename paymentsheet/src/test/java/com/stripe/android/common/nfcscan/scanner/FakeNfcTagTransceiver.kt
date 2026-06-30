package com.stripe.android.common.nfcscan.scanner

import android.nfc.Tag
import app.cash.turbine.Turbine
import java.io.IOException

internal class FakeNfcTagTransceiver(
    private val openException: Throwable? = null,
    private val closeException: Throwable? = null,
    private val transceiveResult: ByteArray = byteArrayOf(),
    private val transceiveException: Throwable? = null,
) : NfcTagTransceiver {
    val openCalls = Turbine<Unit>()
    val closeCalls = Turbine<Unit>()
    val transceiveCalls = Turbine<ByteArray>()

    override fun open() {
        openCalls.add(Unit)
        openException?.let { throw it }
    }

    override fun transceive(data: ByteArray): ByteArray {
        transceiveCalls.add(data)
        transceiveException?.let { throw it as IOException }
        return transceiveResult
    }

    override fun close() {
        closeCalls.add(Unit)
        closeException?.let { throw it }
    }

    fun ensureAllEventsConsumed() {
        openCalls.ensureAllEventsConsumed()
        closeCalls.ensureAllEventsConsumed()
        transceiveCalls.ensureAllEventsConsumed()
    }
}

internal class FakeNfcTagTransceiverFactory(
    private val transceiver: NfcTagTransceiver? = FakeNfcTagTransceiver(),
) : NfcTagTransceiver.Factory {
    val createCalls = Turbine<Tag>()

    override fun create(tag: Tag): NfcTagTransceiver? {
        createCalls.add(tag)
        return transceiver
    }

    fun ensureAllEventsConsumed() {
        createCalls.ensureAllEventsConsumed()
    }
}
