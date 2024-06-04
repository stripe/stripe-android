package com.stripe.android.paymentsheet.paymentdatacollection.bacs

internal sealed interface BacsMandateConfirmationViewAction {
    object OnConfirmPressed : BacsMandateConfirmationViewAction
    object OnModifyDetailsPressed : BacsMandateConfirmationViewAction
    object OnBackPressed : BacsMandateConfirmationViewAction
}
