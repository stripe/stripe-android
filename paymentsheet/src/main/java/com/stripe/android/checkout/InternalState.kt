package com.stripe.android.checkout

import android.os.Parcelable
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import kotlinx.parcelize.Parcelize

@OptIn(CheckoutSessionPreview::class)
@Parcelize
internal data class InternalState(
    val key: String,
    val configuration: Checkout.Configuration.State,
    val checkoutSessionResponse: CheckoutSessionResponse,
    val shippingName: String? = null,
    val billingName: String? = null,
    val shippingPhoneNumber: String? = null,
    val billingPhoneNumber: String? = null,
    val shippingAddress: Address.State? = null,
    val billingAddress: Address.State? = null,
    val integrationLaunched: Boolean = false,
) : Parcelable {
    val initializationMode: PaymentElementLoader.InitializationMode.CheckoutSession
        get() = PaymentElementLoader.InitializationMode.CheckoutSession(
            instancesKey = key,
            checkoutSessionResponse = checkoutSessionResponse,
        )
}
