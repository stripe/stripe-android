package com.stripe.android.paymentsheet.example.samples.ui.customersheet.playground

sealed class CustomerSheetPlaygroundViewAction {
    object ToggleSetupIntentEnabled : CustomerSheetPlaygroundViewAction()
    object ToggleGooglePayEnabled : CustomerSheetPlaygroundViewAction()
    object ToggleExistingCustomer : CustomerSheetPlaygroundViewAction()
    object ToggleUseDefaultBillingAddress : CustomerSheetPlaygroundViewAction()
    object ToggleAttachDefaultBillingAddress : CustomerSheetPlaygroundViewAction()
    object ToggleAchEnabled : CustomerSheetPlaygroundViewAction()
    data class UpdateBillingNameCollection(val value: String) : CustomerSheetPlaygroundViewAction()
    data class UpdateBillingEmailCollection(val value: String) : CustomerSheetPlaygroundViewAction()
    data class UpdateBillingPhoneCollection(val value: String) : CustomerSheetPlaygroundViewAction()
    data class UpdateBillingAddressCollection(val value: String) : CustomerSheetPlaygroundViewAction()
    data class UpdateMerchantCountryCode(val code: String) : CustomerSheetPlaygroundViewAction()
}
