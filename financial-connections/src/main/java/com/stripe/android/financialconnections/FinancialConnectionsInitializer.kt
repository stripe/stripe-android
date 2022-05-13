package com.stripe.android.financialconnections

import com.airbnb.mvrx.Mavericks

/**
 * Wrapper to handle any initialization needed before launching [FinancialConnectionsSheetActivity].
 */
class FinancialConnectionsInitializer {

    fun initialize() {
        initMavericks()
    }

    /**
     * Tries to retrieve [Mavericks.viewModelConfigFactory]. If Mavericks hasn't yet been
     * initialized by the host app, it'll throw an [IllegalStateException]. In that case,
     * initialize.
     */
    private fun initMavericks() {
        try {
            Mavericks.viewModelConfigFactory
        } catch (exception: IllegalStateException) {
            Mavericks.initialize(debugMode = false)
        }
    }
}