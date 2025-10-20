package com.stripe.android.shoppay.bridge

internal sealed interface ShopPayConfirmationState {
    data object Pending : ShopPayConfirmationState
    data class Success(
        val externalSourceId: String,
        val billingDetails: ECEBillingDetails,
        val shippingAddressData: ECEShippingAddressData?
    ) : ShopPayConfirmationState
    data class Failure(val cause: Throwable) : ShopPayConfirmationState
}
