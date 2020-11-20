package com.stripe.android.paymentsheet.model

import android.os.Parcelable
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import kotlinx.parcelize.Parcelize

internal sealed class PaymentSelection : Parcelable {
    @Parcelize
    object GooglePay : PaymentSelection()

    @Parcelize
    data class Saved(
        val paymentMethod: PaymentMethod
    ) : PaymentSelection()

    @Parcelize
    data class New(
        val paymentMethodCreateParams: PaymentMethodCreateParams
    ) : PaymentSelection()
}
