package com.stripe.android.common.taptoadd.nfcdirect

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.taptoadd.nfcdirect.TlvParser.toHexString
import org.junit.Test

class EmvApduCommandsTest {

    @Test
    fun `SELECT_PPSE command is correct`() {
        val cmd = EmvApduCommands.SELECT_PPSE

        // Should be: 00 A4 04 00 0E 32504159...00
        assertThat(cmd[0]).isEqualTo(0x00.toByte()) // CLA
        assertThat(cmd[1]).isEqualTo(0xA4.toByte()) // INS: SELECT
        assertThat(cmd[2]).isEqualTo(0x04.toByte()) // P1: Select by name
        assertThat(cmd[3]).isEqualTo(0x00.toByte()) // P2: First occurrence
        assertThat(cmd[4]).isEqualTo(0x0E.toByte()) // Lc: 14 bytes

        // Name: "2PAY.SYS.DDF01"
        val name = String(cmd.sliceArray(5..18), Charsets.US_ASCII)
        assertThat(name).isEqualTo("2PAY.SYS.DDF01")

        // Le
        assertThat(cmd.last()).isEqualTo(0x00.toByte())
    }

    @Test
    fun `selectAid generates correct command for Visa`() {
        val cmd = EmvApduCommands.selectAid(EmvApduCommands.Aids.VISA)

        assertThat(cmd[0]).isEqualTo(0x00.toByte()) // CLA
        assertThat(cmd[1]).isEqualTo(0xA4.toByte()) // INS: SELECT
        assertThat(cmd[2]).isEqualTo(0x04.toByte()) // P1
        assertThat(cmd[3]).isEqualTo(0x00.toByte()) // P2
        assertThat(cmd[4]).isEqualTo(0x07.toByte()) // Lc: 7 bytes for Visa AID

        // Visa AID
        val aid = cmd.sliceArray(5..11).toHexString()
        assertThat(aid).isEqualTo("A0000000031010")
    }

    @Test
    fun `selectAid generates correct command for Mastercard`() {
        val cmd = EmvApduCommands.selectAid(EmvApduCommands.Aids.MASTERCARD)

        val aid = cmd.sliceArray(5..11).toHexString()
        assertThat(aid).isEqualTo("A0000000041010")
    }

    @Test
    fun `getProcessingOptions with no PDOL`() {
        val cmd = EmvApduCommands.getProcessingOptions(null)

        assertThat(cmd[0]).isEqualTo(0x80.toByte()) // CLA: EMV
        assertThat(cmd[1]).isEqualTo(0xA8.toByte()) // INS: GPO
        assertThat(cmd[2]).isEqualTo(0x00.toByte()) // P1
        assertThat(cmd[3]).isEqualTo(0x00.toByte()) // P2
        assertThat(cmd[4]).isEqualTo(0x02.toByte()) // Lc: 2 (empty PDOL wrapper)
        assertThat(cmd[5]).isEqualTo(0x83.toByte()) // Tag 83
        assertThat(cmd[6]).isEqualTo(0x00.toByte()) // Length 0
    }

    @Test
    fun `getProcessingOptions with PDOL data`() {
        val pdolData = byteArrayOf(0x00, 0x00, 0x00, 0x00) // 4 bytes of PDOL data
        val cmd = EmvApduCommands.getProcessingOptions(pdolData)

        assertThat(cmd[0]).isEqualTo(0x80.toByte()) // CLA: EMV
        assertThat(cmd[1]).isEqualTo(0xA8.toByte()) // INS: GPO
        assertThat(cmd[4]).isEqualTo(0x06.toByte()) // Lc: 6 (tag + length + 4 data)
        assertThat(cmd[5]).isEqualTo(0x83.toByte()) // Tag 83
        assertThat(cmd[6]).isEqualTo(0x04.toByte()) // Length 4
    }

    @Test
    fun `readRecord generates correct command`() {
        val cmd = EmvApduCommands.readRecord(sfi = 1, recordNumber = 1)

        assertThat(cmd[0]).isEqualTo(0x00.toByte()) // CLA
        assertThat(cmd[1]).isEqualTo(0xB2.toByte()) // INS: READ RECORD
        assertThat(cmd[2]).isEqualTo(0x01.toByte()) // P1: Record 1
        // P2: SFI 1 shifted left by 3, plus 0x04
        assertThat(cmd[3]).isEqualTo(0x0C.toByte()) // (1 << 3) | 0x04 = 12
        assertThat(cmd[4]).isEqualTo(0x00.toByte()) // Le
    }

    @Test
    fun `readRecord with different SFI and record`() {
        val cmd = EmvApduCommands.readRecord(sfi = 2, recordNumber = 3)

        assertThat(cmd[2]).isEqualTo(0x03.toByte()) // P1: Record 3
        assertThat(cmd[3]).isEqualTo(0x14.toByte()) // P2: (2 << 3) | 0x04 = 20
    }

    @Test(expected = IllegalArgumentException::class)
    fun `readRecord rejects invalid SFI`() {
        EmvApduCommands.readRecord(sfi = 31, recordNumber = 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `readRecord rejects SFI of 0`() {
        EmvApduCommands.readRecord(sfi = 0, recordNumber = 1)
    }

    @Test
    fun `parseAfl extracts entries correctly`() {
        // AFL format: SFI<<3 | firstRec | lastRec | odaCount
        // SFI 1, records 1-3
        // SFI 2, record 1 only
        val afl = byteArrayOf(
            0x08, 0x01, 0x03, 0x00,  // SFI 1, records 1-3
            0x10, 0x01, 0x01, 0x00   // SFI 2, record 1
        )

        val entries = EmvApduCommands.parseAfl(afl)

        assertThat(entries).hasSize(2)
        assertThat(entries[0]).isEqualTo(Triple(1, 1, 3))
        assertThat(entries[1]).isEqualTo(Triple(2, 1, 1))
    }

    @Test
    fun `parseAfl handles empty AFL`() {
        val entries = EmvApduCommands.parseAfl(byteArrayOf())

        assertThat(entries).isEmpty()
    }

    @Test
    fun `parseAfl handles malformed AFL`() {
        // Not a multiple of 4
        val entries = EmvApduCommands.parseAfl(byteArrayOf(0x08, 0x01, 0x03))

        assertThat(entries).isEmpty()
    }

    @Test
    fun `isSuccess detects 9000 status`() {
        val response = byteArrayOf(0x00, 0x00, 0x90.toByte(), 0x00)

        assertThat(EmvApduCommands.isSuccess(response)).isTrue()
    }

    @Test
    fun `isSuccess rejects non-9000 status`() {
        val response6A82 = byteArrayOf(0x00, 0x6A.toByte(), 0x82.toByte())
        val response6985 = byteArrayOf(0x00, 0x69.toByte(), 0x85.toByte())

        assertThat(EmvApduCommands.isSuccess(response6A82)).isFalse()
        assertThat(EmvApduCommands.isSuccess(response6985)).isFalse()
    }

    @Test
    fun `getResponseData strips status bytes`() {
        val response = byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0x90.toByte(), 0x00)
        val data = EmvApduCommands.getResponseData(response)

        assertThat(data.size).isEqualTo(2)
        assertThat(data[0]).isEqualTo(0xAA.toByte())
        assertThat(data[1]).isEqualTo(0xBB.toByte())
    }

    @Test
    fun `getStatusWord extracts status correctly`() {
        val response9000 = byteArrayOf(0x00, 0x90.toByte(), 0x00)
        val response6A82 = byteArrayOf(0x00, 0x6A.toByte(), 0x82.toByte())

        assertThat(EmvApduCommands.getStatusWord(response9000)).isEqualTo(0x9000)
        assertThat(EmvApduCommands.getStatusWord(response6A82)).isEqualTo(0x6A82)
    }

    @Test
    fun `Aids constants are correct`() {
        assertThat(EmvApduCommands.Aids.VISA.toHexString()).isEqualTo("A0000000031010")
        assertThat(EmvApduCommands.Aids.MASTERCARD.toHexString()).isEqualTo("A0000000041010")
        assertThat(EmvApduCommands.Aids.AMEX.toHexString()).isEqualTo("A00000002501")
        assertThat(EmvApduCommands.Aids.DISCOVER.toHexString()).isEqualTo("A0000001523010")
    }
}
