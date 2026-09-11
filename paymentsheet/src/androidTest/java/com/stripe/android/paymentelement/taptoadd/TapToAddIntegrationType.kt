package com.stripe.android.paymentelement.taptoadd

internal sealed interface TapToAddIntegrationType {
    val runner: TapToAddIntegrationTestRunner

    enum class Continue(override val runner: TapToAddIntegrationTestRunner) : TapToAddIntegrationType {
        FlowController(TapToAddIntegrationTestRunner.FlowControllerRunner),
        Embedded(
            TapToAddIntegrationTestRunner.EmbeddedRunner(
                TapToAddIntegrationTestRunner.EmbeddedRunner.Mode.Continue
            )
        ),
    }

    enum class Complete(override val runner: TapToAddIntegrationTestRunner) : TapToAddIntegrationType {
        PaymentSheet(TapToAddIntegrationTestRunner.PaymentSheetRunner),
        Embedded(
            TapToAddIntegrationTestRunner.EmbeddedRunner(
                TapToAddIntegrationTestRunner.EmbeddedRunner.Mode.Confirm
            )
        ),
    }
}
