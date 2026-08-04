package com.stripe.android.paymentelement.embedded

import android.os.Parcelable
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.parcelize.Parcelize

internal sealed interface EmbeddedLaunchMode : Parcelable {
    @Parcelize
    data class Form(
        val selectedPaymentMethodCode: PaymentMethodCode,
        val paymentMethodLayout: PaymentSheet.PaymentMethodLayout,
    ) : EmbeddedLaunchMode

    @Parcelize
    data object Manage : EmbeddedLaunchMode

    @Parcelize
    data class PaymentOptions(
        val paymentMethodLayout: PaymentSheet.PaymentMethodLayout,
    ) : EmbeddedLaunchMode
}
