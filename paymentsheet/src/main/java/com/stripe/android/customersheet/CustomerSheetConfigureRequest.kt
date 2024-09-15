package com.stripe.android.customersheet

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@OptIn(ExperimentalCustomerSheetApi::class)
@Parcelize
internal data class CustomerSheetConfigureRequest(
    val configuration: CustomerSheet.Configuration,
) : Parcelable
