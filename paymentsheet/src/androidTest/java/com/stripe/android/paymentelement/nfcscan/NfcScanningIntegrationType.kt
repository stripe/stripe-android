package com.stripe.android.paymentelement.nfcscan

import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider

internal enum class NfcScanningIntegrationType(
    val runner: NfcScanningIntegrationTestRunner,
) {
    PaymentSheet(NfcScanningIntegrationTestRunner.PaymentSheetRunner),
    FlowController(NfcScanningIntegrationTestRunner.FlowControllerRunner),
    Embedded(NfcScanningIntegrationTestRunner.EmbeddedRunner);

    internal object Provider : TestParameterValuesProvider() {
        override fun provideValues(context: Context?): List<NfcScanningIntegrationType> {
            return entries
        }
    }
}
