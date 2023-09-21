package com.stripe.android.paymentsheet.paymentdatacollection.bacs

internal sealed interface BacsMandateConfirmationEffect {
    data class CloseWithResult(val result: BacsMandateConfirmationResult) : BacsMandateConfirmationEffect
}
