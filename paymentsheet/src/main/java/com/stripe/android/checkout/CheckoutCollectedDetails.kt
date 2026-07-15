package com.stripe.android.checkout

import android.os.Parcelable
import com.stripe.android.paymentelement.CheckoutSessionPreview
import kotlinx.parcelize.Parcelize

@OptIn(CheckoutSessionPreview::class)
@Parcelize
internal data class CheckoutCollectedDetails(
    val shippingName: String? = null,
    val billingName: String? = null,
    val shippingPhoneNumber: String? = null,
    val billingPhoneNumber: String? = null,
    val shippingAddress: Address.State? = null,
    val billingAddress: Address.State? = null,
) : Parcelable
