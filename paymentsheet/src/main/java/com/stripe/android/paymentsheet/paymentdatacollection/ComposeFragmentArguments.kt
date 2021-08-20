package com.stripe.android.paymentsheet.paymentdatacollection

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ComposeFragmentArguments(
    val supportedPaymentMethodName: String,
    val saveForFutureUseInitialVisibility: Boolean,
    val saveForFutureUseInitialValue: Boolean,
    val merchantName: String
) : Parcelable

