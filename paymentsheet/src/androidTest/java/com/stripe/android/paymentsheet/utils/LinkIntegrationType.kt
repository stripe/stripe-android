package com.stripe.android.paymentsheet.utils

import com.google.testing.junit.testparameterinjector.TestParameter

internal enum class LinkIntegrationType {
    PaymentSheet,
    FlowController,
}

internal object LinkIntegrationTypeProvider : TestParameter.TestParameterValuesProvider {
    override fun provideValues(): List<LinkIntegrationType> {
        return LinkIntegrationType.entries
    }
}
