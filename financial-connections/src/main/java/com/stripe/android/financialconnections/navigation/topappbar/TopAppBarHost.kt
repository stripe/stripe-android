package com.stripe.android.financialconnections.navigation.topappbar

import kotlinx.coroutines.flow.StateFlow

internal interface TopAppBarHost {
    // TODO(tillh-stripe): This is a temporary property. It'll be removed once we lift
    //  the top app bar out of each screen.
    val topAppBarState: StateFlow<TopAppBarState>
    fun updateTopAppBarElevation(isElevated: Boolean)
}
