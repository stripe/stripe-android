package com.stripe.android.common.nfcscan

internal suspend fun FakeIsoDep.assertSuccess(
    gpoCommand: ByteArray = NfcScanningActivityTestFixtures.ApduCommands.GPO_EMPTY_PDOL,
) {
    assertConnect()
    assertCommand(NfcScanningActivityTestFixtures.ApduCommands.SELECT_PPSE)
    assertCommand(NfcScanningActivityTestFixtures.ApduCommands.SELECT_VISA_APPLICATION)
    assertCommand(gpoCommand)
    assertClose()
}

internal suspend fun FakeIsoDep.assertUntilPpseSelectionCommand() {
    assertConnect()
    assertCommand(NfcScanningActivityTestFixtures.ApduCommands.SELECT_PPSE)
    assertClose()
}
