package com.stripe.android.customersheet.data

internal sealed interface CustomerPaymentOption {
    data object GooglePay : CustomerPaymentOption

    data object Link : CustomerPaymentOption

    data class Saved(val id: String) : CustomerPaymentOption

    data object None : CustomerPaymentOption
}
