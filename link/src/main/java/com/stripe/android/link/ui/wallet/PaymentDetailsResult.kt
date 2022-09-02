package com.stripe.android.link.ui.wallet

import android.os.Parcelable
import com.stripe.android.link.ui.ErrorMessage
import kotlinx.parcelize.Parcelize

/**
 * The result of an operation to add or edit a PaymentDetails.
 */
internal sealed class PaymentDetailsResult : Parcelable {

    @Parcelize
    class Success(val itemId: String) : PaymentDetailsResult()

    @Parcelize
    object Cancelled : PaymentDetailsResult()

    @Parcelize
    class Failure(val error: ErrorMessage) : PaymentDetailsResult()

    companion object {
        const val KEY = "PaymentDetailsResult"
    }
}
