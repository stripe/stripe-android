package com.stripe.android.paymentsheet.paymentdatacollection.bacs

internal data class BacsMandateConfirmationViewState(
    val email: String,
    val nameOnAccount: String,
    val sortCode: String,
    val accountNumber: String,
    val supportAddressAsHtml: String,
    val debitGuaranteeAsHtml: String
)
