package com.stripe.android.common.nfcscan.scanner

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowIsoDep
import java.io.IOException
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
internal class IsoNfcTagTransceiverTest {

    @Test
    fun `open sets timeout and connects`() {
        val isoDep = ShadowIsoDep.newInstance()
        val transceiver = IsoNfcTagTransceiver(isoDep)

        transceiver.open()

        assertThat(isoDep.timeout).isEqualTo(TIMEOUT_MS)
        assertThat(isoDep.isConnected).isTrue()
    }

    @Test
    fun `transceive delegates to IsoDep`() {
        val isoDep = ShadowIsoDep.newInstance()
        val transceiver = IsoNfcTagTransceiver(isoDep)

        shadowOf(isoDep).setTransceiveResponse(byteArrayOf(0x90.toByte(), 0x00))

        val response = transceiver.transceive(byteArrayOf(0x00, 0xA4.toByte()))

        assertThat(response).isEqualTo(byteArrayOf(0x90.toByte(), 0x00))
    }

    @Test
    fun `close disconnects from IsoDep`() {
        val isoDep = ShadowIsoDep.newInstance()
        val transceiver = IsoNfcTagTransceiver(isoDep)

        transceiver.open()
        transceiver.close()

        assertThat(isoDep.isConnected).isFalse()
    }

    @Test
    fun `transceive propagates IOException from IsoDep`() {
        val isoDep = ShadowIsoDep.newInstance()
        val transceiver = IsoNfcTagTransceiver(isoDep)

        shadowOf(isoDep).setNextTransceiveResponse(byteArrayOf(0x90.toByte(), 0x00))

        assertThat(transceiver.transceive(byteArrayOf(0x00))).isNotNull()

        assertFailsWith<IOException> {
            transceiver.transceive(byteArrayOf(0x00))
        }
    }

    private companion object {
        const val TIMEOUT_MS = 5000
    }
}
