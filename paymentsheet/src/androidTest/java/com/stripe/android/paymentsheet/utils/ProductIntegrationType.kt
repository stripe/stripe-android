package com.stripe.android.paymentsheet.utils

import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider

internal enum class ProductIntegrationType {
    PaymentSheet,
    FlowController,
}

internal object ProductIntegrationTypeProvider : TestParameterValuesProvider() {
    override fun provideValues(context: Context?): List<ProductIntegrationType> {
        return ProductIntegrationType.entries
    }
}
