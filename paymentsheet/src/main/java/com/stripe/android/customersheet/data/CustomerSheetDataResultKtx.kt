package com.stripe.android.customersheet.data

import com.stripe.android.customersheet.CustomerAdapter
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi

@OptIn(ExperimentalCustomerSheetApi::class)
internal fun <T> CustomerAdapter.Result<T>.toCustomerSheetDataResult(): CustomerSheetDataResult<T> {
    return when (this) {
        is CustomerAdapter.Result.Success -> CustomerSheetDataResult.success(value)
        is CustomerAdapter.Result.Failure -> CustomerSheetDataResult.failure(
            cause = cause,
            displayMessage = displayMessage,
        )
    }
}
