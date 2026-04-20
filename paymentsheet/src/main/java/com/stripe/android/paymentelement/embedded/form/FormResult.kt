package com.stripe.android.paymentelement.embedded.form

import android.os.Parcelable
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.CustomerState
import kotlinx.parcelize.Parcelize

internal sealed interface FormResult : Parcelable {
    val customerState: CustomerState?

    @Parcelize
    data class Complete(
        val selection: PaymentSelection?,
        val hasBeenConfirmed: Boolean,
        override val customerState: CustomerState?,
    ) : FormResult

    @Parcelize
    data class Cancelled(
        override val customerState: CustomerState?
    ) : FormResult
}
