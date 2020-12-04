package com.stripe.android.paymentsheet

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PaymentSheetGooglePayConfig(
    val countryCode: String
) : Parcelable
