package com.stripe.android.customersheet.data

internal fun <T> CustomerSheetDataResult<T>.asSuccess(): CustomerSheetDataResult.Success<T> {
    return this as CustomerSheetDataResult.Success<T>
}

internal fun <T> CustomerSheetDataResult<T>.asFailure(): CustomerSheetDataResult.Failure<T> {
    return this as CustomerSheetDataResult.Failure<T>
}
