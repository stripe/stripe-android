package com.stripe.android.common.nfcscan.scanner.apdu

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.nfcscan.scanner.FakeNfcTagTransceiver
import com.stripe.android.isInstanceOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class SelectPpseCommandTest {
    @Test
    fun `transceiveWith sends SELECT PPSE command`() = test(
        transceiveResult = apduSuccessResponse(tlv(tag = 0x4F, value = VISA_AID)),
    ) {
        SelectPpseCommand.transceiveWith(transceiver)

        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_PPSE_REQUEST)
    }

    @Test
    fun `transceiveWith returns ApplicationIdentifier from AID tag`() = test(
        transceiveResult = apduSuccessResponse(tlv(tag = 0x4F, value = VISA_AID)),
    ) {
        val result = SelectPpseCommand.transceiveWith(transceiver)

        assertThat(result.getOrNull()).isEqualTo(ApplicationIdentifier(VISA_AID.toHexString()))
        assertThat(transceiver.transceiveCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `transceiveWith returns ApplicationIdentifier from DF name tag`() = test(
        transceiveResult = apduSuccessResponse(tlv(tag = 0x84.toByte(), value = MASTERCARD_AID)),
    ) {
        val result = SelectPpseCommand.transceiveWith(transceiver)

        assertThat(result.getOrNull())
            .isEqualTo(ApplicationIdentifier(MASTERCARD_AID.toHexString()))
        assertThat(transceiver.transceiveCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `transceiveWith returns ApplicationIdentifier from unknown tag`() = test(
        transceiveResult = apduSuccessResponse(tlv(tag = 0x42, value = VISA_AID)),
    ) {
        val result = SelectPpseCommand.transceiveWith(transceiver)
        assertThat(result.getOrNull())
            .isEqualTo(ApplicationIdentifier(VISA_AID.toHexString().uppercase()))
        assertThat(transceiver.transceiveCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `transceiveWith returns Invalid error when tlv contains no AID`() = test(
        transceiveResult = apduSuccessResponse(tlv(tag = 0x50, value = byteArrayOf(0x56, 0x49, 0x53, 0x41))),
    ) {
        val result = SelectPpseCommand.transceiveWith(transceiver)
        assertThat(result.exceptionOrNull()).isInstanceOf<ApduResponseError.Invalid>()
        assertThat(transceiver.transceiveCalls.awaitItem()).isNotNull()
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

    private companion object {
        val VISA_AID = byteArrayOf(
            0xA0.toByte(),
            0x00,
            0x00,
            0x00,
            0x03,
            0x10,
            0x10,
        )
        val MASTERCARD_AID = byteArrayOf(
            0xA0.toByte(),
            0x00,
            0x00,
            0x00,
            0x04,
            0x10,
            0x10,
        )
        val VISA_PREFIX = byteArrayOf(
            0xA0.toByte(),
            0x00,
            0x00,
            0x00,
            0x03,
        )
        val SELECT_PPSE_REQUEST = byteArrayOf(
            0x00,
            0xA4.toByte(),
            0x04,
            0x00,
            0x0E,
            0x32,
            0x50,
            0x41,
            0x59,
            0x2E,
            0x53,
            0x59,
            0x53,
            0x2E,
            0x44,
            0x44,
            0x46,
            0x30,
            0x31,
            0x00,
        )
    }
}
