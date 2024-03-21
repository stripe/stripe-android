package com.stripe.android.financialconnections.presentation

import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarHost

@Suppress("UNUSED_PARAMETER", "UnnecessaryAbstractClass")
internal abstract class FinancialConnectionsViewModel<S : MavericksState>(
    initialState: S,
    topAppBarHost: TopAppBarHost,
) : MavericksViewModel<S>(initialState) {

    init {
        onEach { state ->
            // TODO(tillh-stripe) Update top app bar based on state
        }
    }
}
