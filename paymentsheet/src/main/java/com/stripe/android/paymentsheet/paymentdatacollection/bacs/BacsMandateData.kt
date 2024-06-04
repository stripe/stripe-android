package com.stripe.android.paymentsheet.paymentdatacollection.bacs

import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.model.PaymentSelection

internal data class BacsMandateData(
    val name: String,
    val email: String,
    val accountNumber: String,
    val sortCode: String
) {
    companion object {
        fun fromPaymentSelection(
            paymentSelection: PaymentSelection.New.GenericPaymentMethod
        ): BacsMandateData? {
            val overrideParams = paymentSelection.paymentMethodCreateParams

            val bacsDebit = PaymentMethodCreateParams.createBacsFromParams(overrideParams)
            val name = PaymentMethodCreateParams.getNameFromParams(overrideParams)
            val email = PaymentMethodCreateParams.getEmailFromParams(overrideParams)

            return if (bacsDebit != null && name != null && email != null) {
                BacsMandateData(
                    name = name,
                    email = email,
                    accountNumber = bacsDebit.accountNumber,
                    sortCode = bacsDebit.sortCode
                )
            } else {
                null
            }
        }
    }
}
