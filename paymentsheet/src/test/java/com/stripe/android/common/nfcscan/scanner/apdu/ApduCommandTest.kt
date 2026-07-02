package com.stripe.android.common.nfcscan.scanner.apdu

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.nfcscan.scanner.FakeNfcTagTransceiver
import com.stripe.android.isInstanceOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class ApduCommandTest {
    @Test
    fun `transceiveWith sends request without data field when dataArray is null`() = test(
        transceiveResult = apduSuccessResponse(byteArrayOf()),
    ) {
        val command = TestApduCommand(
            dataArray = null,
            response = "ok",
        )

        val result = command.transceiveWith(transceiver)
        assertThat(result.getOrNull()).isEqualTo("ok")

        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(
            byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, 0x00),
        )
    }

    @Test
    fun `transceiveWith sends request with length-prefixed data when dataArray is present`() {
        val data = byteArrayOf(0x01, 0x02, 0x03)

        test(
            transceiveResult = apduSuccessResponse(byteArrayOf()),
        ) {
            val command = TestApduCommand(
                dataArray = data,
                response = "ok",
            )

            val result = command.transceiveWith(transceiver)
            assertThat(result.getOrNull()).isEqualTo("ok")

            assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(
                byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, 0x03) + data + byteArrayOf(0x00),
            )
        }
    }

    @Test
    fun `transceiveWith returns success when status word is 9000`() = test(
        transceiveResult = apduSuccessResponse(byteArrayOf()),
    ) {
        val command = TestApduCommand(response = "parsed")

        val result = command.transceiveWith(transceiver)
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo("parsed")

        assertThat(transceiver.transceiveCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `transceiveWith returns TooShort when response has fewer than two bytes`() = test(
        transceiveResult = byteArrayOf(0x90.toByte()),
    ) {
        val command = TestApduCommand(response = "parsed")

        val result = command.transceiveWith(transceiver)

        assertThat(result.exceptionOrNull()).isInstanceOf<ApduResponseError.TooShort>()
        assertThat(transceiver.transceiveCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `transceiveWith returns Command error when status word is not 9000`() = test(
        transceiveResult = byteArrayOf(0x6A.toByte(), 0x82.toByte()),
    ) {
        val command = TestApduCommand(response = "parsed")

        val result = command.transceiveWith(transceiver)

        val error = result.exceptionOrNull()
        assertThat(error).isInstanceOf<ApduResponseError.Command>()

        val commandError = error as ApduResponseError.Command
        assertThat(commandError.sw1).isEqualTo(0x6A.toByte())
        assertThat(commandError.sw2).isEqualTo(0x82.toByte())

        assertThat(transceiver.transceiveCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `transceiveWith returns Parsing error when response data cannot be parsed`() {
        val responseData = tlv(tag = 0x4F, value = byteArrayOf(0x01))

        test(
            transceiveResult = apduSuccessResponse(responseData)
        ) {
            val command = TestApduCommand(response = null)
            val result = command.transceiveWith(transceiver)

            assertThat(result.exceptionOrNull()).isInstanceOf<ApduResponseError.Parsing>()
            assertThat(transceiver.transceiveCalls.awaitItem()).isNotNull()
        }
    }

    private fun test(
        transceiveResult: ByteArray,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val fakeTransceiver = FakeNfcTagTransceiver(
            transceiveResult = transceiveResult,
        )

        block(Scenario(fakeTransceiver))

        fakeTransceiver.ensureAllEventsConsumed()
    }

    private class Scenario(
        val transceiver: FakeNfcTagTransceiver,
    )

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
