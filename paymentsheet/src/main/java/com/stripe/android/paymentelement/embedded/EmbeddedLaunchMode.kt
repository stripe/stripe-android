package com.stripe.android.paymentelement.embedded

import android.os.Parcelable
import com.stripe.android.model.PaymentMethodCode
import kotlinx.parcelize.Parcelize

internal sealed interface EmbeddedLaunchMode : Parcelable {
    @Parcelize
    data class Form(val selectedPaymentMethodCode: PaymentMethodCode) : EmbeddedLaunchMode

    @Parcelize
    data object Manage : EmbeddedLaunchMode
}
