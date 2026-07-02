package com.stripe.android.common.nfcscan.scanner.adpu

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.nfcscan.scanner.FakeNfcTagTransceiver
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class ApduCommandTest {
    @Test
    fun `transceiveWith sends request without data field when dataArray is null`() = runTest {
        val fakeTransceiver = FakeNfcTagTransceiver(
            transceiveResult = apduSuccessResponse(byteArrayOf()),
        )
        val command = TestApduCommand(
            dataArray = null,
            response = "ok",
        )

        val result = command.transceiveWith(fakeTransceiver)

        assertThat(result.getOrNull()).isEqualTo("ok")
        assertThat(fakeTransceiver.transceiveCalls.awaitItem()).isEqualTo(
            byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, 0x00),
        )
        fakeTransceiver.ensureAllEventsConsumed()
    }

    @Test
    fun `transceiveWith sends request with length-prefixed data when dataArray is present`() = runTest {
        val data = byteArrayOf(0x01, 0x02, 0x03)
        val fakeTransceiver = FakeNfcTagTransceiver(
            transceiveResult = apduSuccessResponse(byteArrayOf()),
        )
        val command = TestApduCommand(
            dataArray = data,
            response = "ok",
        )

        val result = command.transceiveWith(fakeTransceiver)

        assertThat(result.getOrNull()).isEqualTo("ok")
        assertThat(fakeTransceiver.transceiveCalls.awaitItem()).isEqualTo(
            byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, 0x03) + data + byteArrayOf(0x00),
        )
        fakeTransceiver.ensureAllEventsConsumed()
    }

    @Test
    fun `transceiveWith returns success when status word is 9000`() = runTest {
        val fakeTransceiver = FakeNfcTagTransceiver(
            transceiveResult = apduSuccessResponse(byteArrayOf()),
        )
        val command = TestApduCommand(response = "parsed")

        val result = command.transceiveWith(fakeTransceiver)

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo("parsed")
        fakeTransceiver.transceiveCalls.awaitItem()
        fakeTransceiver.ensureAllEventsConsumed()
    }

    @Test
    fun `transceiveWith returns TooShort when response has fewer than two bytes`() = runTest {
        val fakeTransceiver = FakeNfcTagTransceiver(
            transceiveResult = byteArrayOf(0x90.toByte()),
        )
        val command = TestApduCommand(response = "parsed")

        val result = command.transceiveWith(fakeTransceiver)

        assertThat(result.exceptionOrNull()).isInstanceOf(ApduResponseError.TooShort::class.java)
        fakeTransceiver.transceiveCalls.awaitItem()
        fakeTransceiver.ensureAllEventsConsumed()
    }

    @Test
    fun `transceiveWith returns Command error when status word is not 9000`() = runTest {
        val fakeTransceiver = FakeNfcTagTransceiver(
            transceiveResult = byteArrayOf(0x6A.toByte(), 0x82.toByte()),
        )
        val command = TestApduCommand(response = "parsed")

        val result = command.transceiveWith(fakeTransceiver)

        val error = result.exceptionOrNull()
        assertThat(error).isInstanceOf(ApduResponseError.Command::class.java)
        assertThat((error as ApduResponseError.Command).sw1).isEqualTo(0x6A.toByte())
        assertThat(error.sw2).isEqualTo(0x82.toByte())
        fakeTransceiver.transceiveCalls.awaitItem()
        fakeTransceiver.ensureAllEventsConsumed()
    }

    @Test
    fun `transceiveWith returns Parsing error when response data cannot be parsed`() = runTest {
        val responseData = tlv(tag = 0x4F, value = byteArrayOf(0x01))
        val fakeTransceiver = FakeNfcTagTransceiver(
            transceiveResult = apduSuccessResponse(responseData),
        )
        val command = TestApduCommand(response = null)

        val result = command.transceiveWith(fakeTransceiver)

        assertThat(result.exceptionOrNull()).isInstanceOf(ApduResponseError.Parsing::class.java)
        fakeTransceiver.transceiveCalls.awaitItem()
        fakeTransceiver.ensureAllEventsConsumed()
    }

    private class TestApduCommand(
        override val classByte: Byte = 0x00,
        override val instructionByte: Byte = 0xA4.toByte(),
        override val firstParameterByte: Byte = 0x04,
        override val secondParameterByte: Byte = 0x00,
        override val dataArray: ByteArray? = null,
        private val response: String?,
    ) : ApduCommand<String>() {
        override fun responseData(tlv: Map<String, ByteArray>): String? = response
    }
}
