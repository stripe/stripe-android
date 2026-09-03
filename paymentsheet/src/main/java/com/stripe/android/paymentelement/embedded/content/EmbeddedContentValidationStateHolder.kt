package com.stripe.android.paymentelement.embedded.content

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class EmbeddedContentValidationStateHolder @Inject constructor() {
    private val _validationRequested = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val validationRequested: SharedFlow<Unit> = _validationRequested

    fun requestValidation() {
        _validationRequested.tryEmit(Unit)
    }
}
