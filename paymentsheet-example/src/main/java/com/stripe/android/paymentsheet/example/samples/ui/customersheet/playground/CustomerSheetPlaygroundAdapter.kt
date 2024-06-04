package com.stripe.android.paymentsheet.example.samples.ui.customersheet.playground

import com.stripe.android.customersheet.CustomerAdapter
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi

@OptIn(ExperimentalCustomerSheetApi::class)
class CustomerSheetPlaygroundAdapter(
    internal val overrideCanCreateSetupIntents: Boolean = true,
    private val adapter: CustomerAdapter
) : CustomerAdapter by adapter {
    override val canCreateSetupIntents: Boolean
        get() = overrideCanCreateSetupIntents
}
