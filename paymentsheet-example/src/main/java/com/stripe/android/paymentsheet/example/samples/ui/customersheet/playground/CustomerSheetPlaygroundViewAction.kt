package com.stripe.android.paymentsheet.example.samples.ui.customersheet.playground

sealed class CustomerSheetPlaygroundViewAction {
    object ToggleSetupIntentEnabled : CustomerSheetPlaygroundViewAction()
    object ToggleGooglePayEnabled : CustomerSheetPlaygroundViewAction()

    object ToggleExistingCustomer : CustomerSheetPlaygroundViewAction()
}
