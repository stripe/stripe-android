package com.stripe.android.customersheet.data

internal interface CustomerSheetCombinedDataSource :
    CustomerSheetPaymentMethodDataSource,
    CustomerSheetSavedSelectionDataSource
