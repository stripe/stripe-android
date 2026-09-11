package com.stripe.android.paymentelement.nfcscan

internal enum class NfcScanningIntegrationType(
    val runner: NfcScanningIntegrationTestRunner,
) {
    PaymentSheet(NfcScanningIntegrationTestRunner.PaymentSheetRunner),
    FlowController(NfcScanningIntegrationTestRunner.FlowControllerRunner),
    Embedded(NfcScanningIntegrationTestRunner.EmbeddedRunner),
}
