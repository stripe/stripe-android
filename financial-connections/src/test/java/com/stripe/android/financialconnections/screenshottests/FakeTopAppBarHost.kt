package com.stripe.android.financialconnections.screenshottests

import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarHost
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarState
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarStateUpdate
import kotlinx.coroutines.flow.StateFlow

internal class FakeTopAppBarHost : TopAppBarHost {

    override val topAppBarState: StateFlow<TopAppBarState>
        get() = error("Not expected")

    override fun updateTopAppBarState(update: TopAppBarStateUpdate?) {
        // Nothing to do here
    }

    override fun updateTopAppBarElevation(isElevated: Boolean) {
        // Nothing to do here
    }
}
