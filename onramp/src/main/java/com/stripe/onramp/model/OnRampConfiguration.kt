package com.stripe.onramp.model

import android.os.Parcelable
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.parcelize.Parcelize

@Parcelize
data class OnRampConfiguration(
    val publishableKey: String,
    val appearance: PaymentSheet.Appearance
) : Parcelable