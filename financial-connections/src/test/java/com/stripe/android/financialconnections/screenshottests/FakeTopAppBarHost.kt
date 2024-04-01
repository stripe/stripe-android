package com.stripe.android.financialconnections.screenshottests

import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarHost
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarState
import kotlinx.coroutines.flow.StateFlow

internal class FakeTopAppBarHost : TopAppBarHost {

    override val topAppBarState: StateFlow<TopAppBarState>
        get() = error("Not expected")

    override fun updateTopAppBarElevation(isElevated: Boolean) {
        // Nothing to do here
    }
}
