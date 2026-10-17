package com.stripe.android.paymentsheet.utils

import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider

internal sealed interface CustomerSheetTestType {
    data object AttachToCustomer : CustomerSheetTestType
    data object AttachToSetupIntent : CustomerSheetTestType
    data object CustomerSession : CustomerSheetTestType
}

internal object CustomerSheetTestTypeProvider : TestParameterValuesProvider() {
    override fun provideValues(context: Context?): List<CustomerSheetTestType> {
        return listOf(CustomerSheetTestType.AttachToCustomer, CustomerSheetTestType.AttachToCustomer)
    }
}
