package com.stripe.android.paymentsheet.utils

import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider

internal enum class IntegrationType {
    Activity,
    Compose,
}

internal object IntegrationTypeProvider : TestParameterValuesProvider() {
    override fun provideValues(context: Context?): List<IntegrationType> {
        return IntegrationType.entries
    }
}
