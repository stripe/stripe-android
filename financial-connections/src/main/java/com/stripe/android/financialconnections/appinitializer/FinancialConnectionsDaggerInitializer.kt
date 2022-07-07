package com.stripe.android.financialconnections.appinitializer

import com.stripe.android.financialconnections.di.DaggerFinancialConnectionsApplicationComponent
import com.stripe.android.financialconnections.di.FinancialConnectionsApplicationComponent

private var _appComponent: FinancialConnectionsApplicationComponent? = null

internal var appComponent: FinancialConnectionsApplicationComponent
    get() = requireNotNull(_appComponent)
    private set(value) {
        _appComponent = value
    }

internal class FinancialConnectionsDaggerInitializer : InitProvider() {
    override fun onCreate(): Boolean {
        appComponent =
            DaggerFinancialConnectionsApplicationComponent.builder()
                .application(application)
                .build()
        return true
    }
}
