package com.stripe.android.customersheet

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class CustomerSheetConfigureRequest(
    val configuration: CustomerSheet.Configuration,
) : Parcelable
