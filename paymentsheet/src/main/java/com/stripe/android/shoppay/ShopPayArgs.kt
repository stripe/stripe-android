package com.stripe.android.shoppay

import android.os.Parcelable
import com.stripe.android.elements.payment.PaymentSheet
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class ShopPayArgs(
    val shopPayConfiguration: PaymentSheet.ShopPayConfiguration,
    val publishableKey: String,
    val stripeAccountId: String?,
    val paymentElementCallbackIdentifier: String,
    val customerSessionClientSecret: String,
    val businessName: String
) : Parcelable
