package com.stripe.android.link

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

/**
 * Manages communication between Link screen ViewModels and the parent [LinkActivityViewModel]
 */
internal interface LinkActionManager {
    val actionFlow: SharedFlow<LinkActionIntent>

    fun emit(intent: LinkActionIntent)
}

internal sealed class LinkActionIntent {
    data class DismissWithResult(
        val result: LinkActivityResult
    ) : LinkActionIntent()
}

internal class LinkActionManagerImpl @Inject constructor() : LinkActionManager {
    private val _actionFlow = MutableSharedFlow<LinkActionIntent>(extraBufferCapacity = 1)

    override val actionFlow = _actionFlow.asSharedFlow()

    override fun emit(intent: LinkActionIntent) {
        _actionFlow.tryEmit(intent)
    }
}
