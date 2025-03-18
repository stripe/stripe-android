package com.stripe.android.paymentsheet.utils

import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.CreateIntentResult

internal sealed class ConfirmationType(val createIntentCallback: CreateIntentCallback?) {

    data object IntentFirst : ConfirmationType(
        createIntentCallback = null
    )

    data object DeferredClientSideConfirmation : ConfirmationType(
        createIntentCallback = { _, _ ->
            CreateIntentResult.Success(clientSecret = "cs_1234")
        },
    )
}

internal object ConfirmationTypeProvider : TestParameterValuesProvider() {
    override fun provideValues(context: Context?): List<ConfirmationType> {
        return listOf(ConfirmationType.IntentFirst, ConfirmationType.DeferredClientSideConfirmation)
    }
}