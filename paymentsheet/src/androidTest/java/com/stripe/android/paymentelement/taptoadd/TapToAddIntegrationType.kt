package com.stripe.android.paymentelement.taptoadd

import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider

sealed interface TapToAddIntegrationType {
    enum class Continue : TapToAddIntegrationType {
        FlowController, Embedded;

        internal object Provider : TestParameterValuesProvider() {
            override fun provideValues(context: Context?): List<Continue> {
                return Continue.entries
            }
        }
    }

    enum class Complete : TapToAddIntegrationType {
        PaymentSheet, Embedded;

        internal object Provider : TestParameterValuesProvider() {
            override fun provideValues(context: Context?): List<Complete> {
                return Complete.entries
            }
        }
    }
}
