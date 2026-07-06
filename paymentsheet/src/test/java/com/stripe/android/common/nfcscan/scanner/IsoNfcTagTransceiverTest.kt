package com.stripe.android.common.nfcscan.scanner

import android.nfc.Tag
import android.nfc.tech.IsoDep
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.IOException

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
        val isoDep = createIsoDep()
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
        val isoDep = createIsoDep()
        val transceiver = IsoNfcTagTransceiver(isoDep)

        transceiver.close()

        verify(isoDep).close()
    }

    @Test(expected = IOException::class)
    fun `open propagates error from connect`() {
        val isoDep = createIsoDep(IOException("connect failed"))
        val transceiver = IsoNfcTagTransceiver(isoDep)

        transceiver.open()
    }

    @Test
    fun `factory create returns transceiver when tag supports IsoDep`() {
        val tag = createTag()
        val isoDep = createIsoDep()

        mockStatic(IsoDep::class.java).use { mockedIsoDep ->
            mockedIsoDep.`when`<IsoDep> { IsoDep.get(tag) }.thenReturn(isoDep)

            val transceiver = IsoNfcTagTransceiver.Factory().create(tag)

            assertThat(transceiver).isNotNull()
            requireNotNull(transceiver).open()

            verify(isoDep).timeout = TIMEOUT_MS
            verify(isoDep).connect()
        }
    }

    @Test
    fun `factory create returns null when tag does not support IsoDep`() {
        val tag = createTag()

        mockStatic(IsoDep::class.java).use { mockedIsoDep ->
            mockedIsoDep.`when`<IsoDep> { IsoDep.get(tag) }.thenReturn(null)

            val transceiver = IsoNfcTagTransceiver.Factory().create(tag)

            assertThat(transceiver).isNull()
        }
    }

    private fun createIsoDep(
        connectFailure: Exception? = null
    ) = mock<IsoDep> {
        connectFailure?.let {
            on { connect() } doThrow it
        }
    }

    private fun createTag() = mock<Tag>()

    private companion object {
        const val TIMEOUT_MS = 5000
    }
}
