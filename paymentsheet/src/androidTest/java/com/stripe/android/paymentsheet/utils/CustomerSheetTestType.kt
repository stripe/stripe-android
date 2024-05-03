package com.stripe.android.paymentsheet.utils

import com.google.testing.junit.testparameterinjector.TestParameter

internal enum class CustomerSheetTestType {
    AttachToCustomer,
    AttachToSetupIntent,
}

internal object CustomerSheetTestTypeProvider : TestParameter.TestParameterValuesProvider {
    override fun provideValues(): List<CustomerSheetTestType> {
        return CustomerSheetTestType.entries
    }
}
