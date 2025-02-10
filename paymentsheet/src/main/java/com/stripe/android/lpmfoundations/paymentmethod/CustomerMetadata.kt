package com.stripe.android.lpmfoundations.paymentmethod

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class CustomerMetadata(
    val isPaymentMethodSetAsDefaultEnabled: Boolean
) : Parcelable
