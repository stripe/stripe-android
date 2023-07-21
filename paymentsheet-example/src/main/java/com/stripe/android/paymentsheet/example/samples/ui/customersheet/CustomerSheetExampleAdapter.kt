package com.stripe.android.paymentsheet.example.samples.ui.customersheet

import com.stripe.android.customersheet.CustomerAdapter
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi

@OptIn(ExperimentalCustomerSheetApi::class)
class CustomerSheetExampleAdapter(
    private val adapter: CustomerAdapter
) : CustomerAdapter by adapter {
    internal var overrideCanCreateSetupIntents: Boolean = true

    override val canCreateSetupIntents: Boolean
        get() = overrideCanCreateSetupIntents
}
