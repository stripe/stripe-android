package com.stripe.android.paymentelement.taptoadd

import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider

internal sealed interface TapToAddIntegrationType {
    val runner: TapToAddIntegrationTestRunner

    enum class Continue(override val runner: TapToAddIntegrationTestRunner) : TapToAddIntegrationType {
        FlowController(TapToAddIntegrationTestRunner.FlowControllerRunner),
        Embedded(
            TapToAddIntegrationTestRunner.EmbeddedRunner(
                TapToAddIntegrationTestRunner.EmbeddedRunner.Mode.Continue
            )
        );

        internal object Provider : TestParameterValuesProvider() {
            override fun provideValues(context: Context?): List<Continue> {
                return Continue.entries
            }
        }
    }

    enum class Complete(override val runner: TapToAddIntegrationTestRunner) : TapToAddIntegrationType {
        PaymentSheet(TapToAddIntegrationTestRunner.PaymentSheetRunner),
        Embedded(
            TapToAddIntegrationTestRunner.EmbeddedRunner(
                TapToAddIntegrationTestRunner.EmbeddedRunner.Mode.Confirm
            )
        );

        internal object Provider : TestParameterValuesProvider() {
            override fun provideValues(context: Context?): List<Complete> {
                return Complete.entries
            }
        }
    }

    object Provider : TestParameterValuesProvider() {
        override fun provideValues(context: Context?): List<TapToAddIntegrationType> {
            return Complete.entries + Continue.entries
        }
    }
}
