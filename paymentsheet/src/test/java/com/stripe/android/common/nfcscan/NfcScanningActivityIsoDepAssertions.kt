package com.stripe.android.common.nfcscan

import com.google.common.truth.Truth.assertThat

internal suspend fun FakeIsoDep.assertSuccess(
    gpoCommand: ByteArray = NfcScanningActivityTestFixtures.ApduCommands.GPO_EMPTY_PDOL,
) {
    assertConnect()
    assertPpseSelection()
    assertSelectApplication()
    assertGetProcessingOptionsCommand(gpoCommand)
    assertClose()
}

internal suspend fun FakeIsoDep.assertUntilPpseSelectionCommand() {
    assertConnect()
    assertPpseSelection()
    assertClose()
}

private suspend fun FakeIsoDep.assertConnect() {
    assertThat(connectCalls.awaitItem()).isEqualTo(Unit)
}

private suspend fun FakeIsoDep.assertClose() {
    assertThat(closeCalls.awaitItem()).isEqualTo(Unit)
}

private suspend fun FakeIsoDep.assertPpseSelection() {
    assertThat(transceiveCalls.awaitItem())
        .isEqualTo(NfcScanningActivityTestFixtures.ApduCommands.SELECT_PPSE)
}

private suspend fun FakeIsoDep.assertSelectApplication() {
    assertThat(transceiveCalls.awaitItem())
        .isEqualTo(NfcScanningActivityTestFixtures.ApduCommands.SELECT_VISA_APPLICATION)
}

private suspend fun FakeIsoDep.assertGetProcessingOptionsCommand(expectedCommand: ByteArray) {
    assertThat(transceiveCalls.awaitItem()).isEqualTo(expectedCommand)
}
