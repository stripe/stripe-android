package com.stripe.android.paymentsheet.paymentdatacollection.bacs

import com.stripe.android.core.strings.ResolvableString

internal data class BacsMandateConfirmationViewState(
    val email: String,
    val nameOnAccount: String,
    val sortCode: String,
    val accountNumber: String,
    val payer: ResolvableString,
    val supportAddressAsHtml: ResolvableString,
    val debitGuaranteeAsHtml: ResolvableString
)
