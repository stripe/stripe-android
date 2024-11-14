package com.stripe.android.paymentsheet.paymentdatacollection.bacs

import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.ConfirmationHandler

internal data class BacsMandateData(
    val name: String,
    val email: String,
    val accountNumber: String,
    val sortCode: String
) {
    companion object {
        fun fromConfirmationOption(
            confirmationOption: ConfirmationHandler.Option.BacsPaymentMethod,
        ): BacsMandateData? {
            val overrideParams = confirmationOption.createParams

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
