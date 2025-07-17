package com.stripe.android.link

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

internal class FakeLinkActionManager : LinkActionManager {
    private val _actionFlow = MutableSharedFlow<LinkActionIntent>(extraBufferCapacity = 1)
    override val actionFlow: SharedFlow<LinkActionIntent> = _actionFlow.asSharedFlow()

    val emittedActions = mutableListOf<LinkActionIntent>()

    override fun emit(intent: LinkActionIntent) {
        emittedActions.add(intent)
        _actionFlow.tryEmit(intent)
    }
}
