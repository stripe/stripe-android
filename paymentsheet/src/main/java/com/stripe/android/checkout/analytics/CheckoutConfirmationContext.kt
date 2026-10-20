package com.stripe.android.checkout.analytics

import android.os.Parcelable
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class CheckoutConfirmationContext(
    val paymentSelection: PaymentSelection,
    val invocationSource: InvocationSource,
    val activeLinkSession: Boolean,
    val checkoutConfigId: String?,
) : Parcelable {
    enum class InvocationSource(val code: String) {
        Merchant("merchant"),
        ExpressCheckoutElement("express_checkout_element"),
    }
}
