package com.stripe.android.financialconnections.appinitializer

import com.stripe.android.financialconnections.di.ApplicationComponent
import com.stripe.android.financialconnections.di.DaggerApplicationComponent

private var _appComponent: ApplicationComponent? = null

internal var appComponent: ApplicationComponent
    get() = requireNotNull(_appComponent)
    private set(value) {
        _appComponent = value
    }

internal class FinancialConnectionsDaggerInitializer : InitProvider() {
    override fun onCreate(): Boolean {
        appComponent = DaggerApplicationComponent.builder().application(application).build()
        return true
    }
}
