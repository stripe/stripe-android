package com.stripe.android.customersheet

import android.os.Parcelable
import com.stripe.android.elements.customersheet.CustomerSheet
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class CustomerSheetConfigureRequest(
    val configuration: CustomerSheet.Configuration,
) : Parcelable
