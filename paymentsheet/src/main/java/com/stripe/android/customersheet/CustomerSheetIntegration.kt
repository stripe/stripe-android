package com.stripe.android.customersheet

import com.stripe.android.paymentsheet.ExperimentalCustomerSessionApi

@OptIn(ExperimentalCustomerSessionApi::class)
internal sealed class CustomerSheetIntegration(val type: Type) {
    enum class Type(val analyticsValue: String) {
        CustomerAdapter("customer_adapter"),
        CustomerSession("customer_session"),
    }

    class Adapter(
        val adapter: CustomerAdapter
    ) : CustomerSheetIntegration(Type.CustomerAdapter)

    class CustomerSession(
        val customerSessionProvider: CustomerSheet.CustomerSessionProvider
    ) : CustomerSheetIntegration(Type.CustomerSession)
}
