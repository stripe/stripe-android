package com.stripe.android.paymentsheet.utils

import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider

internal sealed interface IntegrationType {
    data object Activity : IntegrationType
    data object Compose : IntegrationType
}

internal object IntegrationTypeProvider : TestParameterValuesProvider() {
    override fun provideValues(context: Context?): List<IntegrationType> {
        return listOf(
            IntegrationType.Activity,
            IntegrationType.Compose,
        )
    }
}
