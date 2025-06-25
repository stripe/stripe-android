package com.stripe.android.shoppay.bridge

internal interface ShopPayConfirmationState {
    data object Pending : ShopPayConfirmationState
    data class Success(
        val externalSourceId: String,
        val billingDetails: ECEBillingDetails,
    ) : ShopPayConfirmationState
    data class Failure(val cause: Throwable) : ShopPayConfirmationState
}
