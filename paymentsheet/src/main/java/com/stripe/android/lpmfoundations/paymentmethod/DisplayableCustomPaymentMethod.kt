package com.stripe.android.lpmfoundations.paymentmethod

import android.os.Parcelable
import com.stripe.android.core.strings.ResolvableString
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class DisplayableCustomPaymentMethod(
    val id: String,
    val displayName: String,
    val logoUrl: String,
    val subtitle: ResolvableString?,
    val doesNotCollectBillingDetails: Boolean,
) : Parcelable
