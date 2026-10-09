package com.stripe.android.paymentelement.confirmation

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/** Customer input captured when a Checkout confirmation begins. */
@Parcelize
internal data class CheckoutConfirmationData(
    val collectedEmail: String?,
) : Parcelable
