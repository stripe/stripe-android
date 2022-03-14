package com.stripe.android.link.model

import com.stripe.android.link.LinkScreen
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class Navigator @Inject constructor() {
    private val _sharedFlow =
        MutableSharedFlow<LinkScreen>(extraBufferCapacity = 1)
    val sharedFlow = _sharedFlow.asSharedFlow()

    var onDismiss = {}

    fun navigateTo(target: LinkScreen) {
        _sharedFlow.tryEmit(target)
    }

    fun dismiss() {
        onDismiss()
    }
}
