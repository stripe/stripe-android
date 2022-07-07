package com.stripe.android.financialconnections.appinitializer

import com.airbnb.mvrx.Mavericks

internal class FinancialConnectionsInitializer : InitProvider() {
    override fun onCreate(): Boolean {
        Mavericks.initialize(context = requireNotNull(context))
        return true
    }
}
