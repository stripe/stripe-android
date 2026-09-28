package com.stripe.android.common.nfcscan.scanner.apdu

import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import org.junit.Test

internal class TlvParserTest {
    @Test
    fun `parse returns empty map for empty data`() {
        val result = TlvParser.parse(byteArrayOf())

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEmpty()
    }

    @Test
    fun `parse returns single TLV entry`() {
        val value = byteArrayOf(0x01, 0x02, 0x03)

        val result = TlvParser.parse(tlv(tag = 0x4F, value = value))

        assertThat(result.isSuccess).isTrue()

        val tlvMap = result.getOrThrow()
        assertThat(tlvMap).containsKey("4F")
        assertThat(tlvMap.getValue("4F").contentEquals(value)).isTrue()
    }

    @Test
    fun `parse returns multiple TLV entries`() {
        val aid = byteArrayOf(0xA0.toByte(), 0x00)
        val label = byteArrayOf(0x56, 0x49, 0x53, 0x41)
        val data = tlv(tag = 0x4F, value = aid) + tlv(tag = 0x50, value = label)

        val result = TlvParser.parse(data)

        assertThat(result.isSuccess).isTrue()

        val tlvMap = result.getOrThrow()
        assertThat(tlvMap).containsKey("4F")
        assertThat(tlvMap.getValue("4F").contentEquals(aid)).isTrue()
        assertThat(tlvMap).containsKey("50")
        assertThat(tlvMap.getValue("50").contentEquals(label)).isTrue()
    }

    @Test
    fun `parse handles two-byte tag`() {
        val value = byteArrayOf(0x03)

        val result = TlvParser.parse(tlv(tag = 0x9F.toByte(), tagContinuation = 0x38, value = value))

        assertThat(result.isSuccess).isTrue()

        val tlvMap = result.getOrThrow()
        assertThat(tlvMap).containsKey("9F38")
        assertThat(tlvMap.getValue("9F38").contentEquals(value)).isTrue()
    }

    @Test
    fun `parse handles extended length`() {
        val value = ByteArray(0x80) { 0xAB.toByte() }

        val result = TlvParser.parse(tlv(tag = 0x4F, value = value))

        assertThat(result.isSuccess).isTrue()

        val tlvMap = result.getOrThrow()
        assertThat(tlvMap).containsKey("4F")
        assertThat(tlvMap.getValue("4F").contentEquals(value)).isTrue()
    }

    @Test
    fun `parse flattens constructed TLV children into map`() {
        val aid = byteArrayOf(0xA0.toByte(), 0x00, 0x00, 0x00, 0x03)
        val fci = tlv(tag = 0x4F, value = aid)
        val data = tlv(tag = 0x6F, value = fci)

        val result = TlvParser.parse(data)

        assertThat(result.isSuccess).isTrue()

        val tlvMap = result.getOrThrow()
        assertThat(tlvMap).containsKey("6F")
        assertThat(tlvMap.getValue("6F").contentEquals(fci)).isTrue()
        assertThat(tlvMap).containsKey("4F")
        assertThat(tlvMap.getValue("4F").contentEquals(aid)).isTrue()
    }

    @Test
    fun `parse returns failure for malformed tlv data`() {
        val malformedData = byteArrayOf(0x4F, 0x05, 0x01)

        val result = TlvParser.parse(malformedData)

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf<IndexOutOfBoundsException>()
    }
}
