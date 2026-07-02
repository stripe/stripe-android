package com.stripe.android.common.nfcscan.scanner

import android.nfc.Tag
import android.nfc.tech.IsoDep
import com.google.common.truth.Truth.assertThat
import java.io.IOException
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class IsoNfcTagTransceiverTest {
    @Test
    fun `open sets timeout and connects to isoDep`() {
        val isoDep = mock<IsoDep>()
        val transceiver = IsoNfcTagTransceiver(isoDep)

        transceiver.open()

        verify(isoDep).timeout = TIMEOUT_MS
        verify(isoDep).connect()
    }

    @Test
    fun `transceive forwards data to isoDep and returns response`() {
        val isoDep = mock<IsoDep>()
        val transceiver = IsoNfcTagTransceiver(isoDep)
        val command = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00)
        val response = byteArrayOf(0x90.toByte(), 0x00)
        whenever(isoDep.transceive(command)).thenReturn(response)

        val result = transceiver.transceive(command)

        assertThat(result).isEqualTo(response)
        verify(isoDep).transceive(command)
    }

    @Test
    fun `close closes isoDep`() {
        val isoDep = mock<IsoDep>()
        val transceiver = IsoNfcTagTransceiver(isoDep)

        transceiver.close()

        verify(isoDep).close()
    }

    @Test(expected = IOException::class)
    fun `open propagates IOException from connect`() {
        val isoDep = mock<IsoDep> {
            on { connect() } doThrow IOException("connect failed")
        }
        val transceiver = IsoNfcTagTransceiver(isoDep)

        transceiver.open()
    }

    @Test(expected = SecurityException::class)
    fun `open propagates SecurityException from connect`() {
        val isoDep = mock<IsoDep> {
            on { connect() } doThrow SecurityException("connect denied")
        }
        val transceiver = IsoNfcTagTransceiver(isoDep)

        transceiver.open()
    }

    @Test
    fun `factory create returns transceiver when tag supports IsoDep`() {
        val tag = mock<Tag>()
        val isoDep = mock<IsoDep>()

        mockStatic(IsoDep::class.java).use { mockedIsoDep ->
            mockedIsoDep.`when`<IsoDep> { IsoDep.get(tag) }.thenReturn(isoDep)

            val transceiver = IsoNfcTagTransceiver.Factory().create(tag)

            assertThat(transceiver).isNotNull()
            transceiver!!.open()
            verify(isoDep).timeout = TIMEOUT_MS
            verify(isoDep).connect()
        }
    }

    @Test
    fun `factory create returns null when tag does not support IsoDep`() {
        val tag = mock<Tag>()

        mockStatic(IsoDep::class.java).use { mockedIsoDep ->
            mockedIsoDep.`when`<IsoDep> { IsoDep.get(tag) }.thenReturn(null)

            val transceiver = IsoNfcTagTransceiver.Factory().create(tag)

            assertThat(transceiver).isNull()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 5000
    }
}
