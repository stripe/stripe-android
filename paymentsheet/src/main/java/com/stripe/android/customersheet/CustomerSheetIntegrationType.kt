package com.stripe.android.customersheet

import com.stripe.android.paymentsheet.ExperimentalCustomerSessionApi

@OptIn(
    ExperimentalCustomerSheetApi::class,
    ExperimentalCustomerSessionApi::class
)
internal sealed interface CustomerSheetIntegrationType {
    class Adapter(val adapter: CustomerAdapter) : CustomerSheetIntegrationType

    class CustomerSession(
        val customerSessionProvider: CustomerSheet.CustomerSessionProvider,
    ) : CustomerSheetIntegrationType
}
