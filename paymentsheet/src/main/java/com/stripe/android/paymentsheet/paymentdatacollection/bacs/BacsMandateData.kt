package com.stripe.android.paymentsheet.paymentdatacollection.bacs

import android.os.Parcelable
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentelement.confirmation.bacs.BacsConfirmationOption
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class BacsMandateData(
    val name: String,
    val email: String,
    val accountNumber: String,
    val sortCode: String
) : Parcelable {
    companion object {
        fun fromConfirmationOption(
            confirmationOption: BacsConfirmationOption,
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
