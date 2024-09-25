package com.stripe.android.paymentsheet.utils

import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider

internal enum class CustomerSheetTestType {
    AttachToCustomer,
    AttachToSetupIntent,
}

internal object CustomerSheetTestTypeProvider : TestParameterValuesProvider() {
    override fun provideValues(context: Context?): List<CustomerSheetTestType> {
        return CustomerSheetTestType.entries
    }
}
