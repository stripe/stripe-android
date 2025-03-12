package com.stripe.android.paymentsheet.utils

import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider

internal enum class MultipleInstancesTestType {
    RunWithFirst,
    RunWithSecond,
}

internal object MultipleInstancesTestTypeProvider : TestParameterValuesProvider() {
    override fun provideValues(context: Context?): List<MultipleInstancesTestType> {
        return MultipleInstancesTestType.entries
    }
}
