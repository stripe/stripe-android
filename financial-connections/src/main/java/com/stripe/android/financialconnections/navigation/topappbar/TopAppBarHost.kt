package com.stripe.android.financialconnections.navigation.topappbar

import kotlinx.coroutines.flow.StateFlow

internal interface TopAppBarHost {

    // This is a temporary property. It'll be removed once we lift the top app bar
    // out of each screen.
    // TODO(tillh-stripe) Remove this
    val topAppBarState: StateFlow<TopAppBarState>

    fun updateTopAppBarState(update: TopAppBarStateUpdate?)
    fun updateTopAppBarElevation(isElevated: Boolean)
}
