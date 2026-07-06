package com.stripe.android.common.nfcscan.scanner.apdu

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.nfcscan.scanner.FakeNfcTagTransceiver
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class SelectApplicationCommandTest {
    @Test
    fun `transceiveWith sends SELECT application command`() = test(
        transceiveResult = apduSuccessResponse(byteArrayOf()),
    ) {
        SelectApplicationCommand(ApplicationIdentifier(VISA_AID.toHexString()))
            .transceiveWith(transceiver)

        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_VISA_APPLICATION_REQUEST)
    }

    @Test
    fun `transceiveWith returns success when status word is 9000`() = test(
        transceiveResult = apduSuccessResponse(byteArrayOf()),
    ) {
        val result = SelectApplicationCommand(ApplicationIdentifier(VISA_AID.toHexString()))
            .transceiveWith(transceiver)

        assertThat(result.isSuccess).isTrue()
        assertThat(transceiver.transceiveCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `transceiveWith returns Command error when status word is not 9000`() = test(
        transceiveResult = byteArrayOf(0x6A.toByte(), 0x82.toByte()),
    ) {
        val result = SelectApplicationCommand(ApplicationIdentifier(VISA_AID.toHexString()))
            .transceiveWith(transceiver)

        assertThat(result.isFailure).isTrue()
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
        val SELECT_VISA_APPLICATION_REQUEST = byteArrayOf(
            0x00,
            0xA4.toByte(),
            0x04,
            0x00,
            0x07,
            0xA0.toByte(),
            0x00,
            0x00,
            0x00,
            0x03,
            0x10,
            0x10,
            0x00,
        )
    }
}
