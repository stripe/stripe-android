package com.stripe.android.checkout

import android.os.Parcelable
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class InternalState(
    val checkoutSessionResponse: CheckoutSessionResponse,
    val shippingName: String? = null,
) : Parcelable
