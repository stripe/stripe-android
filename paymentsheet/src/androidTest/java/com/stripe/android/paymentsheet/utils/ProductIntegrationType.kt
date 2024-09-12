package com.stripe.android.paymentsheet.utils

import com.google.testing.junit.testparameterinjector.TestParameter

internal enum class ProductIntegrationType {
    PaymentSheet,
    FlowController,
}

internal object ProductIntegrationTypeProvider : TestParameter.TestParameterValuesProvider {
    override fun provideValues(): List<ProductIntegrationType> {
        return ProductIntegrationType.entries
    }
}
