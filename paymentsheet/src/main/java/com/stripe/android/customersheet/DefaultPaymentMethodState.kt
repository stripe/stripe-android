package com.stripe.android.customersheet

internal sealed class DefaultPaymentMethodState {
    data class Enabled(val defaultPaymentMethodId: String?) : DefaultPaymentMethodState()
    data object Disabled : DefaultPaymentMethodState()
}
