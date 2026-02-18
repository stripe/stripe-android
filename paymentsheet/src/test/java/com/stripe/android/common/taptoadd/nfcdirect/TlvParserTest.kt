package com.stripe.android.common.taptoadd.nfcdirect

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.taptoadd.nfcdirect.TlvParser.hexToByteArray
import com.stripe.android.common.taptoadd.nfcdirect.TlvParser.toHexString
import org.junit.Test

class TlvParserTest {

    @Test
    fun `parse single byte tag with single byte length`() {
        // Tag 5A (PAN), Length 08, Value 4111111111111111
        val data = "5A084111111111111111".hexToByteArray()
        val result = TlvParser.parse(data)

        assertThat(result).containsKey("5A")
        assertThat(result["5A"]?.toHexString()).isEqualTo("4111111111111111")
    }

    @Test
    fun `parse two byte tag`() {
        // Tag 5F24 (Expiry), Length 03, Value 261231 (Dec 2026)
        val data = "5F2403261231".hexToByteArray()
        val result = TlvParser.parse(data)

        assertThat(result).containsKey("5F24")
        assertThat(result["5F24"]?.toHexString()).isEqualTo("261231")
    }

    @Test
    fun `parse multiple TLV elements`() {
        // PAN + Expiry + Cardholder Name
        val data = "5A084111111111111111" +
            "5F2403261231" +
            "5F200E4A4F484E20444F452F4D522E20"
        val result = TlvParser.parse(data.hexToByteArray())

        assertThat(result).hasSize(3)
        assertThat(result).containsKey("5A")
        assertThat(result).containsKey("5F24")
        assertThat(result).containsKey("5F20")
    }

    @Test
    fun `parse constructed tag recursively`() {
        // Tag 6F (FCI Template) containing nested tags
        // 6F 0F (length 15)
        //   84 07 A0000000041010 (DF Name - Mastercard AID)
        //   A5 04 (FCI Proprietary)
        //     50 02 4D43 (App Label "MC")
        val data = "6F0F8407A0000000041010A5045002MC".hexToByteArray()
        val result = TlvParser.parse(data)

        // Should find both outer and inner tags
        assertThat(result).containsKey("6F")
        assertThat(result).containsKey("84")
        assertThat(result).containsKey("A5")
        assertThat(result).containsKey("50")
    }

    @Test
    fun `parse long form length`() {
        // Tag with length > 127 (using long form)
        // Tag 70, Length 81 80 (128 bytes), followed by 128 bytes of data
        val valueBytes = ByteArray(128) { 0xAA.toByte() }
        val data = byteArrayOf(0x70, 0x81.toByte(), 0x80.toByte()) + valueBytes
        val result = TlvParser.parse(data)

        assertThat(result).containsKey("70")
        assertThat(result["70"]?.size).isEqualTo(128)
    }

    @Test
    fun `findTag returns correct value`() {
        val data = "5A084111111111111111".hexToByteArray()
        val pan = TlvParser.findTag(data, "5A")

        assertThat(pan).isNotNull()
        assertThat(pan?.toHexString()).isEqualTo("4111111111111111")
    }

    @Test
    fun `findTag returns null for missing tag`() {
        val data = "5A084111111111111111".hexToByteArray()
        val missing = TlvParser.findTag(data, "5F24")

        assertThat(missing).isNull()
    }

    @Test
    fun `findTag is case insensitive`() {
        val data = "5a084111111111111111".hexToByteArray()
        val pan = TlvParser.findTag(data, "5A")

        assertThat(pan).isNotNull()
    }

    @Test
    fun `parseToElements returns structured data`() {
        val data = "5A084111111111111111".hexToByteArray()
        val elements = TlvParser.parseToElements(data)

        assertThat(elements).hasSize(1)
        assertThat(elements[0].tag).isEqualTo("5A")
        assertThat(elements[0].isConstructed).isFalse()
    }

    @Test
    fun `parseToElements identifies constructed tags`() {
        val data = "6F0F8407A0000000041010A5045002MC".hexToByteArray()
        val elements = TlvParser.parseToElements(data)

        assertThat(elements).hasSize(1)
        assertThat(elements[0].tag).isEqualTo("6F")
        assertThat(elements[0].isConstructed).isTrue()
        assertThat(elements[0].children).isNotEmpty()
    }

    @Test
    fun `hexToByteArray conversion`() {
        val hex = "DEADBEEF"
        val bytes = hex.hexToByteArray()

        assertThat(bytes.size).isEqualTo(4)
        assertThat(bytes[0]).isEqualTo(0xDE.toByte())
        assertThat(bytes[1]).isEqualTo(0xAD.toByte())
        assertThat(bytes[2]).isEqualTo(0xBE.toByte())
        assertThat(bytes[3]).isEqualTo(0xEF.toByte())
    }

    @Test
    fun `toHexString conversion`() {
        val bytes = byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())
        val hex = bytes.toHexString()

        assertThat(hex).isEqualTo("DEADBEEF")
    }

    @Test
    fun `handles empty input`() {
        val result = TlvParser.parse(byteArrayOf())

        assertThat(result).isEmpty()
    }

    @Test
    fun `skips padding bytes`() {
        // 00 and FF are padding bytes that should be skipped
        val data = "005A0841111111111111110000FF".hexToByteArray()
        val result = TlvParser.parse(data)

        // Should not have entries for padding
        assertThat(result).doesNotContainKey("00")
        assertThat(result).doesNotContainKey("FF")
    }

    @Test
    fun `parse Track 2 equivalent data`() {
        // Tag 57 (Track 2), typical format: PAN D Expiry Service Code
        val data = "5712411111111111111D2612201123456789".hexToByteArray()
        val result = TlvParser.parse(data)

        assertThat(result).containsKey("57")
        assertThat(result["57"]?.toHexString()).contains("4111111111111111")
    }

    @Test
    fun `parse real GPO response format 1`() {
        // Format 1 (tag 80): AIP (2 bytes) + AFL
        // 80 0E 1900 08010100 10010300 18010100
        val data = "800E190008010100100103001801010".hexToByteArray()
        val result = TlvParser.parse(data)

        assertThat(result).containsKey("80")
    }

    @Test
    fun `parse real GPO response format 2`() {
        // Format 2 (tag 77): constructed template with AIP and AFL tags
        // 77 0E 82 02 1900 94 08 08010100 10010300
        val data = "770E82021900940808010100100103".hexToByteArray()
        val result = TlvParser.parse(data)

        assertThat(result).containsKey("77")
        assertThat(result).containsKey("82") // AIP
        assertThat(result).containsKey("94") // AFL
    }
}
