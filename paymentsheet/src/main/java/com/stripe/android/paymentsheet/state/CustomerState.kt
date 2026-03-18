package com.stripe.android.paymentsheet.state

import android.os.Parcelable
import com.stripe.android.model.PaymentMethod
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class CustomerState(
    val paymentMethods: List<PaymentMethod>,
    val defaultPaymentMethodId: String?,
) : Parcelable
