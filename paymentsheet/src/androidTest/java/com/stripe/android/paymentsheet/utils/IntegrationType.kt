package com.stripe.android.paymentsheet.utils

import com.google.testing.junit.testparameterinjector.TestParameter

internal enum class IntegrationType {
    Activity,
    Compose,
}

internal object IntegrationTypeProvider : TestParameter.TestParameterValuesProvider {
    override fun provideValues(): List<IntegrationType> {
        return IntegrationType.values().toList()
    }
}
