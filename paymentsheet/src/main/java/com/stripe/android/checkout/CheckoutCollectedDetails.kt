package com.stripe.android.checkout

import android.os.Parcelable
import com.stripe.android.checkout.CheckoutController.Address
import com.stripe.android.paymentelement.CheckoutSessionPreview
import kotlinx.parcelize.Parcelize

@OptIn(CheckoutSessionPreview::class)
@Parcelize
internal data class CheckoutCollectedDetails(
    val email: String?,
    val shippingName: String? = null,
    val shippingAddress: Address.State? = null,
) : Parcelable
